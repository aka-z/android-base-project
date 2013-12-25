package com.github.lorcan.base.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import android.util.Log;
import com.github.lorcan.base.network.GenericTask;
import com.github.lorcan.base.network.TaskParams;
import com.github.lorcan.base.network.TaskResult;

/**
 * 日志工具类
 *
 * @author Tsimle
 *
 */
public class LogUtil {

    public static int LOG_D = 1;
    public static int LOG_I = 2;
    public static int LOG_W = 3;
    public static int LOG_E = 4;

    private static String DEF_TAG = "LogUtil";

    private static final String LOG_FILE_PATH = "background.log";
    private static final int MAX_LOG_FILE_LENGTH = 1024 * 500;

    private static final String EXCEPTION_LOG_FILE_PATH = "crash.log";
    public static final int MAX_CRASHLOG_FILE_LENGTH = 1024 * 500;

    /**
     * log 标志位，false关闭
     */
    public static boolean gLogFlag = true;

    public static void d(String tag, String msg) {
        log(tag, msg, LOG_D);
    }

    public static void d(String msg) {
        log(DEF_TAG, msg, LOG_D);
    }

    public static void i(String tag, String msg) {
        log(tag, msg, LOG_I);
    }

    public static void i(String msg) {
        log(DEF_TAG, msg, LOG_I);
    }

    public static void w(String tag, String msg, Throwable tr) {
        log(tag, msg, tr, LOG_W);
    }

    public static void w(String tag, String msg) {
        log(tag, msg, LOG_W);
    }

    public static void w(Throwable tr) {
        log(DEF_TAG, "", tr, LOG_W);
    }

    public static void e(String tag, String msg, Throwable tr) {
        log(tag, msg, tr, LOG_E);
    }

    public static void e(String tag, String msg) {
        log(tag, msg, LOG_E);
    }

    public static void e(Throwable tr) {
        log(DEF_TAG, "", tr, LOG_E);
    }

    private static void log(String tag, String msg, int logType) {
        // 如果关闭log，直接返回
        if (!gLogFlag) {
            return;
        }
        if (msg == null) {
            msg = "null";
        }
        if (LOG_D == logType) {
            Log.d(tag, msg);
        } else if (LOG_I == logType) {
            Log.i(tag, msg);
        } else if (LOG_W == logType) {
            Log.w(tag, msg);
        } else if (LOG_E == logType) {
            Log.e(tag, msg);
        } else {
            Log.d(tag, msg);
        }
    }

    /**
     * 用来debug一些后台服务的运行情况<br>
     * log到文件<br>
     *
     * @param msg
     */
    public static void fileLogI(String msg) {
        if (!gLogFlag) {
            return;
        }
        if (msg == null) {
            msg = "null";
        }
        writeLogInfoToFile(LOG_FILE_PATH, MAX_LOG_FILE_LENGTH, msg + "\n");
    }

    /**
     * 不管是否debug，都会记录<br>
     * 崩溃异常信息<br>
     *
     * @param msg
     */
    public static void fileLogE(String msg) {
        writeLogInfoToFile(EXCEPTION_LOG_FILE_PATH, MAX_CRASHLOG_FILE_LENGTH,
                msg);
    }

    private static void log(String tag, String msg, Throwable tr, int logType) {
        // 如果关闭log，直接返回
        if (!gLogFlag) {
            return;
        }
        log(tag, msg + '\n' + getStackTraceString(tr), logType);
    }

    private static void writeLogInfoToFile(final String filePath,
            final int logFileLength, final String logInfo) {
        try {
            if (StorageUtil.isSDCardExist()) {
                // make sure file created
                final File file = new File(
                        StorageUtil.getDirByType(StorageUtil.DIR_TYPE_LOG),
                        filePath);
                if (file.exists()) {
                    if (file.length() > MAX_LOG_FILE_LENGTH) {
                        file.delete();
                    }
                }
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                // async write log
                new GenericTask() {

                    @Override
                    protected TaskResult doInBackground(TaskParams... params) {
                        FileWriter fw = null;
                        try {
                            fw = new FileWriter(file, true);
                            fw.write(logInfo);
                            fw.flush();
                        } catch (IOException e) {
                            LogUtil.e(e);
                        } finally {
                            if (fw != null) {
                                try {
                                    fw.close();
                                } catch (IOException e) {
                                    LogUtil.e(DEF_TAG, "write file log fail", e);
                                }
                            }
                        }
                        return null;
                    }
                }.execute();
            }
        } catch (Exception e) {
            LogUtil.e(DEF_TAG, "write file log fail", e);
        }
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }
}
