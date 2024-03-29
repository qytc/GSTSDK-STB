package io.qytc.stb;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.teduboard.TEduBoardController;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudDef.TRTCParams;
import com.tencent.trtc.TRTCCloudListener;
import com.tencent.trtc.TRTCStatistics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class VideoActivity extends Activity implements View.OnClickListener {
    private final static String TAG = VideoActivity.class.getSimpleName();

    private TRTCParams trtcParams;     /// TRTC SDK 视频通话房间进入所必须的参数
    private TRTCCloud trtcCloud;              /// TRTC SDK 实例对象

    public static final int LIVER = 20;//发言人
    public static final int WATCHER = 21;//观众
    private int mRole = WATCHER;

    private TRTCVideoViewLayout mVideoViewLayout;
    private RelativeLayout mDrawer;
    private TextView mTvMicStatus, mTvVideoStatus;//麦克风，摄像头
    private TextView mTvRequestLiver;//申请发言
    private TextView mTvRoomInfo;//房间信息（显示房间号）

    private Dialog mExitVideoDialog;//退出会议对话框

    private int mRoomNo;
    private String mUserId;
    private int mSdkAppId;
    private String mUserSig;

    private boolean mIsEnableVideo = false, mIsEnableAudio = false; // 是否开启视频\音频上行
    private Handler mHandler;

    public Map<String, String> mMemberMap = new HashMap<>();
    private TEduBoardController mBoard;
    private FrameLayout mBoardController;
    private boolean chairmanMuteMicAll = false;
    private String mChairman;
    private TRTCCloudListenerImpl mTrtcCloudListener;
    private TXCloudVideoView mShare_video_view;
    private TextView mTvNetSpeed;
    private String mAliveJsonStr;
    private StatusBean mStatusBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initParams();//初始化sdk和参数
        initView();//初始化 UI 控件
        initSocket();//加入房间开始心跳
    }

    private void initParams() {
        Intent intent = getIntent();

        mSdkAppId = intent.getIntExtra(ThirdLoginConstant.SDKAPPID, -1);
        mUserId = intent.getStringExtra(ThirdLoginConstant.USERID);
        mUserSig = intent.getStringExtra(ThirdLoginConstant.USERSIG);
        mRoomNo = intent.getIntExtra(ThirdLoginConstant.ROOMID, -1);
        trtcParams = new TRTCParams(mSdkAppId, mUserId, mUserSig, mRoomNo, "", "");
        mRole = intent.getIntExtra(ThirdLoginConstant.ROLE, WATCHER);
    }

    /**
     * 初始化界面控件，包括主要的视频显示View
     */
    private void initView() {
        setContentView(R.layout.activity_video);
        mVideoViewLayout = findViewById(R.id.TRTCVideoViewLayout);

        mDrawer = findViewById(R.id.drawerlayout);
        mTvMicStatus = findViewById(R.id.tv_mic_status);
        mTvMicStatus.setOnClickListener(this);
        mTvVideoStatus = findViewById(R.id.tv_video_status);
        mTvVideoStatus.setOnClickListener(this);
        mTvRequestLiver = findViewById(R.id.tv_request_liver);
        mTvRequestLiver.setOnClickListener(this);

        mTvRoomInfo = findViewById(R.id.tv_room_info);
        mTvRoomInfo.setText("房间号:" + mRoomNo);

        mShare_video_view = findViewById(R.id.share_video_view);

        mTvNetSpeed = findViewById(R.id.tv_net_speed);

        mTvNetSpeed.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }

    private void initRoomParams() {
        if (trtcCloud == null) {
            trtcCloud = TRTCCloud.sharedInstance(this);//创建 TRTC SDK 实例
        }

        if (mTrtcCloudListener == null) {
            mTrtcCloudListener = new TRTCCloudListenerImpl(this);
        }

        trtcCloud.setListener(mTrtcCloudListener);

        // 预览前配置默认参数
        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();     // 大画面的编码器参数设置
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540;     //分辨率   // 注意（1）：不要在码率很低的情况下设置很高的分辨率，会出现较大的马赛克
        encParam.videoFps = 15;    //帧率15    // 注意（2）：不要设置超过25FPS以上的帧率，因为电影才使用24FPS，我们一般推荐15FPS，这样能将更多的码率分配给画质
        encParam.videoBitrate = 1500;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_LANDSCAPE;
        trtcCloud.setVideoEncoderParam(encParam);

        TRTCCloudDef.TRTCNetworkQosParam qosParam = new TRTCCloudDef.TRTCNetworkQosParam();
        qosParam.controlMode = TRTCCloudDef.VIDEO_QOS_CONTROL_SERVER;//服务端控制
        qosParam.preference = TRTCCloudDef.TRTC_VIDEO_QOS_PREFERENCE_SMOOTH;//优先清晰
        trtcCloud.setNetworkQosParam(qosParam);

        trtcCloud.setPriorRemoteVideoStreamType(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);//设置视频流类型为大画面

        // 如果当前角色是主播
        if (mRole == LIVER) {
            showRole(LIVER);
            startLocalVideo(true);
            trtcCloud.startLocalAudio();
        }

        trtcCloud.setLocalViewFillMode(TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);//适应模式
        trtcCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);//音频线路为：扬声器
        trtcCloud.setGSensorMode(TRTCCloudDef.TRTC_GSENSOR_MODE_UIFIXLAYOUT);//UI 不适应布局 （无重力感应）

        trtcCloud.enterRoom(trtcParams, TRTCCloudDef.TRTC_APP_SCENE_LIVE);
    }

    private static class MyBoardCallback implements TEduBoardController.TEduBoardCallback {
        WeakReference<VideoActivity> mActivityRef;

        MyBoardCallback(VideoActivity activityEx) {
            mActivityRef = new WeakReference<>(activityEx);
        }

        @Override
        public void onTEBError(int i, String s) {

        }

        @Override
        public void onTEBWarning(int i, String s) {

        }

        @Override
        public void onTEBInit() {
            Log.i(TAG, "onTEBInit");
            VideoActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.addBoardView();
            }
        }

        @Override
        public void onTEBHistroyDataSyncCompleted() {
            Log.i(TAG, "onTEBHistroyDataSyncCompleted");
        }

        @Override
        public void onTEBSyncData(String s) {
            Log.i(TAG, "onTEBSyncData  data=" + s);
        }

        @Override
        public void onTEBUndoStatusChanged(boolean b) {

        }

        @Override
        public void onTEBRedoStatusChanged(boolean b) {

        }

        @Override
        public void onTEBImageStatusChanged(String s, String s1, int i) {

        }

        @Override
        public void onTEBSetBackgroundImage(String s) {

        }

        @Override
        public void onTEBBackgroundH5StatusChanged(String s, String s1, int i) {

        }

        @Override
        public void onTEBAddBoard(List<String> list, String s) {

        }

        @Override
        public void onTEBDeleteBoard(List<String> list, String s) {

        }

        @Override
        public void onTEBGotoBoard(String s, String s1) {

        }

        @Override
        public void onTEBGotoStep(int i, int i1) {

        }

        @Override
        public void onTEBAddFile(String s) {

        }

        @Override
        public void onTEBAddH5PPTFile(String s) {

        }

        @Override
        public void onTEBAddTranscodeFile(String s) {

        }

        @Override
        public void onTEBDeleteFile(String s) {

        }

        @Override
        public void onTEBSwitchFile(String s) {

        }

        @Override
        public void onTEBFileUploadProgress(String s, int i, int i1, int i2, float v) {

        }

        @Override
        public void onTEBFileUploadStatus(String s, int i, int i1, String s1) {

        }
    }

    private void joinClass() {
//        mBoard = TrtcManager.getInstance().getBoard();

//        TrtcManager.getInstance().joinClassRoom(trtcParams, new MyBoardCallback(this));
    }

    private void switchBoard(boolean open) {
        if (open) {
            joinClass();
        } else {
            quitClassRoom();
        }
    }

    private void quitClassRoom() {
        TrtcManager.getInstance().quitClassRoom();
        TrtcManager.getInstance().quitClassRoom();

        if (mBoardController != null) {
            mBoardController.removeAllViews();
        }
    }

    private void addBoardView() {
        mBoardController = findViewById(R.id.board_view_container);
        View boardview = mBoard.getBoardRenderView();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ViewGroup parent = (ViewGroup) boardview.getParent();
        if (parent != null) {
            parent.removeView(boardview);
        }
        mBoardController.addView(boardview, layoutParams);
    }

    private static class TRTCCloudListenerImpl extends TRTCCloudListener implements TRTCCloudListener.TRTCVideoRenderListener {
        private WeakReference<VideoActivity> mContext; //弱引用ScreenAv
        private boolean mShareScreen = false;//当前是否在屏幕共享
        private String mShareUserId;//当前屏幕共享的ID

        public TRTCCloudListenerImpl(VideoActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        /**
         * 当前用户首先加入房间
         */
        @Override
        public void onEnterRoom(long elapsed) {
            Log.i(TAG, "--onEnterRoom() , elapsed =" + elapsed);

        }

        /**
         * 当前用户离开房间
         */
        @Override
        public void onExitRoom(int reason) {
            Log.i(TAG, "--onExitRoom() , reason =" + reason);
            VideoActivity activity = mContext.get();
            if (activity != null) {
                activity.finish();
            }
        }

        /**
         * 发生错误 ERROR
         * 大多是不可恢复的错误，需要通过 UI 提示用户
         */
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.i(TAG, "--onError() , errCode = " + errCode + " errMsg =" + errMsg);
            VideoActivity activity = mContext.get();
            if (activity != null) {
                ToastUtils.toast(activity, "发生错误: " + errMsg + "[" + errCode + "]");
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {  //严重错误，退出房间
                    activity.httpExitRoom();
                    activity.exitRoom();
                }
            }
        }

        /**
         * 一些警告 WARNING
         * 大多是一些可以忽略的事件通知，SDK内部会启动一定的补救机制
         */
        @Override
        public void onWarning(int warningCode, String warningMsg, Bundle extraInfo) {
            Log.i(TAG, "--onWarning() , warningCode = " + warningCode + " warningMsg =" + warningMsg);
        }

        /**
         * 有新的用户加入了当前视频房间
         */
        @Override
        public void onUserEnter(String userId) {
            Log.i(TAG, "--onUserEnter() , userId = " + userId);
        }

        /**
         * 有用户离开了当前视频房间
         */
        @Override
        public void onUserExit(String userId, int reason) {
            Log.i(TAG, "--onUserExit() , userId = " + userId + " reason = " + reason);
            VideoActivity activity = mContext.get();
            if (activity != null) {
                activity.trtcCloud.stopRemoteView(userId);
                activity.mVideoViewLayout.onMemberLeave(userId);
            }
        }

        /**
         * 当用户开始视频流或关闭视频流
         */
        @Override
        public void onUserVideoAvailable(final String userId, boolean available) {
            Log.i(TAG, "--onUserVideoAvailable() , userId = " + userId + " available = " + available);
            VideoActivity activity = mContext.get();
            if (activity != null) {
                if (available) {
                    TXCloudVideoView renderView = activity.mVideoViewLayout.onMemberEnter(userId);
                    if (renderView != null) {
                        renderView.setVisibility(View.VISIBLE);
                        activity.trtcCloud.setRemoteViewFillMode(userId, TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);//适应模式
                        activity.trtcCloud.startRemoteView(userId, renderView);
                        activity.mVideoViewLayout.showAllText();
                    }

                    if (activity.mVideoViewLayout.mBroadcastArr != null) {
                        activity.mVideoViewLayout.setScreen();
                    }

                } else {
                    activity.trtcCloud.stopRemoteView(userId);
                    activity.mVideoViewLayout.onMemberLeave(userId);
                }
            }
        }

        /**
         * 收到远端用户的屏幕分享画面
         */
        @Override
        public void onUserSubStreamAvailable(final String userId, boolean available) {
            Log.i(TAG, "--onUserSubStreamAvailable() , userId = " + userId + " , available = " + available);
            VideoActivity activity = mContext.get();
            if (activity != null) {
                if (mShareUserId == null && available && userId != null) {
                    mShareScreen = true;
                    mShareUserId = userId;

                    activity.mVideoViewLayout.setCanZoomFullscreen(false);
                    activity.mShare_video_view.setVisibility(View.VISIBLE);
                    activity.mShare_video_view.setUserId(userId);

                    activity.trtcCloud.startRemoteSubStreamView(userId, activity.mShare_video_view);

                } else if (mShareScreen && userId != null && userId.equals(mShareUserId)) {
                    activity.mVideoViewLayout.setCanZoomFullscreen(true);
                    activity.trtcCloud.stopRemoteSubStreamView(mShareUserId);
                    activity.mShare_video_view.setVisibility(View.INVISIBLE);

                    TXCloudVideoView videoView = activity.mVideoViewLayout.getVideoViewByUserId(userId);
                    if (videoView == null) {
                        videoView = activity.mVideoViewLayout.onMemberEnter(mShareUserId);
                    }
                    videoView.setVisibility(View.VISIBLE);
                    activity.trtcCloud.setRemoteViewFillMode(userId, TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);//适应模式
                    activity.trtcCloud.startRemoteView(mShareUserId, videoView);

                    mShareUserId = null;
                    mShareScreen = false;
                }
            }
        }

        /**
         * 有用户获取了声音
         */
        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            Log.i(TAG, "--onUserAudioAvailable() , userId = " + userId + " , available = " + available);
        }

        /**
         * 首帧渲染回调
         */
        @Override
        public void onFirstVideoFrame(String userId, int width, int height) {
            Log.i(TAG, "--onFirstVideoFrame() , userId = " + userId);
        }

        public void onStartPublishCDNStream(int err, String errMsg) {

        }

        public void onStopPublishCDNStream(int err, String errMsg) {

        }

        public void onRenderVideoFrame(String userId, int streamType, TRTCCloudDef.TRTCVideoFrame frame) {
            Log.i(TAG, "--onRenderVideoFrame() , userId = " + userId + " streamType = " + streamType);
        }

        public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {

        }

        private long sendBytes = 0;
        private long receiveBytes = 0;

        public void onStatistics(TRTCStatistics statics) {
            long tempSend = statics.sendBytes - sendBytes;
            long tempReceive = statics.receiveBytes - receiveBytes;
            sendBytes = statics.sendBytes;
            receiveBytes = statics.receiveBytes;


            String netStatus = "网络上行：" + (tempSend / 1024) + "kb/s , 网络下行：" + (tempReceive / 1024) + "kb/s";

            VideoActivity activity = mContext.get();
            if (activity != null) {
                activity.mTvNetSpeed.setText(netStatus);
            }
        }

        @Override
        public void onConnectOtherRoom(final String userID, final int err, final String errMsg) {

        }

        @Override
        public void onDisConnectOtherRoom(final int err, final String errMsg) {

        }

        @Override
        public void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, ArrayList<TRTCCloudDef.TRTCQuality> remoteQuality) {

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_3) {//八爪鱼麦克风开关按钮
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDrawer != null && mDrawer.getVisibility() == View.VISIBLE) {
                closeDrawer();
            } else if (mExitVideoDialog != null && mExitVideoDialog.isShowing()) {
                mExitVideoDialog.dismiss();
            } else {
                loadExitDialog();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mDrawer != null && mDrawer.getVisibility() == View.GONE) {
                openDrawer();
            } else {
                closeDrawer();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //打开侧边栏
    private void openDrawer() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mDrawer, "translationX", 0, WindowUtils.dip2px(this, 300)).setDuration(500);
        anim.addListener(new ShowLayoutAnimListener(mDrawer, 0, mTvMicStatus));
        anim.start();
    }

    //关闭侧边栏
    private void closeDrawer() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mDrawer, "translationX", WindowUtils.dip2px(this, 300)).setDuration(500);
        anim.addListener(new ShowLayoutAnimListener(mDrawer, 1, null));
        anim.start();
    }

    private void loadExitDialog() {
        if (mExitVideoDialog == null) {
            mExitVideoDialog = new Dialog(this, R.style.dialogstyle);
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exit_video, null);
        view.findViewById(R.id.tv_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissDialog(mExitVideoDialog);
                httpExitRoom();
                exitRoom();
            }
        });

        view.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissDialog(mExitVideoDialog);
            }
        });
        mExitVideoDialog.setContentView(view);
        mExitVideoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mExitVideoDialog.show();
    }

    /**
     * 退出视频房间
     */
    private void exitRoom() {
        quitClassRoom();
        if (mBoardController != null) {
            mBoardController.removeAllViews();
        }

        if (trtcCloud != null) {
            trtcCloud.exitRoom();
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mTaskRunnable);
        }
        if (webSocket != null) {
            webSocket.close(1001, "close");
        }
    }

    //开闭本地画面
    private void startLocalVideo(boolean enable) {
        if (enable) {
            TXCloudVideoView localVideoView = mVideoViewLayout.onMemberEnter(mUserId);
            if (localVideoView == null) {
                return;
            }
            localVideoView.setUserId(mUserId);
            localVideoView.setVisibility(View.VISIBLE);
            trtcCloud.startLocalPreview(true, localVideoView);//启动SDK摄像头采集和渲染

            if (mVideoViewLayout.mBroadcastArr != null) {
                mVideoViewLayout.setScreen();
            }
            mVideoViewLayout.showAllText();
        } else {
            mVideoViewLayout.onMemberLeave(mUserId);
            trtcCloud.stopLocalPreview();
        }
        DevicesUtils.checkOritation(trtcCloud);

        showVideo(enable);
        switchMic(enable);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tv_mic_status) {
            if (mRole == WATCHER) {
                ToastUtils.toast(this, "请先申请发言");
            } else {
                if (chairmanMuteMicAll) {
                    ToastUtils.toast(this, "主席已禁麦本会场");
                } else {
                    switchMic(!mIsEnableAudio);
                }
            }
        } else if (id == R.id.tv_video_status) {
            if (mRole == WATCHER) {
                ToastUtils.toast(this, "请先申请发言");
            } else {
                httpCancelSpeak();
            }
        } else if (id == R.id.tv_request_liver) {
            closeDrawer();
            if (mRole == LIVER) {
                httpCancelSpeak();
            } else {
                httpRequestSpeak();
            }
        }
    }

    //显示麦克风开关
    private void switchMic(boolean status) {
        if (chairmanMuteMicAll) return;
        trtcCloud.muteLocalAudio(!status);
        if (status) {
            mTvMicStatus.setText("麦克风：开");
        } else {
            mTvMicStatus.setText("麦克风：关");
        }
        mIsEnableAudio = status;

        mStatusBean.getData().setMic(mIsEnableAudio ? "1" : "0");
        mAliveJsonStr = JSON.toJSONString(mStatusBean);
        sendKeepAlive();
    }

    //显示摄像头开关
    private void showVideo(boolean status) {
        //显示改变后的状态
        if (status) {
            mTvVideoStatus.setText("摄像头：开");
        } else {
            mTvVideoStatus.setText("摄像头：关");
        }
        mIsEnableVideo = status;

        mStatusBean.getData().setCamera(mIsEnableVideo ? "1" : "0");
        mAliveJsonStr = JSON.toJSONString(mStatusBean);
        sendKeepAlive();
    }

    /**
     * 切换角色
     */
    private void switchRole(int role) {
        if (role == LIVER) {
            showRole(LIVER);
            initRoomParams();
        } else {
            showRole(WATCHER);
            startLocalVideo(false);//关闭本地画面
            mVideoViewLayout.onMemberLeave(mUserId);
        }
    }

    //显示申请/取消发言文字
    private void showRole(int role) {
        mRole = role;
        if (role == LIVER) {
            mTvRequestLiver.setText("退出发言");
        } else {
            mTvRequestLiver.setText("申请发言");
        }
    }

    //关闭任意对话框
    private void dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }


    private OkHttpClient mOkHttpClient;
    protected okhttp3.WebSocket webSocket;
    private boolean webSocketConnect;
    private int errorCount = 0;


    private void initSocket() {
        initAliveData();

        restartWebSocket();

        mHandler = new Handler();

        mTaskRunnable.run();
    }

    private void initAliveData() {
        mStatusBean = new StatusBean();
        mStatusBean.setCmd("keepAlive");

        StatusBean.Data data = new StatusBean.Data();
        data.setAcctno(mUserId);
        data.setCamera(mIsEnableVideo ? "1" : "0");
        data.setDeviceId("stb-" + mUserId);
        data.setMic(mIsEnableAudio ? "1" : "0");
        data.setSpeaker("1");
        data.setInRoom("1");
        data.setRoomNo(String.valueOf(mRoomNo));
        mStatusBean.setData(data);

        mAliveJsonStr = JSON.toJSONString(mStatusBean);
    }

    private Runnable mTaskRunnable = new Runnable() {
        @Override
        public void run() {
            sendData();
            mHandler.postDelayed(this, 1000 * 5);
        }
    };


    public void restartWebSocket() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        mOkHttpClient = builder.build();

        Request request = new Request.Builder().url(Urls.KEEPLIVE_URL + "?acctno=" + mUserId + "&roomNo=" + mRoomNo).build();
        webSocket = mOkHttpClient.newWebSocket(request, new EchoWebSocketListener());
        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    private final class EchoWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.i(TAG, "WebSocket 连接成功");
            webSocketConnect = true;
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.i(TAG, "ByteString onMessage: " + bytes.toString());
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, final String text) {
            super.onMessage(webSocket, text);
            VideoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handlerMsg(text);
                }
            });
        }

        @Override
        public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            Log.i(TAG, "WebSocket 连接已关闭");
            webSocketConnect = false;
        }

        @Override
        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            Log.i(TAG, "WebSocket 正在关闭");
            webSocketConnect = false;
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            Log.e(TAG, "WebSocket 连接出错");
            webSocketConnect = false;
        }
    }

    public void sendData() {
        if (webSocketConnect && webSocket != null) {
            errorCount = 0;//重新计数
            sendKeepAlive();
        } else {
            errorCount++;
            if (errorCount == 2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.toast(VideoActivity.this, "网络连接异常，请重新加入房间");
                        exitRoom();
                    }
                });
                errorCount = 0;
            }

            restartWebSocket();
        }
    }

    private void sendKeepAlive() {
        if (webSocketConnect) {
            webSocket.send(mAliveJsonStr);
        }
    }

    private void handlerMsg(String s) {
        JSONObject jsonObject = JSON.parseObject(s);
        if (jsonObject == null) {
            return;
        }
        String cmd = jsonObject.getString("cmd");
        if (cmd == null) {
            return;
        }
        Log.i(TAG, "cmd=" + cmd);
        String roomNo = jsonObject.getString("roomNo");
        if (!TextUtils.isEmpty(roomNo) && !String.valueOf(mRoomNo).equals(roomNo)) {
            return;
        }
        if (cmd.equals(LogicConstant.CONFIRM_SPEAK)) {
            String result = jsonObject.getString("result");
            ToastUtils.toast(VideoActivity.this, "主席已" + ("1".equals(result) ? "同意" : "拒绝") + "您的发言申请");
            if ("1".equals(result)) {
                switchRole(LIVER);
            }
        } else if (cmd.equals(LogicConstant.CONTROL_MIC)) {
            String allMute = jsonObject.getString("allMute");
            String micResult = jsonObject.getString("result");
            if (TextUtils.isEmpty(allMute)) {
                //单人开闭音
                if (mRole == LIVER) {
                    ToastUtils.toast(VideoActivity.this, "主席已" + ("1".equals(micResult) ? "打开" : "关闭") + "您的麦克风");
                    switchMic("1".equals(micResult));
                }
            } else {
                //全体开闭音
                chairmanMuteMicAll = false;
                if (mRole == LIVER) {
                    switchMic("0".equals(micResult));
                }
                chairmanMuteMicAll = "1".equals(micResult);
                ToastUtils.toast(VideoActivity.this, "主席已" + ("1".equals(micResult) ? "开启" : "取消") + "全场闭麦");
            }
        } else if (cmd.equals(LogicConstant.CONTROL_CAMERA)) {
            String cameraResult = jsonObject.getString("result");
            if (mRole == LIVER && "0".equals(cameraResult)) {
                ToastUtils.toast(VideoActivity.this, "已被主席取消发言");
                switchRole(WATCHER);
            } else if ("1".equals(cameraResult)) {
                switchRole(LIVER);
            }
        } else if (cmd.equals(LogicConstant.INVITE_SPEAK)) {
            switchRole(LIVER);
        } else if (cmd.equals(LogicConstant.CANCEL_SPEAK)) {
            switchRole(WATCHER);
        } else if (cmd.equals(LogicConstant.MULTI_SCREEN)) {
            String users = jsonObject.getString("layout");
            String[] acctnoArray = users.split(",");
            // 补齐数组
            if (acctnoArray.length < 5) {
                String[] userIds = Arrays.copyOf(acctnoArray, 5);
                for (int i = acctnoArray.length; i < userIds.length; i++) {
                    userIds[i] = "-1";
                }
                mVideoViewLayout.setBroadcastArr(userIds);
                mVideoViewLayout.setScreen();
                return;
            } else if (acctnoArray.length > 5) {
                String[] userIds = Arrays.copyOf(acctnoArray, 5);
                mVideoViewLayout.setBroadcastArr(userIds);
                mVideoViewLayout.setScreen();
                return;
            }
            mVideoViewLayout.setBroadcastArr(acctnoArray);
            mVideoViewLayout.setScreen();
        } else if (cmd.equals(LogicConstant.FORCE_EXIT)) {
            String msg = jsonObject.getString("msg");
            ToastUtils.toast(VideoActivity.this, msg);
            exitRoom();
        } else if (cmd.equals(LogicConstant.JOIN_ROOM)) {
            String data = jsonObject.getString("memberList");
            List<UserInfo> infos = JSON.parseArray(data, UserInfo.class);
            for (UserInfo user : infos) {
                mMemberMap.put(user.getAcctno(), user.getName());
            }
            String chairman = jsonObject.getString("chairman");

            if (mUserId.equals(chairman)) {
                ToastUtils.toast(VideoActivity.this, "当前账号为主席账号，请使用Windows客户端登陆该账号");
                finish();
                return;
            }

            mChairman = chairman;
            initRoomParams();
        } else if (cmd.equals("board")) {
//            String status = jsonObject.getString("status");
//            switchBoard("1".equals(status));
        } else if (cmd.equals("watch")) {
            String broadcast = jsonObject.getString("broadcast");
            String brUserId = jsonObject.getString("acctno");

            mVideoViewLayout.setCanZoomFullscreen(false);

            if ("off".equals(broadcast)) {
                brUserId = null;
                mVideoViewLayout.setCanZoomFullscreen(true);
            }
            mVideoViewLayout.zoom(brUserId);
        } else if (cmd.equals("transfer_role")) {
            String newChairman = jsonObject.getString("newChairman");
            mChairman = newChairman;
        }
    }

    //申请发言
    private void httpRequestSpeak() {
        Map<String, String> map = new HashMap<>();
        map.put("acctno", mUserId);
        map.put("roomNo", String.valueOf(mRoomNo));
        MyHttpsUtils.post(Urls.REQUEST_SPEAK, map, this, new MyHttpsUtils.ApiBackCall() {
            @Override
            public void onFail() {
                ToastUtils.toast(VideoActivity.this, "网络请求异常");
            }

            @Override
            public void onSuccess(String result) {
                if (MyHttpsUtils.isSuccessCode(result)) {
                    ToastUtils.toast(VideoActivity.this, "请求成功，请等待主席允许");
                } else {
                    ToastUtils.toast(VideoActivity.this, MyHttpsUtils.getMsg(result));
                }
            }
        });
    }

    //退出房间
    private void httpExitRoom() {
        Map<String, String> map = new HashMap<>();
        map.put("acctno", mUserId);
        map.put("roomNo", String.valueOf(mRoomNo));

        MyHttpsUtils.post(Urls.EXIT_ROOM, map, this, null);
    }

    //取消发言
    private void httpCancelSpeak() {
        Map<String, String> map = new HashMap<>();
        map.put("acctno", mChairman);
        map.put("target", mUserId);
        map.put("roomNo", String.valueOf(mRoomNo));
        MyHttpsUtils.post(Urls.CANCEL_SPEAK, map, this, new MyHttpsUtils.ApiBackCall() {
            @Override
            public void onFail() {
                ToastUtils.toast(VideoActivity.this, "网络请求异常");
            }

            @Override
            public void onSuccess(String result) {
                if (MyHttpsUtils.isSuccessCode(result)) {
                    switchRole(WATCHER);
                } else {
                    ToastUtils.toast(VideoActivity.this, MyHttpsUtils.getMsg(result));
                }
            }
        });

    }
}