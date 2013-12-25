package com.github.lorcan.base.image;

import android.os.Handler;
import android.os.Message;
import com.github.lorcan.base.utils.LogUtil;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 修改AsyncTask，修改线程池大小和线程池的抛弃策略 《图片线程池》
 *
 * @param <Result>
 * @author Tsimle
 */
public abstract class AbsImageAsyncTask<Params, Result> {
    private static final String LOG_TAG = "ImageBaseTask";

    private static final int CORE_POOL_SIZE = 16;

    private static final int MAXIMUM_POOL_SIZE = 128;

    private static final int KEEP_ALIVE = 32;

    private static final BlockingQueue<Runnable> sWorkQueue = new LinkedBlockingQueue<Runnable>(15);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageTask #" + mCount.getAndIncrement());
        }
    };

    private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue, sThreadFactory,
            new DiscardAndRemoveOldestPolicy());

    private static final int MESSAGE_POST_RESULT = 0x1;

    private static final int MESSAGE_POST_CANCEL = 0x2;

    private static final InternalHandler sHandler = new InternalHandler();

    private final WorkerRunnable<Params, Result> mWorker;

    private final WrappedFutureTask<Params, Result> mFuture;

    private volatile Status mStatus = Status.PENDING;

    private boolean isCancelled = false;

    /**
     * Indicates the current status of the task. Each status will be set only
     * once during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link android.os.AsyncTask#onPostExecute} has finished.
         */
        FINISHED,
    }

    /**
     * 释放队列中所有的图片加载任务
     */
    public static void release() {
        sWorkQueue.clear();
    }

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the
     * UI thread.
     */
    public AbsImageAsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                return doInBackground(mParams);
            }
        };

        mFuture = new WrappedFutureTask<Params, Result>(mWorker, this) {
            @SuppressWarnings("unchecked")
            @Override
            protected void done() {
                Message message;
                Result result = null;

                try {
                    result = get();
                } catch (InterruptedException e) {
                    LogUtil.e(LOG_TAG, e.getMessage(), e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    message = sHandler
                            .obtainMessage(MESSAGE_POST_CANCEL, new ImageBaseTaskResult<Result>(
                                    AbsImageAsyncTask.this, (Result[]) null));
                    message.sendToTarget();
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("An error occured while executing "
                            + "doInBackground()", t);
                }

                message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                        new ImageBaseTaskResult<Result>(AbsImageAsyncTask.this, result));
                message.sendToTarget();
            }
        };
    }

    /**
     * Returns the current status of this task.
     *
     * @return The current status.
     */
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute} by the
     * caller of this task.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     */
    protected abstract Result doInBackground(Params... params);

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onCancelled() {
    }

    public final boolean isCancelled() {
        return isCancelled;
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        isCancelled = true;
        if (sWorkQueue.remove(mFuture)) {
            onCancelled();
        } else {
            return mFuture.cancel(mayInterruptIfRunning);
        }
        return true;
    }

    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    public final AbsImageAsyncTask<Params, Result> execute(Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
                case PENDING:
                    break;
                default:
                    break;
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        sExecutor.execute(mFuture);
        return this;
    }

    private void finish(Result result) {
        if (isCancelled())
            result = null;
        onPostExecute(result);
        mStatus = Status.FINISHED;
    }

    private static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked"})
        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("rawtypes")
            ImageBaseTaskResult result = (ImageBaseTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_CANCEL:
                    result.mTask.onCancelled();
                    break;
            }
        }
    }

    private static class DiscardAndRemoveOldestPolicy implements RejectedExecutionHandler {
        public DiscardAndRemoveOldestPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                AbsImageAsyncTask<?, ?> discardTask = ((WrappedFutureTask<?, ?>) e.getQueue()
                        .poll()).originTask;
                Message message = sHandler.obtainMessage(MESSAGE_POST_CANCEL,
                        new ImageBaseTaskResult<String>(discardTask, (String[]) null));
                message.sendToTarget();
                e.execute(r);
            }
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    @SuppressWarnings("rawtypes")
    private static class ImageBaseTaskResult<Data> {
        final AbsImageAsyncTask mTask;

        final Data[] mData;

        ImageBaseTaskResult(AbsImageAsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }

    private static class WrappedFutureTask<Params, Result> extends FutureTask<Result> {
        public AbsImageAsyncTask<Params, Result> originTask;

        public WrappedFutureTask(Callable<Result> callable,
                                 AbsImageAsyncTask<Params, Result> originTask) {
            super(callable);
            this.originTask = originTask;
        }
    }
}
