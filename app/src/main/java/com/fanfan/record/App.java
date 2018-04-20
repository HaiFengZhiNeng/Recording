package com.fanfan.record;

import android.app.Application;

import com.robot.seabreeze.log.Log;
import com.robot.seabreeze.log.inner.ConsoleTree;
import com.robot.seabreeze.log.inner.FileTree;
import com.robot.seabreeze.log.inner.LogcatTree;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.qalsdk.sdk.MsfSdkUtils;

public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        if (MsfSdkUtils.isMainProcess(this)) {
            // 初始化LiveSDK
            ILiveSDK.getInstance().setCaptureMode(ILiveConstants.CAPTURE_MODE_SURFACEVIEW);
            ILiveLog.setLogLevel(ILiveLog.TILVBLogLevel.DEBUG);
            ILiveSDK.getInstance().initSdk(this, Constants.IMSDK_APPID, Constants.IMSDK_ACCOUNT_TYPE);
            ILVLiveManager.getInstance().init(new ILVLiveConfig()
                    .setLiveMsgListener(MessageObservable.getInstance()));
        }

        if (BuildConfig.DEBUG) {
            Log.getLogConfig().configAllowLog(true).configShowBorders(false);
            Log.plant(new FileTree(this, Constants.PRINT_LOG_PATH));
            Log.plant(new ConsoleTree());
            Log.plant(new LogcatTree());
        }
    }
}
