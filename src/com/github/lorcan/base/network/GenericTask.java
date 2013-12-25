package com.github.lorcan.base.network;

public abstract class GenericTask extends
        AbsNormalAsyncTask<TaskParams, Object, TaskResult> {

	private ITaskFinishListener mTaskFinishListener;
	protected TaskResult mResult = new TaskResult(-1, this, null);

	@Override
	protected void onPostExecute(TaskResult result) {
		super.onPostExecute(result);
		if (mTaskFinishListener != null) {
			mTaskFinishListener.onTaskFinished(result);
		}
	}

	@Override
	protected void onCancelled() {
	    mTaskFinishListener = null;
		super.onCancelled();
		//当任务被取消后，不再回调listener
	}

	public ITaskFinishListener getTaskFinishListener() {
		return mTaskFinishListener;
	}

	public void setTaskFinishListener(ITaskFinishListener taskFinishListener) {
		this.mTaskFinishListener = taskFinishListener;
	}

	protected void sleepForSecond(long second) {
		long time = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - time > second) {
				break;
			}
		}
	}
}
