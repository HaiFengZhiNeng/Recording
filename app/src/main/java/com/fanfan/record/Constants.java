package com.fanfan.record;

import com.fanfan.record.utils.FileUtils;

import java.io.File;

public class Constants {

    public static int IMSDK_APPID = 1400043768;
    public static int IMSDK_ACCOUNT_TYPE = 17967;

    public static String USER_ACCOUNT = "hotel002";
    public static String USER_PWD = "1234567890";

    public static String START_UP = "fanw106";


    private static final String M_SDROOT_CACHE_PATH = FileUtils.getCacheDir(App.getInstance().getApplicationContext()) + File.separator;

    public static final String PRINT_LOG_PATH = M_SDROOT_CACHE_PATH + "print";


    public static final String ROLE_MASTER = "LiveMaster";
    public static int roomId = 12345678;

    public static final String HD_ROLE = "HD";
    public static final String SD_ROLE = "SD";
    public static final String LD_ROLE = "LD";
}
