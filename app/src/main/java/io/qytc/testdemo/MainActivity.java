package io.qytc.testdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import io.qytc.stb.LoginActivity;
import io.qytc.stb.ThirdLoginConstant;


public class MainActivity extends Activity implements View.OnClickListener {

    private EditText mEtUserId;
    private EditText mEtRoomNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);


        mEtUserId = findViewById(R.id.et_userId);
        mEtRoomNo = findViewById(R.id.et_roomNo);

        findViewById(R.id.btn_join_room).setOnClickListener(this);

        mEtUserId.setText("11111");
        mEtUserId.setSelection(5);

        mEtRoomNo.setText("444444");
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_join_room) {
            enterRoom();
        }
    }

    private void enterRoom() {
        String userId = mEtUserId.getText().toString();
        String roomNo = mEtRoomNo.getText().toString();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(ThirdLoginConstant.ROOMID, Integer.valueOf(roomNo));
        intent.putExtra(ThirdLoginConstant.USERID, userId);
        startActivity(intent);
    }
}
