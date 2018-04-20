package com.fanfan.record.utils;

import android.content.Context;
import android.os.Environment;

public class FileUtils {

    /**
     * @return 程序系统缓存目录
     */
    public static String getCacheDir(Context context) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

}
