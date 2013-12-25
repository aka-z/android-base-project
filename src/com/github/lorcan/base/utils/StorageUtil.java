/*
 *
 */
package com.github.lorcan.base.utils;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.widget.Toast;
import com.github.lorcan.base.R;


/**
 * 存储卡管理类.
 *
 * @author MaXingliang
 */
public class StorageUtil {

    /**
     * The Constant TAG.
     */
    private static final String TAG = "StorageUtil";

    /**
     * The Constant EXTERNAL_STORAGE.
     */
    public static final String EXTERNAL_STORAGE = Environment.getExternalStorageDirectory()
            .toString();

    /**
     * The Constant SINAREADER_BOOK_DIR.
     */
    public static final String SINALWALLPAPER_DIR = EXTERNAL_STORAGE + "/github/lorcan";

    /**
     * 缓存图片文件夹.
     */
    private static final String IMAGE_DIR = SINALWALLPAPER_DIR + "/image";

    /**
     * 日志文件夹.
     */
    private static final String LOG_DIR = SINALWALLPAPER_DIR + "/log";

    /**
     * The Constant DIR_TYPE_IMAGE.
     */
    public static final int DIR_TYPE_IMAGE = 1;

    /**
     * The Constant DIR_TYPE_LOG.
     */
    public static final int DIR_TYPE_LOG = 2;

    /**
     * The Constant DIR_TYPE_LOG.
     */
    public static final int DIR_TYPE_ALBUM = 3;

    /**
     * 该文件用来在图库中屏蔽本应用的图片.
     */
    public static final String NOMEDIA_FILE = SINALWALLPAPER_DIR + "/.nomedia";

    /**
     * Instantiates a new storage util.
     */
    private StorageUtil() {
    }

    /**
     * Creates the no media file.
     */
    public static void createNoMediaFile() {
        File file = new File(StorageUtil.NOMEDIA_FILE);
        try {
            if (!file.exists()) {
                LogUtil.e(TAG, "创建.nomedia文件:" + file.getAbsolutePath());
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks for external storage.
     *
     * @return true, if successful
     */
    public static boolean hasExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Gets the dir by type.
     *
     * @param type the type
     * @return the dir by type
     */
    public static String getDirByType(int type) {
        String dir = "/";
        String filePath = "";
        switch (type) {
            case DIR_TYPE_IMAGE: {
                filePath = IMAGE_DIR;
                break;
            }
            case DIR_TYPE_LOG: {
                filePath = LOG_DIR;
                break;
            }
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdirs();
        }
        if (file.exists()) {
            if (file.isDirectory()) {
                dir = file.getPath();
            }
        } else {
            // 文件没创建成功，可能是sd卡不存在，但是还是把路径返回
            dir = filePath;
        }
        return dir;
    }

    /**
     * 是否插入sdcard.
     *
     * @return true, if is sD card exist
     */
    public static boolean isSDCardExist() {
        boolean sdCardExist = false;
        sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        return sdCardExist;
    }

    /**
     * 判断存储空间是否足够.
     *
     * @param needSize the need size
     * @return true, if successful
     */
    public static boolean checkExternalSpace(float needSize, Context context) {
        boolean flag = false;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();
            long restSize = availCount * blockSize;
            if (restSize > needSize) {
                flag = true;
            } else {
                Toast.makeText(context, R.string.sd_smallsize,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, R.string.no_sdcard,
                    Toast.LENGTH_SHORT).show();
        }
        return flag;
    }
}
