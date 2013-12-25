package com.github.lorcan.base.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import com.github.lorcan.base.utils.LogUtil;
import com.github.lorcan.base.utils.StorageUtil;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * 清理图片缓存，缩放图片等功能方法.
 *
 * @author Tsimle
 */
public class ImageUtil {

    private static final String TAG = "Image";

    /**
     * 图片缓存的清理参数.
     */
    public static int IMAGE_CACHE_LIMIT = 500;

    /**
     * The image cache clear limit.
     */
    public static int IMAGE_CACHE_CLEAR_LIMIT = 200;

    /**
     * The image cache expire.
     */
    public static long IMAGE_CACHE_EXPIRE = 8L;

    /**
     * 间隔48小时尝试清理图片.
     */
    public static long IMAGE_CACHE_CLEAR_DUR_TIME = 172800000L;

    /**
     * The Constant IMG_CACHE_PREF_FILE.
     */
    public static final String IMG_CACHE_PREF_FILE = "img_cache";

    /**
     * 存储图片缓存的清理时间.
     */
    private static final String KEY_IMG_CACHE_TIME = "key_img_clear";

    /**
     * Gets the bitmap from file.
     *
     * @param pathFileName the path file name
     * @param scaleWidth   the scale width
     * @param scaleHeight  the scale height
     * @return the bitmap from file
     */
    public static Bitmap getBitmapFromFile(String pathFileName, int scaleWidth, int scaleHeight) {
        if (!StorageUtil.isSDCardExist()) {
            return null;
        }
        Bitmap bitmap = null;
        File tempFile = new File(pathFileName);
        if (tempFile.exists()) {
            bitmap = getResizeBitmap(tempFile.getAbsolutePath(), scaleWidth, scaleHeight);

            // 更新文件的访问时间，防止被清理
            tempFile.setLastModified(System.currentTimeMillis());
        }
        return bitmap;
    }

    /**
     * Gets the bitmap from file.
     *
     * @param dirpath      the dirpath
     * @param tempFileName the temp file name
     * @param scaleWidth   the scale width
     * @param scaleHeight  the scale height
     * @return the bitmap from file
     */
    public static Bitmap getBitmapFromFile(String dirpath, String tempFileName, int scaleWidth,
                                           int scaleHeight) {
        if (!StorageUtil.isSDCardExist()) {
            return null;
        }
        Bitmap bitmap = null;
        File tempFile = new File(dirpath, tempFileName);
        if (tempFile.exists()) {
            bitmap = getResizeBitmap(tempFile.getAbsolutePath(), scaleWidth, scaleHeight);

            // 更新文件的访问时间，防止被清理
            tempFile.setLastModified(System.currentTimeMillis());
        }
        return bitmap;
    }

    /**
     * Gets the bitmap from stream.
     *
     * @param in          the in
     * @param scaleWidth  the scale width
     * @param scaleHeight the scale height
     * @return the bitmap from stream
     * @throws java.io.IOException Signals that an I/O exception has occurred.
     */
    public static Bitmap getBitmapFromStream(InputStream in, int scaleWidth, int scaleHeight)
            throws IOException {
        Bitmap bitmap = null;
        byte[] bytes = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len = 0;

            while ((len = in.read(b, 0, 1024)) != -1) {
                baos.write(b, 0, len);
                baos.flush();
            }
            bytes = baos.toByteArray();
            bitmap = getResizeBitmap(bytes, scaleWidth, scaleHeight);
        } catch (IOException e) {
            throw e;
        } finally {
            if (baos != null) {
                baos.close();
            }
        }
        return bitmap;
    }

    /**
     * Gets the bitmap from assets file.
     *
     * @param context  the context
     * @param filePath the file path
     * @return the bitmap from assets file
     */
    public static Bitmap getBitmapFromAssetsFile(Context context, String filePath) {
        Bitmap bitmap = null;
        InputStream is = null;
        AssetManager am = context.getAssets();
        try {
            is = am.open(filePath);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPurgeable = true;
            opt.inInputShareable = true;
            opt.inPreferredConfig = Config.RGB_565;
            bitmap = BitmapFactory.decodeStream(is, null, opt);
        } catch (IOException e) {
            // 忽略
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e.getMessage(), e);
                }
            }
        }
        return bitmap;
    }

    /**
     * Gets the bitmap from res id.
     *
     * @param context the context
     * @param id      the id
     * @return the bitmap from res id
     */
    public static Bitmap getBitmapFromResId(Context context, int id) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPurgeable = true;
            opt.inInputShareable = true;
            opt.inPreferredConfig = Config.RGB_565;
            is = context.getResources().openRawResource(id);
            bitmap = BitmapFactory.decodeStream(is, null, opt);
        } catch (OutOfMemoryError e) {
            LogUtil.e(TAG, e.getMessage(), e);
            System.gc();
        } catch (Exception e) {
            // do nothing
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    /**
     * 根据URL获取图片在文件系统中的文件名.
     *
     * @param url the url
     * @return the temp file name
     */
    public static String getTempFileName(String url) {
        String tempFileName = null;
        if (null != url && !"".equals(url.trim())) {
            tempFileName = url.replace('/', '_').replace(':', '_').replace("?", "_")
                    .replace(".jpg", ".tmp").replace(".png", ".tmp");
        }
        return tempFileName;
    }

    /**
     * 根据图片清理参数，清理图片缓存.
     *
     * @param context the context
     */
    public static void clearDiskCache(Context context) {
        LogUtil.d(TAG, "try to clearDiskCache");
        if (!StorageUtil.isSDCardExist()) {
            return;
        }
        if (needImgCacheClear(context)) {
            final String imgDir = StorageUtil.getDirByType(StorageUtil.DIR_TYPE_IMAGE);
            new Thread() {
                public void run() {
                    File imgDirFile = new File(imgDir);
                    executeClear(imgDirFile);
                }

                ;
            }.start();

            saveImgCacheClearTime(context);
        }
    }

    /**
     * 清理图片缓存.
     *
     * @param dirFile the dir file
     */
    private static void executeClear(File dirFile) {
        File[] arrayOfFile = null;
        long now = 0L;
        int fileLength = 0;
        if ((dirFile.exists()) && (dirFile.isDirectory())) {
            now = new Date().getTime();
            arrayOfFile = dirFile.listFiles();
            if (arrayOfFile != null)
                fileLength = arrayOfFile.length;
        }
        // 如果缓存图片的数量小于图片缓存上限
        if (fileLength < IMAGE_CACHE_LIMIT) {
            return;
        }

        // 对文件按时间排序
        Arrays.sort(arrayOfFile, new FileTimeCompartor());

        int clearNum = 0;
        int needClearTotal = fileLength - IMAGE_CACHE_CLEAR_LIMIT;
        for (int i = 0; i < fileLength; i++) {
            File paramFile = arrayOfFile[i];
            // 如果缓存图片的时间为IMAGE_CACHE_EXPIRE以内，即最经常使用的
            if ((now - paramFile.lastModified()) / 3600000L < IMAGE_CACHE_EXPIRE) {
                break;
            }
            if ((paramFile.exists()) && (paramFile.isFile())) {
                paramFile.delete();
                clearNum++;
            }
            // 如果缓存图片的数量已经小于图片缓存上限，不再清理
            if (clearNum >= needClearTotal) {
                break;
            }
        }
        LogUtil.d(TAG, "clearDiskCache-fileLength :" + fileLength + "needClearTotal :" + needClearTotal
                + "clearNum :" + clearNum);
    }

    /**
     * 存储图片缓存清理的时间.
     */
    private static void saveImgCacheClearTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                IMG_CACHE_PREF_FILE, Context.MODE_PRIVATE);
        long curtime = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_IMG_CACHE_TIME, curtime);
        editor.commit();
    }

    /**
     * 是否需要清理图片缓存.
     *
     * @return true, if successful
     */
    private static boolean needImgCacheClear(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                IMG_CACHE_PREF_FILE, Context.MODE_PRIVATE);
        long curtime = System.currentTimeMillis();
        long preftime = preferences.getLong(KEY_IMG_CACHE_TIME, 0L);
        LogUtil.d(TAG, "clear disk cache--curtime:" + curtime + " preftime:" + preftime);
        // 间隔3小时，就再次尝试清理文件的图片缓存
        if (curtime - preftime > IMAGE_CACHE_CLEAR_DUR_TIME) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 返回resize之后的bitmap.
     *
     * @param filePath the file path
     * @param width    the width
     * @param height   the height
     * @return the resize bitmap
     */
    public static Bitmap getResizeBitmap(String filePath, int width, int height) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        if (width == 0 || height == 0) {
            return null;
        }
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            options.inJustDecodeBounds = false;
            if (options.outWidth != 0 && options.outHeight != 0) {
                int[] scale = ImageUtil.getScaleByWidth(options.outWidth, options.outHeight, width,
                        height);
                int scaleW = scale[0];
                int scaleH = scale[1];
                // 如果需要的大小小于原始大小时
                if (scaleW < options.outWidth && scaleH < options.outHeight) {
                    options.inSampleSize = ImageUtil
                            .computeSampleSize(options, -1, scaleW * scaleH);
                    options.outWidth = scaleW;
                    options.outHeight = scaleH;
                }
            }
            options.inPreferredConfig = Config.RGB_565;
            options.inPurgeable = true;
            options.inInputShareable = true;
            try {
                BitmapFactory.Options.class.getField("inNativeAlloc").setBoolean(options, true);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // e.printStackTrace();
            }
            bitmap = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError error) {
            // 实在要出现内存不足的问题，清空缓存，调GC，没办法
            LogUtil.w(TAG, error.getMessage());
            System.gc();
            bitmap = getResizeBitmap(filePath, width / 2, height / 2);
            if (bitmap != null) {
                LogUtil.e(TAG, "getResizeBitmap 降低采样率获取图片成功");
            } else {
                LogUtil.e(TAG, "getResizeBitmap 降低采样率获取图片失败");
            }
        }
        return bitmap;
    }

    /**
     * 返回resize之后的bitmap.
     *
     * @param bytes  the bytes
     * @param width  the width
     * @param height the height
     * @return the resize bitmap
     */
    public static Bitmap getResizeBitmap(byte[] bytes, int width, int height) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (width == 0 || height == 0) {
            return null;
        }
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            options.inJustDecodeBounds = false;
            if (options.outWidth != 0 && options.outHeight != 0) {
                int[] scale = ImageUtil.getScaleByWidth(options.outWidth, options.outHeight, width,
                        height);
                int scaleW = scale[0];
                int scaleH = scale[1];
                // 如果需要的大小小于原始大小时
                if (scaleW < options.outWidth && scaleH < options.outHeight) {
                    options.inSampleSize = ImageUtil
                            .computeSampleSize(options, -1, scaleW * scaleH);
                    options.outWidth = scaleW;
                    options.outHeight = scaleH;
                }
            }
            options.inPreferredConfig = Config.RGB_565;
            options.inPurgeable = true;
            options.inInputShareable = true;
            try {
                BitmapFactory.Options.class.getField("inNativeAlloc").setBoolean(options, true);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // e.printStackTrace();
            }

            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            return bitmap;
        } catch (OutOfMemoryError error) {
            // 实在要出现内存不足的问题，清空缓存，调GC，没办法
            LogUtil.w(TAG, error.getMessage());
            System.gc();
            bitmap = getResizeBitmap(bytes, width / 2, height / 2);
            if (bitmap != null) {
                LogUtil.e(TAG, "getResizeBitmap 降低采样率获取图片成功");
            } else {
                LogUtil.e(TAG, "getResizeBitmap 降低采样率获取图片失败");
            }
        }
        return bitmap;
    }

    /**
     * Do scale by width.
     *
     * @param bitmap    the bitmap
     * @param minWidth  the min width
     * @param minHeight the min height
     * @return the bitmap
     */
    public static Bitmap doScaleByWidth(Bitmap bitmap, int minWidth, int minHeight) {
        if (bitmap == null) {
            return bitmap;
        }
        int h = minHeight;
        int w = h * bitmap.getWidth() / bitmap.getHeight();
        while (w < minWidth) {
            h++;
            w = h * bitmap.getWidth() / bitmap.getHeight();
        }
        if (w == bitmap.getWidth() && h == bitmap.getHeight()) {
            return bitmap;
        }
        return zoom(bitmap, w, h);
    }

    /**
     * Do scale by width warp height.
     *
     * @param bitmap   the bitmap
     * @param minWidth the min width
     * @return the bitmap
     */
    public static Bitmap doScaleByWidthWarpHeight(Bitmap bitmap, int minWidth) {
        if (bitmap == null) {
            return bitmap;
        }
        float scale = (float) minWidth / bitmap.getWidth();
        int h = (int) (bitmap.getHeight() * scale);
        int w = minWidth;

        if (w == bitmap.getWidth() && h == bitmap.getHeight()) {
            return bitmap;
        }
        return zoom(bitmap, w, h);
    }

    /**
     * 返回的bitmap宽度和高度都不会比屏幕宽度和高度小，且不会失真.
     *
     * @param bitmap    the bitmap
     * @param minWidth  the min width
     * @param minHeight the min height
     * @return the bitmap
     */
    public static Bitmap doScaleByHeigth(Bitmap bitmap, int minWidth, int minHeight) {
        if (bitmap == null) {
            return bitmap;
        }
        int w = minWidth;
        int h = w * bitmap.getHeight() / bitmap.getWidth();
        while (h < minHeight) {
            w++;
            h = w * bitmap.getHeight() / bitmap.getWidth();
        }
        if (w == bitmap.getWidth() && h == bitmap.getHeight()) {
            return bitmap;
        }
        return zoom(bitmap, w, h);
    }

    /**
     * Gets the scale by height.
     *
     * @param width     the width
     * @param height    the height
     * @param minWidth  the min width
     * @param minHeight the min height
     * @return the scale by height
     */
    public static int[] getScaleByHeight(int width, int height, int minWidth, int minHeight) {
        int w = minWidth;
        int h = w * height / width;
        while (h < minHeight) {
            w++;
            h = w * height / width;
        }
        return new int[]{w, h};
    }

    /**
     * Gets the scale by width.
     *
     * @param width     the width
     * @param height    the height
     * @param minWidth  the min width
     * @param minHeight the min height
     * @return the scale by width
     */
    public static int[] getScaleByWidth(int width, int height, int minWidth, int minHeight) {
        int h = minHeight;
        int w = h * width / height;
        while (w < minWidth) {
            h++;
            w = h * width / height;
        }
        return new int[]{w, h};
    }

    /**
     * 缩放图片.
     *
     * @param bitmap 原图
     * @param w      宽
     * @param h      高
     * @return the bitmap
     */
    public static Bitmap zoom(Bitmap bitmap, int w, int h) {
        if (bitmap == null) {
            return bitmap;
        }
        Bitmap newBitmap = null;
        try {
            newBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        } catch (OutOfMemoryError error) {
            System.gc();
            LogUtil.e(TAG, "内存溢出：zoom缩放图片");
            return bitmap;
        }
        if (newBitmap == null) {
            return bitmap;
        }
        if (!bitmap.isRecycled() && !bitmap.equals(newBitmap)) {
            bitmap.recycle();
            bitmap = null;
        }
        return newBitmap;
    }

    /**
     * 获得圆角图片.
     *
     * @param bm      the bm
     * @param roundPx the round px
     * @return the rounded corner bitmap
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bm, float roundPx) {
        Bitmap bitmapOrg = bm;
        Bitmap output = Bitmap.createBitmap(bitmapOrg.getWidth(), bitmapOrg.getHeight(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmapOrg.getWidth(), bitmapOrg.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmapOrg, rect, rect, paint);
        bitmapOrg.recycle();
        return output;
    }

    /**
     * Compute sample size.
     *
     * @param options        the options
     * @param minSideLength  the min side length
     * @param maxNumOfPixels the max num of pixels
     * @return the int
     */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength,
                                        int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize += 1;
            }
            if (roundedSize != 1) {
                roundedSize = roundedSize - 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    /**
     * Compute initial sample size.
     *
     * @param options        the options
     * @param minSideLength  the min side length
     * @param maxNumOfPixels the max num of pixels
     * @return the int
     */
    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength,
                                                int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h
                / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    /**
     * The Class FileTimeCompartor.
     */
    private static class FileTimeCompartor implements Comparator<File> {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(File object1, File object2) {
            if (object1.lastModified() - object2.lastModified() < 0) {
                return -1;
            } else if (object1.lastModified() - object2.lastModified() > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
