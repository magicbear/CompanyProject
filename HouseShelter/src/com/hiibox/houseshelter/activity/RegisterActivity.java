package com.hiibox.houseshelter.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tsz.afinal.annotation.view.ViewInject;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hiibox.houseshelter.ShaerlocActivity;
import com.hiibox.houseshelter.MyApplication;
import com.hiibox.houseshelter.R;
import com.hiibox.houseshelter.core.MianActivity;
import com.hiibox.houseshelter.core.GlobalUtil;
import com.hiibox.houseshelter.util.ImageOperation;
import com.hiibox.houseshelter.util.LocationUtil;
import com.hiibox.houseshelter.util.MessageUtil;
import com.hiibox.houseshelter.util.PreferenceUtil;
import com.hiibox.houseshelter.util.StringUtil;

    
  
  
  
  
  
  
  
public class RegisterActivity extends ShaerlocActivity {

    @ViewInject(id = R.id.root_layout) LinearLayout rootLayout;
    @ViewInject(id = R.id.two_dimension_code_iv, click = "onClick") ImageView codeIV;
    @ViewInject(id = R.id.cancel_register_iv, click = "onClick") ImageView cancelIV;
    @ViewInject(id = R.id.commit_iv, click = "onClick") ImageView commitIV;
    @ViewInject(id = R.id.device_code_et) EditText deviceCodeET;
    @ViewInject(id = R.id.phone_number_et) EditText phoneET;
    @ViewInject(id = R.id.password_et) EditText passwordET;
    @ViewInject(id = R.id.nickname_et) EditText nicknameET;
    @ViewInject(id = R.id.confirm_password_et) EditText confirmPasswordET;
    @ViewInject(id = R.id.address_et) EditText addressET;
    @ViewInject(id = R.id.auth_code_et) EditText authCodeET;
    
    private String deviceCode = null;
    private String authCode = null;
    private String phone = null;
    private String newPsw = null;
    private ProgressDialog loginDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_layout);
    }
    
    public void onClick(View v) {
        if (v == codeIV) {
            startActivityForResult(new Intent(this, ScanCodeActivity.class), 0x101);
        } else if (v == cancelIV) {
            MianActivity.getScreenManager().exitActivity(mActivity);
        } else if (v == commitIV) {
            phone = phoneET.getText().toString().trim();
            if (StringUtil.isEmpty(phone)) {
                MessageUtil.alertMessage(mContext, R.string.hint_input_phone_number);
                return;
            }
            if (!isMobileNum(phone)) {
                MessageUtil.alertMessage(mContext, R.string.prompt_input_correct_phone);
                return;
            }
            newPsw = passwordET.getText().toString();
            if (StringUtil.isEmpty(newPsw)) {
                setFocusable(passwordET, R.string.hint_input_password);
                return;
            }
            String confirmNewPsw = confirmPasswordET.getText().toString();
            if (StringUtil.isEmpty(confirmNewPsw)) {
                setFocusable(confirmPasswordET, R.string.hint_input_confirm_password);
                return;
            }
            if (!newPsw.equals(confirmNewPsw)) {
                setFocusable(passwordET, R.string.prompt_input_diffrient_password);
                MessageUtil.alertMessage(mContext, R.string.prompt_input_diffrient_password);
                confirmPasswordET.setText("");
                return;
            }
                                                                                       
            String nickname = nicknameET.getText().toString().trim();
            if (StringUtil.isEmpty(nickname)) {
                setFocusable(nicknameET, R.string.hint_input_nickname);
                return;
            }
                                                                                     
            String address = addressET.getText().toString().trim();
            if (StringUtil.isEmpty(address)) {
                setFocusable(addressET, R.string.hint_input_address);
                return;
            }
            deviceCode = deviceCodeET.getText().toString().trim();
            if (StringUtil.isEmpty(deviceCode)) {
                setFocusable(deviceCodeET, R.string.hint_input_device_code);
                return;
            }
            authCode = authCodeET.getText().toString().trim();
            if (StringUtil.isEmpty(authCode)) {
                setFocusable(authCodeET, R.string.hint_input_auth_code);
                return;
            }
            try {
                MyApplication.tcpManager.register(phone, newPsw, deviceCode, authCode, address, nickname, handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("unused")
    private String transformCode(String src) {
        String des = null;
        try {
            des = URLEncoder.encode(src, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return des;
    }
    
    @SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what == 0) {
            	PreferenceUtil.getInstance(mContext).saveString("deviceCode", deviceCode);
            	PreferenceUtil.getInstance(mContext).saveString("authCode", authCode);
            	PreferenceUtil.getInstance(mContext).saveString("phone", phone);
                PreferenceUtil.getInstance(mContext).saveString("password", newPsw);
                MessageUtil.alertMessage(mContext, R.string.register_success);
                autoLogin();
                                                                              
            } else if (what == 1) {
                String reason = (String) msg.obj;
                MessageUtil.alertMessage(mContext, getString(R.string.register_failed)+reason);
            } else if (what == 2) {
                MessageUtil.alertMessage(mContext, R.string.network_connect_failed);
            } else if (what == 3) {
                MessageUtil.alertMessage(mContext, R.string.network_timeout);
            }
        }
    };
    
    private void autoLogin() {
        if (StringUtil.isNotEmpty(phone) && StringUtil.isNotEmpty(newPsw)) {
            loginDialog = new ProgressDialog(this);
            loginDialog.setCancelable(true);
            loginDialog.setCanceledOnTouchOutside(false);
            loginDialog.setMessage(getString(R.string.logining));
            loginDialog.show();
            try {
                String imei = LocationUtil.getDrivenToken(mContext);
                MyApplication.tcpManager.login1(phone, newPsw, imei, loginHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Handler loginHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 2) {
                loginDialog.setMessage(getString(R.string.user_auth));
                MyApplication.phone = phone;
                MyApplication.password = newPsw;
                MyApplication.initTcpManager();
                MyApplication.mainClient = MyApplication.tcpManager.getMainClient(phone, newPsw, "1111111111111111", "66666666");
                MyApplication.fileClient = MyApplication.tcpManager.getFileClient(phone);
                if (null != loginDialog && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                startActivity(new Intent(RegisterActivity.this, HomepageActivity.class));
                MianActivity.getScreenManager().exitAllActivityExceptOne();
            } else if (msg.what == 4) {
                if (null != loginDialog && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                MessageUtil.alertMessage(mContext, R.string.network_error);
            } else if (msg.what == 5) {
                if (null != loginDialog && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                MessageUtil.alertMessage(mContext, R.string.network_not_response);
            } else if (msg.what == 6) {
                if (null != loginDialog && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                MessageUtil.alertMessage(mContext, R.string.network_timeout);
            }  
        }
    };

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x101 && resultCode == RESULT_OK) {
            deviceCodeET.setText(data.getStringExtra("code"));
        }
    }
    
    public void setFocusable(EditText editText, int msg) {
        MessageUtil.alertMessage(mContext, msg);
        editText.setText("");
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
        editText.requestFocus();
        Editable editable = editText.getText();
        Selection.setSelection(editable, 0);
    }
    
    private boolean isMobileNum(String mobiles) {
        Pattern p = Pattern
                .compile("((13[0-9])|(15[^4,\\D])|(18[0,1,3,5-9]))\\d{8}");
        Matcher m = p.matcher(mobiles);
        return m.matches();
    }
    
	@Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(
            ImageOperation.readBitMap(mContext, R.drawable.bg_app));
        rootLayout.setBackgroundDrawable(bitmapDrawable);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        rootLayout.setBackgroundDrawable(null);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(null);
        loginHandler.removeCallbacks(null);
    }
    
}
