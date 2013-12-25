package com.github.lorcan.base.image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ImageView;


import com.github.lorcan.base.network.HttpUtil;
import com.github.lorcan.base.utils.LogUtil;
import com.github.lorcan.base.utils.StorageUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片加载器.
 *
 * @author Tsimle
 */
@SuppressLint("UseSparseArrays")
public class ImageLoader {

    /**
     * The Constant LOG_TAG.
     */
    private static final String LOG_TAG = "ImageLoader";

    /**
     * 不对下载完成的图片做任何处理,返回原始下载的图片.
     */
    public static final int TYPE_BIG_PIC = 1004;

    /**
     * 圆角.
     */
    public static final int TYPE_ROUND_PIC = 1005;

    /**
     * 按照屏幕高度缩放图片.
     */
    public static final int TYPE_SCALE_PIC_BY_HEIGTH = 1006;

    /**
     * 按照屏幕宽度缩放图片.
     */
    public static final int TYPE_SCALE_PIC_BY_WIDTH = 1007;

    /**
     * 按照屏幕宽度缩放图片,高度自己适应.
     */
    public static final int TYPE_SCALE_PIC_BY_WIDTH_WARP_HEIGHT = 1008;

    /**
     * The instance.
     */
    private static ImageLoader instance;

    /**
     * The m context.
     */
    private Context mContext;

    /**
     * The sd card directory.
     */
    private String sdCardDirectory;

    /**
     * The running tasks.
     */
    private List<BitmapAsyncLoadTask> runningTasks = new ArrayList<BitmapAsyncLoadTask>();

    /**
     * The image cache.
     */
    private ImageCache imageCache;


    private int mScaleWidth;

    private int mScaleHeight;

    private String mDistinctkey;

    private int mScreenWidth;

    private int mScreenHeigth;

    /**
     * Instantiates a new image loader.
     */
    private ImageLoader() {
        imageCache = new ImageSoftCache();
    }

    /**
     * 只在UI线程调用.
     *
     * @return single instance of ImageLoader
     */
    public static ImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new ImageLoader();
            instance.mContext = context;
            instance.sdCardDirectory = StorageUtil.getDirByType(StorageUtil.DIR_TYPE_IMAGE);
        }
        return instance;
    }

    /**
     * 释放队列中所有的图片加载任务.
     */
    public void release() {
        AbsImageAsyncTask.release();
        for (BitmapAsyncLoadTask task : runningTasks) {
            imageCache.clear(task.getLoadContext());
        }
        runningTasks.clear();
        System.gc();
    }

    /**
     * 释放该Context的图片加载任务及它所有的图片缓存<br>
     * Activity destroy时才能调用.
     *
     * @param context the context
     */
    public void releaseContext(Context context) {
        String loadContext = getLoadContext(context);
        int size = runningTasks.size();
        if (size > 0) {
            BitmapAsyncLoadTask[] arrays = new BitmapAsyncLoadTask[size];
            runningTasks.toArray(arrays);
            for (BitmapAsyncLoadTask task : arrays) {
                if (task.getLoadContext().equals(loadContext)) {
                    task.cancel(true);
                    runningTasks.remove(task);
                }
            }
        }
        imageCache.clear(loadContext);
    }

    /**
     * 释放该Context的图片加载任务及它所有的图片缓存<br>
     * Activity destroy时才能调用.
     *
     * @param context the context
     */
    public void releaseContext(Context context, String distinctKey) {
        String loadContext = getLoadContext(context, distinctKey);
        int size = runningTasks.size();
        if (size > 0) {
            BitmapAsyncLoadTask[] arrays = new BitmapAsyncLoadTask[size];
            runningTasks.toArray(arrays);
            for (BitmapAsyncLoadTask task : arrays) {
                if (task.getLoadContext().equals(loadContext)) {
                    task.cancel(true);
                    runningTasks.remove(task);
                }
            }
        }
        imageCache.clear(loadContext);
    }

    /**
     * 取消掉该Context等待执行的图片加载任务<br>
     * 可以在Activity pause时调用<br>
     * 若调用了，在Activity resume时需要刷新列表，重新加载图片<br>
     * 在将跳转的页面和本页面都有大量图片加载任务，防止前一页面的图片加载阻塞后面的<br>
     * .
     *
     * @param context the context
     */
    public void cancelContext(Context context) {
        String loadContext = getLoadContext(context);
        int size = runningTasks.size();
        if (size > 0) {
            BitmapAsyncLoadTask[] arrays = new BitmapAsyncLoadTask[size];
            runningTasks.toArray(arrays);
            for (BitmapAsyncLoadTask task : arrays) {
                if (task.getLoadContext().equals(loadContext)) {
                    task.cancel(true);
                    runningTasks.remove(task);
                }
            }
        }
    }

    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @param listener      the listener
     * @param adapter       the adapter
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, int type, Bitmap defaultBitmap,
                        IImageLoadListener listener, BaseAdapter adapter, int screenWidth, int screenHeight) {
        if (url == null || "".equals(url.trim())) {
            imageView.setImageDrawable(new NoRecycledDrawable(imageView.getResources(),
                    defaultBitmap));
            return false;
        }

        this.mScreenWidth = screenWidth;
        this.mScreenHeigth = screenHeight;

        // 出现url中有空格的现象
        url = url.replaceAll(" ", "");

        Bitmap bitmap = null;
        if (mDistinctkey != null) {
            bitmap = imageCache.get(getImageCacheKey(url, getLoadContext(imageView, mDistinctkey)));

        } else {
            bitmap = imageCache.get(getImageCacheKey(url, getLoadContext(imageView)));

        }


        if (bitmap == null || bitmap.isRecycled()) {
            asyncLoad(url, imageView, type, defaultBitmap, listener, adapter);
            return false;
        } else {
            cancelPotentialAsyncLoad(url, imageView);
            imageView.setImageDrawable(new NoRecycledDrawable(imageView.getResources(), bitmap));
            if (listener != null) {
                listener.onImageLoaded(bitmap, imageView, true);
            }
            return true;
        }
    }

    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @param listener      the listener
     * @param scaleWidth    the scale width
     * @param scaleHeigth   the scale heigth
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, int type, Bitmap defaultBitmap,
                        IImageLoadListener listener, int scaleWidth, int scaleHeigth, int screenWidth, int screenHeight) {
        mScaleHeight = scaleHeigth;
        mScaleWidth = scaleWidth;

        return load(url, imageView, type, defaultBitmap, listener, null, screenWidth, screenHeight);
    }

    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @param adapter       the adapter
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, int type, Bitmap defaultBitmap,
                        BaseAdapter adapter, int screenWidth, int screenHeight) {
        return load(url, imageView, type, defaultBitmap, null, adapter, screenWidth, screenHeight);
    }

    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, int type, Bitmap defaultBitmap, String distinctKey, int screenWidth, int screenHeight) {
        mDistinctkey = distinctKey;
        return load(url, imageView, type, defaultBitmap, screenWidth, screenHeight);
    }


    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param defaultBitmap the default bitmap
     * @param adapter       the adapter
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, Bitmap defaultBitmap, BaseAdapter adapter, int screenWidth, int screenHeight) {
        return load(url, imageView, TYPE_BIG_PIC, defaultBitmap, null, adapter, screenWidth, screenHeight);
    }

    /**
     * Load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @return true, if successful
     */
    public boolean load(String url, ImageView imageView, int type, Bitmap defaultBitmap, int screenWidth, int screenHeight) {
        return load(url, imageView, type, defaultBitmap, null, null, screenWidth, screenHeight);
    }

    /**
     * 直接从本地加载同步返回图片.
     *
     * @param url the url
     * @return the bitmap
     */
    public Bitmap syncLoadBitmap(String url, int width, int height) {
        if (url == null) {
            return null;
        }
        // 出现url中有空格的现象
        url = url.replaceAll(" ", "");
        String tempFileName = ImageUtil.getTempFileName(url);
        return ImageUtil.getBitmapFromFile(sdCardDirectory, tempFileName,
                width, height);
    }


    /**
     * 是否已经下载过该图片.
     *
     * @param url the url
     * @return true, if successful
     */
    public boolean hasDownload(String url) {
        if (url == null) {
            return false;
        }
        if (!StorageUtil.isSDCardExist()) {
            return false;
        }
        // 出现url中有空格的现象
        url = url.replaceAll(" ", "");
        String tempFileName = ImageUtil.getTempFileName(url);
        File tempFile = new File(sdCardDirectory, tempFileName);
        if (tempFile.exists()) {
            return true;
        }
        return false;
    }

    /**
     * Async load.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param type          the type
     * @param defaultBitmap the default bitmap
     * @param listener      the listener
     * @param adapter       the adapter
     */
    private void asyncLoad(String url, ImageView imageView, int type, Bitmap defaultBitmap,
                           IImageLoadListener listener, BaseAdapter adapter) {
        if (cancelPotentialAsyncLoad(url, imageView)) {
            if (hasNoSameUrlTask(url, imageView, defaultBitmap, listener, adapter)) {
                BitmapAsyncLoadTask task = new BitmapAsyncLoadTask(imageView, url);
                if (listener != null) {
                    task.setListener(listener);
                }
                AsyncDrawable asyncDrawable = new AsyncDrawable(task, defaultBitmap);
                imageView.setImageDrawable(asyncDrawable);
                if (mDistinctkey != null) {
                    task.setLoadContext(getLoadContext(imageView, mDistinctkey));
                } else {
                    task.setLoadContext(getLoadContext(imageView));
                }
                task.execute(Integer.valueOf(type).toString());
                runningTasks.add(task);
                // LogUtil.e(LOG_TAG, url + ":" + runningTasks.size());
            }
        }
    }

    /**
     * Returns true if the current download has been canceled or if there was no
     * download in progress on this image view. Returns false if the download in
     * progress deals with the same url. The download is not stopped in that
     * case.
     *
     * @param url       the url
     * @param imageView the image view
     * @return true, if successful
     */
    private boolean cancelPotentialAsyncLoad(String url, ImageView imageView) {
        BitmapAsyncLoadTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                if (bitmapDownloaderTask.isSuccessLoad()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks for no same url task.
     *
     * @param url           the url
     * @param imageView     the image view
     * @param defaultBitmap the default bitmap
     * @param listener      the listener
     * @param adapter       the adapter
     * @return true, if successful
     */
    private boolean hasNoSameUrlTask(String url, ImageView imageView, Bitmap defaultBitmap,
                                     final IImageLoadListener listener, final BaseAdapter adapter) {
        for (BitmapAsyncLoadTask task : runningTasks) {
            if (task.url.equals(url) && !task.isCancelled()) {

                ImageView prevImageView = null;
                IImageLoadListener prevListener = null;

                // 1 拿到原task的imageView及listener
                if (task.imageViewReference != null) {
                    prevImageView = task.imageViewReference.get();
                    if (prevImageView == imageView) {
                        return false;
                    }
                }
                prevListener = task.listener;

                // 2 加入新的imageView及listener
                AsyncDrawable asyncDrawable = new AsyncDrawable(task, defaultBitmap);
                imageView.setImageDrawable(asyncDrawable);
                task.imageViewReference = new WeakReference<ImageView>(imageView);
                if (mDistinctkey != null) {
                    task.setLoadContext(getLoadContext(imageView, mDistinctkey));
                } else {
                    task.setLoadContext(getLoadContext(imageView));
                }
                task.setListener(listener);

                // 3 重新存入旧的imageView及listener
                if (prevImageView != null) {
                    task.setPrevImageView(prevImageView, prevListener);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the bitmap downloader task.
     *
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated
     * with this imageView. null if there is no such task.
     */
    private static BitmapAsyncLoadTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                AsyncDrawable downloadedDrawable = (AsyncDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    /**
     * Download url.
     *
     * @param url the url
     */
    public void downloadUrl(String url) {
        InputStream inputStream = null;
        HttpClient httpClient = null;
        FileOutputStream fos = null;
        try {
            httpClient = HttpUtil.getHttpClient(mContext);
            HttpResponse response = HttpUtil.doGetRequest(httpClient, url, null);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
            }
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            if (inputStream != null) {
                if (StorageUtil.isSDCardExist()) {
                    byte[] data = new byte[1024];
                    int len = 0;
                    File file = new File(sdCardDirectory, ImageUtil.getTempFileName(url));
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    fos = new FileOutputStream(file);
                    while ((len = inputStream.read(data, 0, data.length)) != -1) {
                        fos.write(data, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } catch (Exception e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fos != null) {
                    fos.flush();
                    fos.close();
                    fos = null;
                }
                if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                }
            } catch (IOException e) {
                LogUtil.e(LOG_TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Download bitmap.
     *
     * @param url the url
     * @return the bitmap
     */
    public Bitmap downloadBitmap(String url, Context context, int screenWidth, int screenHeight) {
        InputStream inputStream = null;
        HttpClient httpClient = null;
        FileOutputStream fos = null;
        try {
            httpClient = HttpUtil.getHttpClient(mContext);
            HttpResponse response = HttpUtil.doGetRequest(httpClient, url, null);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            if (inputStream != null) {
                if (StorageUtil.isSDCardExist()) {
                    byte[] data = new byte[1024];
                    int len = 0;
                    File file = new File(sdCardDirectory, ImageUtil.getTempFileName(url));
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    fos = new FileOutputStream(file);
                    while ((len = inputStream.read(data, 0, data.length)) != -1) {
                        fos.write(data, 0, len);
                    }
                    return ImageUtil.getBitmapFromFile(file.getAbsolutePath(),
                            screenWidth, screenHeight);
                } else {
                    return ImageUtil.getBitmapFromStream(inputStream,
                            screenWidth, screenHeight);
                }
            }
        } catch (IOException e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } catch (Exception e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fos != null) {
                    fos.flush();
                    fos.close();
                    fos = null;
                }
                if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                }
            } catch (IOException e) {
                LogUtil.w(LOG_TAG, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Download bitmap.
     *
     * @param url          the url
     * @param tempFileName the temp file name
     * @param scaleWidth   the scale width
     * @param scaleHeight  the scale height
     * @return the bitmap
     */
    public Bitmap downloadBitmap(String url, String tempFileName, int scaleWidth, int scaleHeight) {
        InputStream inputStream = null;
        HttpClient httpClient = null;
        FileOutputStream fos = null;
        try {
            httpClient = HttpUtil.getHttpClient(mContext);
            HttpResponse response = HttpUtil.doGetRequest(httpClient, url, null);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            if (inputStream != null) {
                if (StorageUtil.isSDCardExist()) {
                    byte[] data = new byte[1024];
                    int len = 0;
                    File file = new File(sdCardDirectory, tempFileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    fos = new FileOutputStream(file);
                    while ((len = inputStream.read(data, 0, data.length)) != -1) {
                        fos.write(data, 0, len);
                    }
                    return ImageUtil.getBitmapFromFile(sdCardDirectory, tempFileName, scaleWidth,
                            scaleHeight);
                } else {
                    return ImageUtil.getBitmapFromStream(inputStream, scaleWidth, scaleHeight);
                }
            }
        } catch (IOException e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } catch (Exception e) {
            LogUtil.w(LOG_TAG, e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fos != null) {
                    fos.flush();
                    fos.close();
                    fos = null;
                }
                if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                }
            } catch (IOException e) {
                LogUtil.w(LOG_TAG, e.getMessage());
            }
        }
        return null;
    }

    /**
     * The Class BitmapAsyncLoadTask.
     */
    private class BitmapAsyncLoadTask extends AbsImageAsyncTask<String, Bitmap> {

        /**
         * The url.
         */
        private String url;

        /**
         * The listener.
         */
        private IImageLoadListener listener;

        /**
         * The image view reference.
         */
        private WeakReference<ImageView> imageViewReference;

        /**
         * The prev image view list.
         */
        private List<WeakReference<ImageView>> prevImageViewList;

        /**
         * The prev listeners.
         */
        private Map<ImageView, IImageLoadListener> prevListeners;

        /**
         * The is success load.
         */
        private boolean isSuccessLoad;

        /**
         * The load context.
         */
        private String loadContext;

        /**
         * The scale width.
         */
        private int scaleWidth;

        /**
         * The scale height.
         */
        private int scaleHeight;

        /**
         * Instantiates a new bitmap async load task.
         *
         * @param imageView the image view
         * @param url       the url
         */
        public BitmapAsyncLoadTask(ImageView imageView, String url) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.url = url;
            isSuccessLoad = false;
            if (imageView != null) {
                scaleWidth = imageView.getWidth();
                scaleHeight = imageView.getHeight();
            }
        }

        /**
         * Sets the prev image view.
         *
         * @param prevImageView the prev image view
         * @param prevListener  the prev listener
         */
        public void setPrevImageView(ImageView prevImageView, IImageLoadListener prevListener) {
            if (prevImageViewList == null) {
                prevImageViewList = new ArrayList<WeakReference<ImageView>>();
            }
            prevImageViewList.add(new WeakReference<ImageView>(prevImageView));

            if (prevListener != null) {
                if (prevListeners == null) {
                    prevListeners = new HashMap<ImageView, IImageLoadListener>();
                }
                prevListeners.put(prevImageView, prevListener);
            }
        }

        /**
         * Sets the listener.
         *
         * @param listener the new listener
         */
        public void setListener(IImageLoadListener listener) {
            this.listener = listener;
        }

        /**
         * Checks if is success load.
         *
         * @return true, if is success load
         */
        public boolean isSuccessLoad() {
            return isSuccessLoad;
        }

        /**
         * Sets the load context.
         *
         * @param loadContext the new load context
         */
        public void setLoadContext(String loadContext) {
            this.loadContext = loadContext;
        }

        /**
         * Gets the load context.
         *
         * @return the load context
         */
        public String getLoadContext() {
            return loadContext;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.sina.weatherwallpaper.image.AbsImageAsyncTask#doInBackground(
         * Params[])
         */
        @Override
        protected Bitmap doInBackground(String... params) {
            int type = Integer.parseInt(params[0]);
            Bitmap bm = null;
            String tempFileName = ImageUtil.getTempFileName(url);
            if (scaleWidth == 0 || scaleHeight == 0) {
                scaleWidth = mScreenWidth;
                scaleHeight = mScreenHeigth;
            }
            if (url.startsWith(ConstantData.LOCAL_PATH_IMG)) {
                bm = ImageUtil.getBitmapFromAssetsFile(mContext,
                        url.replaceFirst(ConstantData.LOCAL_PATH_IMG, ""));
                if (bm != null && type == TYPE_SCALE_PIC_BY_HEIGTH) {
                    bm = ImageUtil.doScaleByHeigth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH) {
                    bm = ImageUtil.doScaleByWidth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH_WARP_HEIGHT) {
                    bm = ImageUtil.doScaleByWidthWarpHeight(bm, mScaleWidth);
                }
                return bm;
            } else if (url.startsWith(ConstantData.SDCARD_PATH_IMG)) {
                bm = ImageUtil
                        .getBitmapFromFile(url.replaceFirst(ConstantData.SDCARD_PATH_IMG, ""),
                                scaleWidth, scaleHeight);
                if (bm != null && type == TYPE_SCALE_PIC_BY_HEIGTH) {
                    bm = ImageUtil.doScaleByHeigth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH) {
                    bm = ImageUtil.doScaleByWidth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH_WARP_HEIGHT) {
                    bm = ImageUtil.doScaleByWidthWarpHeight(bm, mScaleWidth);
                }
                return bm;
            } else {
                bm = ImageUtil.getBitmapFromFile(sdCardDirectory, tempFileName, scaleWidth,
                        scaleHeight);
                if (bm == null) {
                    bm = downloadBitmap(url, tempFileName, scaleWidth, scaleHeight);
                }
                if (bm != null && type == TYPE_ROUND_PIC) {
                    bm = ImageUtil.getRoundedCornerBitmap(bm, 4);
                }
                if (bm != null && type == TYPE_SCALE_PIC_BY_HEIGTH) {
                    bm = ImageUtil.doScaleByHeigth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH) {
                    bm = ImageUtil.doScaleByWidth(bm, mScaleWidth, mScaleHeight);
                } else if (bm != null && type == TYPE_SCALE_PIC_BY_WIDTH_WARP_HEIGHT) {
                    bm = ImageUtil.doScaleByWidthWarpHeight(bm, mScaleWidth);
                }
                return bm;
            }

        }

        /*
         * (non-Javadoc)
         *
         * @see com.sina.weatherwallpaper.image.AbsImageAsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            runningTasks.remove(this);
        }

        /**
         * Once the image is downloaded, associates it to the imageView.
         *
         * @param bitmap the bitmap
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            runningTasks.remove(this);

            if (isCancelled()) {
                bitmap = null;
            }

            if (bitmap != null) {
                imageCache.put(getImageCacheKey(url, loadContext), bitmap);

                if (imageViewReference != null) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null) {
                        BitmapAsyncLoadTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                        // Change bitmap only if this process is still
                        // associated
                        // with it
                        // Or if we don't use any bitmap to task association
//                        if (this == bitmapDownloaderTask) {
                        isSuccessLoad = true;
                        imageView.setImageDrawable(new NoRecycledDrawable(imageView
                                .getResources(), bitmap));
                        if (listener != null) {
                            listener.onImageLoaded(bitmap, imageView, true);
                        }
//                        }
                    }
                }

                if (prevImageViewList != null) {
                    for (WeakReference<ImageView> prevImageViewRef : prevImageViewList) {
                        ImageView prevImageView = prevImageViewRef.get();
                        if (prevImageView != null) {
                            BitmapAsyncLoadTask bitmapDownloaderTask = getBitmapDownloaderTask(prevImageView);
                            if (this == bitmapDownloaderTask) {
                                prevImageView.setImageDrawable(new NoRecycledDrawable(prevImageView
                                        .getResources(), bitmap));
                                if (prevListeners != null) {
                                    IImageLoadListener prevListener = prevListeners
                                            .get(prevImageView);
                                    if (prevListener != null) {
                                        prevListener.onImageLoaded(bitmap, prevImageView, true);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // bitmap加载失败的情况下，还是去通知
                if (listener != null) {
                    if (imageViewReference != null) {
                        ImageView imageView = imageViewReference.get();
                        if (imageView != null) {
                            BitmapAsyncLoadTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                            if (this == bitmapDownloaderTask) {
                                listener.onImageLoaded(bitmap, imageView, false);
                            }
                        }
                    }
                }

                if (prevImageViewList != null && prevListeners != null) {
                    for (WeakReference<ImageView> prevImageViewRef : prevImageViewList) {
                        ImageView prevImageView = prevImageViewRef.get();
                        if (prevImageView != null) {
                            BitmapAsyncLoadTask bitmapDownloaderTask = getBitmapDownloaderTask(prevImageView);
                            if (this == bitmapDownloaderTask) {
                                IImageLoadListener prevListener = prevListeners.get(prevImageView);
                                if (prevListener != null) {
                                    prevListener.onImageLoaded(bitmap, prevImageView, false);
                                }
                            }
                        }
                    }
                }
            }

            // 为了放心，清理引用
            imageViewReference = null;
            listener = null;
            prevImageViewList = null;
            prevListeners = null;
        }
    }

    /**
     * 通过imageview拿到它context的名字.
     *
     * @param imageView the image view
     * @return the load context
     */
    private String getLoadContext(ImageView imageView) {
        return imageView.getContext().getClass().getSimpleName()
                + Integer.toHexString(imageView.getContext().hashCode());
    }

    /**
     * 通过imageview拿到它context的名字.
     *
     * @param imageView the image view
     * @return the load context
     */
    private String getLoadContext(ImageView imageView, String distinctKey) {
        return imageView.getContext().getClass().getSimpleName()
                + Integer.toHexString(imageView.getContext().hashCode()) + distinctKey;
    }

    /**
     * Gets the load context.
     *
     * @param context the context
     * @return the load context
     */
    private String getLoadContext(Context context) {
        return context.getClass().getSimpleName() + Integer.toHexString(context.hashCode());
    }

    /**
     * Gets the load context.
     * 同一个页面的回收机制
     *
     * @param context the context
     * @return the load context
     */
    private String getLoadContext(Context context, String distinctKey) {
        return context.getClass().getSimpleName() + Integer.toHexString(context.hashCode()) + distinctKey;
    }


    /**
     * 得到image cache的key.
     *
     * @param url         the url
     * @param loadContext the load context
     * @return the image cache key
     */
    private String getImageCacheKey(String url, String loadContext) {
        StringBuilder sb = new StringBuilder(loadContext);
        sb.append(url);
        return sb.toString();
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download
     * is in progress.
     * <p/>
     * <p>
     * Contains a reference to the actual download task, so that a download task
     * can be stopped if a new binding is required, and makes sure that only the
     * last started download process can bind its result, independently of the
     * download finish order.
     * </p>
     */
    private static class AsyncDrawable extends BitmapDrawable {

        /**
         * The bitmap downloader task reference.
         */
        private WeakReference<BitmapAsyncLoadTask> bitmapDownloaderTaskReference;

        /**
         * Instantiates a new async drawable.
         *
         * @param bitmapDownloaderTask the bitmap downloader task
         * @param defaultBitmap        the default bitmap
         */
        public AsyncDrawable(BitmapAsyncLoadTask bitmapDownloaderTask, Bitmap defaultBitmap) {
            super(defaultBitmap);
            bitmapDownloaderTaskReference = new WeakReference<BitmapAsyncLoadTask>(
                    bitmapDownloaderTask);
        }

        /**
         * Gets the bitmap downloader task.
         *
         * @return the bitmap downloader task
         */
        public BitmapAsyncLoadTask getBitmapDownloaderTask() {
            if (bitmapDownloaderTaskReference != null) {
                return bitmapDownloaderTaskReference.get();
            } else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.graphics.drawable.BitmapDrawable#draw(android.graphics.Canvas
         * )
         */
        @Override
        public void draw(Canvas canvas) {
            if (getBitmap() == null || getBitmap().isRecycled()) {
                return;
            }
            super.draw(canvas);
        }
    }

    /**
     * 防止抛出recycled异常.
     *
     * @author Tsmile
     */
    private static class NoRecycledDrawable extends BitmapDrawable {

        /**
         * Instantiates a new no recycled drawable.
         *
         * @param res    the res
         * @param bitmap the bitmap
         */
        public NoRecycledDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.graphics.drawable.BitmapDrawable#draw(android.graphics.Canvas
         * )
         */
        @Override
        public void draw(Canvas canvas) {
            if (getBitmap() == null || getBitmap().isRecycled()) {
                return;
            }
            super.draw(canvas);
        }
    }

    /**
     * The Class ImageResizeBean.
     */
    public class ImageResizeBean {

        /**
         * The scale width.
         */
        int scaleWidth;

        /**
         * The scale height.
         */
        int scaleHeight;

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("{w:").append(scaleWidth);
            buffer.append(",h:").append(scaleHeight);
            buffer.append('}');
            return buffer.toString();
        }
    }
}
