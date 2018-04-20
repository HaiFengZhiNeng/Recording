package com.fanfan.record;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fanfan.record.permiss.HiPermission;
import com.fanfan.record.permiss.PermissionCallback;
import com.fanfan.record.utils.DialogUtils;
import com.robot.seabreeze.log.Log;
import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.adapter.CommonConstants;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;
import com.tencent.livesdk.ILVText;

public class MainActivity extends AppCompatActivity implements BaseHandler.HandleMessage,
        ILiveLoginManager.TILVBStatusListener, ILVLiveConfig.ILVLiveMsgListener {

    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    //handler
    private static final int REFRESH_COMPLETE = 0X153;
    private static final String PACKAGE_URL_SCHEME = "package:"; // 方案

    private static final int AGAIN_LOGIN = 0X154;

    private Handler mHandler = new BaseHandler<>(MainActivity.this);

    private MaterialDialog materialDialog;

    private AVRootView arvRoot;
    private Runnable infoRun = new Runnable() {
        @Override
        public void run() {
            ILiveQualityData qualityData = ILiveRoomManager.getInstance().getQualityData();
            if (null != qualityData) {
                String info = "上行速率:\t" + qualityData.getSendKbps() + "kbps\t"
                        + "上行丢包率:\t" + qualityData.getSendLossRate() / 100 + "%\n\n"
                        + "下行速率:\t" + qualityData.getRecvKbps() + "kbps\t"
                        + "下行丢包率:\t" + qualityData.getRecvLossRate() / 100 + "%\n\n"
                        + "应用CPU:\t" + qualityData.getAppCPURate() + "\t"
                        + "系统CPU:\t" + qualityData.getSysCPURate() + "\n\n";
//                Log.e(info);
            }
            if (ILiveRoomManager.getInstance().isEnterRoom()) {
                mHandler.postDelayed(infoRun, 2000);
            }
        }
    };

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (StatusObservable.getInstance().getObserverCount() > 0) {
            // 避免重复打开
            finish();
            return;
        }

        arvRoot = findViewById(R.id.arv_root);

        checkPermissions();

    }

    private void checkPermissions() {
        HiPermission.create(this)
                .checkMutiPermission(PERMISSIONS, new PermissionCallback() {
                    @Override
                    public void onClose() {
                        showMissingPermissionDialog();
                    }

                    @Override
                    public void onFinish() {
                        login();
                    }

                    @Override
                    public void onDeny(String permission, int position) {
                    }

                    @Override
                    public void onGuarantee(String permission, int position) {
                    }
                });

    }


    private void login() {
        showLoading();
        ILiveLoginManager.getInstance().tlsLoginAll(Constants.USER_ACCOUNT, Constants.USER_PWD, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                dismissLoading();
                ILiveLoginManager.getInstance().setUserStatusListener(StatusObservable.getInstance());
                StatusObservable.getInstance().addObserver(MainActivity.this);
                MessageObservable.getInstance().addObserver(MainActivity.this);

                record();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                mHandler.sendEmptyMessageDelayed(AGAIN_LOGIN, 500);
            }
        });
    }

    private void record() {
        ILVLiveManager.getInstance().setAvVideoView(arvRoot);

        arvRoot.setAutoOrientation(false);
        arvRoot.setSubCreatedListener(new AVRootView.onSubViewCreatedListener() {
            @Override
            public void onSubViewCreated() {
                arvRoot.renderVideoView(true, ILiveLoginManager.getInstance().getMyUserId(), CommonConstants.Const_VideoType_Camera, true);
            }
        });
        // 打开摄像头预览
        ILiveRoomManager.getInstance().enableCamera(ILiveConstants.FRONT_CAMERA, true);

        ILVLiveRoomOption option = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .autoCamera(true)
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.ROLE_MASTER)
                .autoFocus(true);
        ILVLiveManager.getInstance().createRoom(Constants.roomId, option, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                isRecording = true;
                afterCreate();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                if (module.equals(ILiveConstants.Module_IMSDK) && 10021 == errCode) {
                    // 被占用，改加入
                    showChoiceDlg();
                } else {
//                            Log.e("create failed : " + module + " , errCode : " + errCode + " errMsg : " + errMsg);
                }
            }
        });
    }

    private void unrecord() {
        if (ILiveConstants.NONE_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
            ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getActiveCameraId(), false);
        }

        ILiveRoomManager.getInstance().quitRoom(true, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                isRecording = false;
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                Log.e("create failed : " + module + " , errCode : " + errCode + " errMsg : " + errMsg);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILVLiveManager.getInstance().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ILVLiveManager.getInstance().onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ILiveConstants.NONE_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
            ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getActiveCameraId(), false);
        }
        StatusObservable.getInstance().deleteObserver(this);
        MessageObservable.getInstance().deleteObserver(this);
        ILVLiveManager.getInstance().onDestory();
    }

    private void showChoiceDlg() {
        DialogUtils.showBasicDialog(this, "提示", "房间已存在，是否加入房间？", "取消", "确定",
                new DialogUtils.OnNiftyDialogListener() {
                    @Override
                    public void onClickLeft() {

                    }

                    @Override
                    public void onClickRight() {
                        joinRoom();
                    }
                });
    }

    private void joinRoom() {
        ILVLiveRoomOption option = new ILVLiveRoomOption("")
                .autoCamera(ILiveConstants.NONE_CAMERA == ILiveRoomManager.getInstance().getActiveCameraId())
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.HD_ROLE)
                .autoFocus(true);
        ILVLiveManager.getInstance().joinRoom(Constants.roomId, option, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                afterCreate();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
//                Log.e("create failed : " + module + " , errCode : " + errCode + " errMsg : " + errMsg);
            }
        });
    }

    private void afterCreate() {

        //开关摄像头
        ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getCurCameraId(), false);
        //前后摄像头转换
//        if (ILiveConstants.NONE_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
//            ILiveRoomManager.getInstance().switchCamera(1 - ILiveRoomManager.getInstance().getActiveCameraId());
//        } else {
//            ILiveRoomManager.getInstance().switchCamera(ILiveConstants.FRONT_CAMERA);
//        }
        //录音开起
        ILiveRoomManager.getInstance().enableMic(true);

        mHandler.postDelayed(infoRun, 500);
    }

    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.help);
        builder.setMessage(R.string.string_help_text);

        // 拒绝, 退出应用
        builder.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHandler.sendEmptyMessageDelayed(REFRESH_COMPLETE, 1000);
            }
        });

        builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });

        builder.show();
    }


    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
        startActivity(intent);
    }

    private void showLoading() {
        if (materialDialog == null) {
            materialDialog = new MaterialDialog.Builder(this)
                    .title("请稍等...")
                    .content("正在获取中...")
                    .progress(true, 0)
                    .progressIndeterminateStyle(false)
                    .build();
        }
        materialDialog.show();
    }

    private void dismissLoading() {
        if (materialDialog != null && materialDialog.isShowing()) {
            materialDialog.dismiss();
            materialDialog = null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case REFRESH_COMPLETE:
                finish();
                break;
            case AGAIN_LOGIN:
                login();
                break;
        }
    }

    @Override
    public void onForceOffline(int error, String message) {
        new MaterialDialog.Builder(this)
                .content(R.string.str_tips_offline)
                .negativeText(R.string.confirm).onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                finish();
            }
        })
                .build()
                .show();
    }

    @Override
    public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
        Log.e("onNewTextMsg : " + text.getText());
        if (SenderId.equals(Constants.START_UP)) {

            if (text.getText().equals("1")) {
                if (!isRecording)
                    record();
            } else if (text.getText().equals("2")) {
                if (isRecording)
                    unrecord();

            }

        }
    }

    @Override
    public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {

    }

    @Override
    public void onNewOtherMsg(TIMMessage message) {

    }

}
