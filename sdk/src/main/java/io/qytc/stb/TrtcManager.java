package io.qytc.stb;

import android.content.Context;

import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMGroupManager;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMSdkConfig;
import com.tencent.teduboard.TEduBoardController;
import com.tencent.trtc.TRTCCloudDef;

public class TrtcManager {
    private static final String TAG = "TrtcManager";
    private static TrtcManager mManager;
    //Board
    private TEduBoardController mBoard;
    private TEduBoardController.TEduBoardCallback mBoardCallback;
    private String mRoomNO;

    public static TrtcManager getInstance() {
        if (mManager == null) {
            mManager = new TrtcManager();
        }
        return mManager;
    }

    public TEduBoardController getBoard() {
        return mBoard;
    }

    public void init(Context context, int appId) {
        // TIM SDK初始化
        TIMManager.getInstance().init(context, new TIMSdkConfig(appId));

        // TEdu Board
        if (mBoard == null) {
            mBoard = new TEduBoardController(context);
        }
    }

    public void login(String userId, String userSig, final TIMCallBack timCallBack) {

        TIMManager.getInstance().login(userId, userSig, timCallBack);
    }

    public void joinClassRoom(TRTCCloudDef.TRTCParams params, TEduBoardController.TEduBoardCallback boardCallback) {
        mRoomNO = params.roomId + "1";

        TEduBoardController.TEduBoardAuthParam authParam = new TEduBoardController.TEduBoardAuthParam(params.sdkAppId, params.userId, params.userSig);
        mBoard.init(authParam, Integer.valueOf(mRoomNO), null);

        mBoardCallback = boardCallback;
        if (mBoard != null && mBoardCallback != null) {
            mBoard.addCallback(boardCallback);
        }

        TIMGroupManager.getInstance().applyJoinGroup(mRoomNO, "board group" + mRoomNO, null);
    }

    public void quitClassRoom() {
        if (mBoardCallback != null) {
            mBoard.removeCallback(mBoardCallback);
        }

        TIMGroupManager.getInstance().quitGroup(mRoomNO, null);
    }
}