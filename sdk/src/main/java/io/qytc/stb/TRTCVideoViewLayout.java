package io.qytc.stb;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.rtmp.ui.TXCloudVideoView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TRTCVideoViewLayout extends RelativeLayout {
    // 默认布局参数布局
    final static String DEFAULT_USERID = "-1";

    private VideoActivity mActivity;

    private static LayoutParams[] mGridParamArr = new LayoutParams[6];//画面参数数组
    private static TXCloudVideoView[] mVideoViewArr = new TXCloudVideoView[5];//所有的画面播放控件数组
    private static String[] mLiverArr = new String[5];//布局数组
    private final int mScreenWidth; //屏幕宽度
    private final int mScreenHeight;   //屏幕高度
    private Handler mHandler = new Handler();

    private String mFullScreenUserIdTag = DEFAULT_USERID;//当前全屏你画面用户id
    private long mOperateTime;//操作放大缩小的时间戳
    private boolean mCanZoomFullscreen = true;//是否可以操作放大或缩小画面
    public String[] mBroadcastArr;//广播多画面的ID数组

    public TRTCVideoViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (VideoActivity) context;
        mScreenWidth = WindowUtils.getScreenWidth(context);
        mScreenHeight = WindowUtils.getScreenHeight(context);
        init();
    }

    /**
     * 初始化操作
     */
    private void init() {
        //布局数组参数初始化
        mLiverArr = new String[]{DEFAULT_USERID, DEFAULT_USERID, DEFAULT_USERID, DEFAULT_USERID, DEFAULT_USERID};//发言人ID数组

        //画面参数初始化
        initGridParam();

        //初始化生成 待显示画面的控件
        for (int i = 0; i < mVideoViewArr.length; i++) {
            TXCloudVideoView txCloudVideoView = initVideoView();
            txCloudVideoView.setLayoutParams(mGridParamArr[i]);
            mVideoViewArr[i] = txCloudVideoView;
        }
    }

    /**
     * 初始化画面参数组
     */
    private void initGridParam() {
        int screenW = mScreenWidth / 4;
        int screenH = mScreenHeight / 4;

        // 主席画面参数
        mGridParamArr[0] = new LayoutParams(screenW * 3, screenH * 3);
        mGridParamArr[0].addRule(RelativeLayout.CENTER_HORIZONTAL);

        // 发言人画面参数
        for (int i = 1; i < mVideoViewArr.length; i++) {
            mGridParamArr[i] = new LayoutParams(screenW, screenH);
            mGridParamArr[i].leftMargin = screenW * (i - 1);
            mGridParamArr[i].addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }

        // 大画面参数
        mGridParamArr[5] = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private TXCloudVideoView initVideoView() {
        final TXCloudVideoView cloudVideoView = new TXCloudVideoView(mActivity);
        cloudVideoView.setVisibility(GONE);
        cloudVideoView.setUserId(DEFAULT_USERID);
        addView(cloudVideoView);
        cloudVideoView.setFocusable(true);
        cloudVideoView.setBackgroundResource(R.drawable.video_focused_selector);
        cloudVideoView.setPadding(1, 1, 1, 1);
        cloudVideoView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mCanZoomFullscreen) {
                    return;
                }
                long time = System.currentTimeMillis();
                if (time - mOperateTime < 2000) {
                    return;
                }
                focusVideoView(cloudVideoView);
            }
        });
        return cloudVideoView;
    }

    public void setCanZoomFullscreen(boolean canZoomFullscreen) {
        mCanZoomFullscreen = canZoomFullscreen;
    }

    //在视频画面底部显示文字
    private void showText(TXCloudVideoView videoView) {
        String userId = videoView.getUserId();
        TextView textView = videoView.findViewById(videoView.hashCode());
        if (textView == null) {
            textView = new TextView(mActivity);
            textView.setId(videoView.hashCode());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            layoutParams.bottomMargin = 5;
            textView.setTextSize(28);
            textView.setTextColor(Color.WHITE);
            videoView.addView(textView, layoutParams);
        }
        String name = mActivity.mMemberMap.get(userId);
        if (TextUtils.isEmpty(name)) {
            textView.setText("");
        } else {
            textView.setText(name);
        }
        final TextView finalTextView = textView;
        //延时显示文字，防止被视频遮挡
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finalTextView.setVisibility(VISIBLE);
                finalTextView.bringToFront();
            }
        }, 5000);
    }

    //刷新全部画面会场名称
    public void showAllText() {
        for (TXCloudVideoView videoView : mVideoViewArr) {
            showText(videoView);
        }
    }

    /**
     * 有视频流需要加入
     */
    public TXCloudVideoView onMemberEnter(String userId) {
        if (userId == null) return null;

        if (mCanZoomFullscreen) {
            cancelZoom();//变换画面之前切换为原始缩小画面
        }

        //存在即返回
        for (int i = 0; i < mLiverArr.length; i++) {
            if (userId.equals(mLiverArr[i])) {
                return mVideoViewArr[i];
            }
        }

        //不存在即新找
        for (int i = 0; i < mLiverArr.length; i++) {
            if (DEFAULT_USERID.equals(mLiverArr[i])) {
                mLiverArr[i] = userId;

                if (mBroadcastArr != null) {
                    for (int j = 0; j < mBroadcastArr.length; j++) {
                        if (DEFAULT_USERID.equals(mBroadcastArr[j])) {
                            mBroadcastArr[j] = userId;
                            break;
                        }
                    }
                }

                showText(mVideoViewArr[i]);
                return mVideoViewArr[i];
            }
        }
        return null;
    }

    /**
     * 有视频流离开
     *
     * @param userId
     */
    public void onMemberLeave(String userId) {
        if (userId == null) return;

        if (mCanZoomFullscreen) {
            cancelZoom();//变换画面之前切换为原始缩小画面
        }

        for (int i = 0; i < mLiverArr.length; i++) {
            if (userId.equals(mLiverArr[i])) {
                TXCloudVideoView txCloudVideoView = mVideoViewArr[i];
                txCloudVideoView.setVisibility(GONE);
                txCloudVideoView.setUserId(DEFAULT_USERID);
                mLiverArr[i] = DEFAULT_USERID;

                if (mBroadcastArr != null) {
                    for (int j = 0; j < mBroadcastArr.length; j++) {
                        if (userId.equals(mBroadcastArr[j])) {
                            mBroadcastArr[j] = DEFAULT_USERID;
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    //放大/取消画面
    public void focusVideoView(TXCloudVideoView videoView) {
        if (mFullScreenUserIdTag.equals(DEFAULT_USERID)) {
            // 如果没有放大的用户，此时就放大
            for (int i = 0; i < mVideoViewArr.length; i++) {
                if (mVideoViewArr[i].getUserId().equals(videoView.getUserId())) {
                    videoView.setLayoutParams(mGridParamArr[5]);
                    videoView.bringToFront();
                    break;
                }
            }
            mFullScreenUserIdTag = videoView.getUserId();
        } else {
            mOperateTime = System.currentTimeMillis();
            // 如果有放大的用户，此时就缩小
            for (int i = 0; i < mVideoViewArr.length; i++) {
                if (mVideoViewArr[i].getUserId().equals(videoView.getUserId())) {
                    videoView.setLayoutParams(mGridParamArr[i]);
                    break;
                }
            }
            mFullScreenUserIdTag = DEFAULT_USERID;

            if (mBroadcastArr != null) {
                setScreen();
            }
        }

        videoView.requestFocus();
    }

    //还原焦点
    public void resetFocus() {
        if (mFullScreenUserIdTag.equals(DEFAULT_USERID)) {
            //没有放大时，焦点还原到主席画框
            mVideoViewArr[0].requestFocus();
        } else {
            //放大时，焦点还原到放大画框
            for (int i = 0; i < mVideoViewArr.length; i++) {
                if (mVideoViewArr[i].equals(mFullScreenUserIdTag)) {
                    mVideoViewArr[i].requestFocus();
                    break;
                }
            }
        }
    }

    //广播/取消广播指定会场
    public void zoom(String userID) {
        if (userID == null) {
            cancelZoom();
            if (mBroadcastArr != null) {
                setScreen();
            }
            return;
        }

        if (!DEFAULT_USERID.equals(mFullScreenUserIdTag)) {
            cancelZoom();
        }

        for (int i = 0; i < mLiverArr.length; i++) {
            if (mLiverArr[i].equals(userID)) {
                mVideoViewArr[i].setVisibility(VISIBLE);
                mVideoViewArr[i].setLayoutParams(mGridParamArr[5]);
                mVideoViewArr[i].bringToFront();
                mFullScreenUserIdTag = userID;
                break;
            }
        }
        showAllText();
    }

    //移除放大效果
    private void cancelZoom() {
        for (int i = 0; i < mLiverArr.length; i++) {
            if (mLiverArr[i].equals(mFullScreenUserIdTag)) {
                mVideoViewArr[i].setLayoutParams(mGridParamArr[i]);
                break;
            }
        }
        mFullScreenUserIdTag = DEFAULT_USERID;
    }

    public void setBroadcastArr(String[] ids) {
        mBroadcastArr = ids;
    }

    // 设置布局画面
    public void setScreen() {
        try {
            List<Integer> list = new ArrayList();
            for (int i = 0; i < mBroadcastArr.length; i++) {
                // 每个账号跟已有的框体进行比对
                for (int j = 0; j < mLiverArr.length; j++) {
                    if (mLiverArr[j].equals(DEFAULT_USERID)) {
                        continue;
                    }
                    if (mBroadcastArr[i].equals(mLiverArr[j])) {
                        mVideoViewArr[j].setLayoutParams(mGridParamArr[i]);
                        list.add(i);
                        break;
                    }
                }
            }

            List<Integer> notUseList = new ArrayList();
            for (int i = 0; i < 5; i++) {
                notUseList.add(i);
            }

            notUseList.removeAll(list);

            int index = 0;
            for (int i = 0; i < mLiverArr.length; i++) {
                if (mLiverArr[i].equals(DEFAULT_USERID)) {
                    Integer j = notUseList.get(index);
                    index = index + 1;
                    mVideoViewArr[i].setLayoutParams(mGridParamArr[j]);
                }
            }

            showAllText();
        } catch (Exception e) {

        }
    }
}
