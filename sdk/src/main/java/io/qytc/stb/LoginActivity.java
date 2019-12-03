package io.qytc.stb;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.tencent.imsdk.TIMCallBack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends Activity {

    private String mUserId;
    private int mRoomNo;
    private int mSdkAppId = 1400222844;
    private int mRole;

    private String mUserSig;

    private Dialog mInputPswDialog;
    private EditText etPsw_0, etPsw_1, etPsw_2, etPsw_3;
    private int pswIndex = 0;
    private Dialog mProgressDialog;
    private TextView mTvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);

        mTvTip = findViewById(R.id.tv_tip);

        Intent intent = getIntent();

        mUserId = intent.getStringExtra(ThirdLoginConstant.USERID);
        mRoomNo = intent.getIntExtra(ThirdLoginConstant.ROOMID, -1);
        int role = intent.getIntExtra(ThirdLoginConstant.ROLE, -1);
        mSdkAppId=intent.getIntExtra(ThirdLoginConstant.SDKAPPID,mSdkAppId);
        if (role == ThirdLoginConstant.Anchor) {
            mRole = VideoActivity.LIVER;
        } else {
            mRole = VideoActivity.WATCHER;
        }

        if (TextUtils.isEmpty(mUserId)) {
            ToastUtils.toast(this, R.string.userId_not_empty);
            mTvTip.setText(R.string.userId_not_empty);
            return;
        }

        if (mRoomNo < 0) {
            ToastUtils.toast(this, R.string.roomNo_invalid);
            mTvTip.setText(R.string.roomNo_invalid);
            return;
        }

        showProgressDialog();
        getUserSig();
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new Dialog(this, R.style.dialogstyle);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);
            mProgressDialog.setContentView(view);
        }
        mProgressDialog.show();
    }

    private void getUserSig() {
        Map<String, String> map = new HashMap<>();
        map.put("appId", String.valueOf(mSdkAppId));
        map.put("userId", mUserId);
        MyHttpsUtils.post(Urls.GENERATE_USER_SIG, map, this, new MyHttpsUtils.ApiBackCall() {
            @Override
            public void onFail() {
                mProgressDialog.dismiss();
                mTvTip.setText(R.string.get_sig_fail);
            }

            @Override
            public void onSuccess(String result) {
                if (MyHttpsUtils.isSuccessCode(result)) {
                    mUserSig = MyHttpsUtils.checkResponse(result, SigBean.class).getData();
                    joinRoom("");
                } else {
                    showErrorMsg(result);
                }
            }
        });
    }

    private void joinRoom(String roomPassword) {
        Map<String, String> map = new HashMap<>();
        map.put("acctno", mUserId);
        map.put("roomNo", String.valueOf(mRoomNo));
        map.put("password", roomPassword);
        map.put("role", mRole == VideoActivity.LIVER ? "2" : "3");
        MyHttpsUtils.post(Urls.JOIN_ROOM, map, this, new MyHttpsUtils.ApiBackCall() {
            @Override
            public void onFail() {
                mProgressDialog.dismiss();
                mTvTip.setText(R.string.join_room_fail);
            }

            @Override
            public void onSuccess(String result) {
                int code = MyHttpsUtils.getCode(result);
                if (code == 0) {
                    enterRoom();
                } else if (code == 2) {
                    showPasswordDialog();
                } else {
                    showErrorMsg(result);
                }
            }
        });
    }

    private void showErrorMsg(String result) {
        mProgressDialog.dismiss();
        String msg = MyHttpsUtils.getMsg(result);
        mTvTip.setText(msg);
        ToastUtils.toast(LoginActivity.this, msg);
    }

    //加入视频房间
    private void enterRoom() {
        Intent intent = new Intent(LoginActivity.this, VideoActivity.class);
        intent.putExtra(ThirdLoginConstant.SDKAPPID, mSdkAppId);
        intent.putExtra(ThirdLoginConstant.USERID, mUserId);
        intent.putExtra(ThirdLoginConstant.USERSIG, mUserSig);
        intent.putExtra(ThirdLoginConstant.ROOMID, Integer.valueOf(mRoomNo));
        intent.putExtra(ThirdLoginConstant.ROLE, mRole);

        startActivity(intent);
        finish();

//        TrtcManager trtcManager = TrtcManager.getInstance();
//        trtcManager.init(this, mSdkAppId);
//
//        trtcManager.login(mUserId, mUserSig, new TIMCallBack() {
//            @Override
//            public void onError(int i, String s) {
//                showErrorMsg("登陆失败");
//            }
//
//            @Override
//            public void onSuccess() {
//                mProgressDialog.dismiss();
//                Intent intent = new Intent(LoginActivity.this, VideoActivity.class);
//                intent.putExtra(ThirdLoginConstant.SDKAPPID, mSdkAppId);
//                intent.putExtra(ThirdLoginConstant.USERID, mUserId);
//                intent.putExtra(ThirdLoginConstant.USERSIG, mUserSig);
//                intent.putExtra(ThirdLoginConstant.ROOMID, Integer.valueOf(mRoomNo));
//                intent.putExtra(ThirdLoginConstant.ROLE, mRole);
//
//                startActivity(intent);
//                finish();
//            }
//        });
    }

    //弹密码输入框
    private void showPasswordDialog() {
        if (mInputPswDialog == null) {
            mInputPswDialog = new Dialog(this, R.style.dialogstyle);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_input_psw, null);
            mInputPswDialog.setContentView(view);
            mInputPswDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            mInputPswDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });

            etPsw_0 = view.findViewById(R.id.et_psw_0);
            etPsw_1 = view.findViewById(R.id.et_psw_1);
            etPsw_2 = view.findViewById(R.id.et_psw_2);
            etPsw_3 = view.findViewById(R.id.et_psw_3);

            etPsw_0.setOnFocusChangeListener(focusChangeListener);
            etPsw_1.setOnFocusChangeListener(focusChangeListener);
            etPsw_2.setOnFocusChangeListener(focusChangeListener);
            etPsw_3.setOnFocusChangeListener(focusChangeListener);

            //停止弹出然键盘
            mInputPswDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            try {
                Class<EditText> cls = EditText.class;
                Method setShowSoftInputOnFocus = cls.getMethod("setShowSoftInputOnFocus", boolean.class);
                setShowSoftInputOnFocus.setAccessible(true);
                setShowSoftInputOnFocus.invoke(etPsw_0, false);
                setShowSoftInputOnFocus.invoke(etPsw_1, false);
                setShowSoftInputOnFocus.invoke(etPsw_2, false);
                setShowSoftInputOnFocus.invoke(etPsw_3, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mInputPswDialog.show();

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                switch (pswIndex) {
                    case 0:
                        if (etPsw_0.getText().toString().length() > 0) {
                            etPsw_1.requestFocus();
                            pswIndex = 1;
                        }
                        break;
                    case 1:
                        if (etPsw_1.getText().toString().length() > 0) {
                            etPsw_2.requestFocus();
                            pswIndex = 2;
                        }
                        break;
                    case 2:
                        if (etPsw_2.getText().toString().length() > 0) {
                            etPsw_3.requestFocus();
                            pswIndex = 3;
                        }
                        break;
                }
                String num_0 = etPsw_0.getText().toString();
                String num_1 = etPsw_1.getText().toString();
                String num_2 = etPsw_2.getText().toString();
                String num_3 = etPsw_3.getText().toString();
                if (!TextUtils.isEmpty(num_0) && !TextUtils.isEmpty(num_1) && !TextUtils.isEmpty(num_2) && !TextUtils.isEmpty(num_3)) {
                    mInputPswDialog.dismiss();
                    String roomPassword = num_0 + num_1 + num_2 + num_3;
                    joinRoom(roomPassword);
                }
            }
        };
        etPsw_0.addTextChangedListener(textWatcher);
        etPsw_1.addTextChangedListener(textWatcher);
        etPsw_2.addTextChangedListener(textWatcher);
        etPsw_3.addTextChangedListener(textWatcher);
        etPsw_0.setText("");
        etPsw_1.setText("");
        etPsw_2.setText("");
        etPsw_3.setText("");

        etPsw_0.requestFocus();
        pswIndex = 0;
    }

    private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (b) {
                int id = view.getId();
                if (id == R.id.et_psw_0) {
                    pswIndex = 0;
                    etPsw_0.setText("");
                } else if (id == R.id.et_psw_1) {
                    pswIndex = 1;
                    etPsw_1.setText("");
                } else if (id == R.id.et_psw_2) {
                    pswIndex = 2;
                    etPsw_2.setText("");
                } else if (id == R.id.et_psw_3) {
                    pswIndex = 3;
                    etPsw_3.setText("");
                }
            }
        }
    };
}
