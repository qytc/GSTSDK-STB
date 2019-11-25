package io.qytc.stb;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyHttpsUtils {
    private static final String TAG = "MyHttpsUtils";
    private static final OkHttpClient mOkHttpClient = new OkHttpClient();
    private static Map<String, String> mMap = new HashMap<>();

    public interface ApiBackCall {
        void onFail();

        void onSuccess(String result);
    }

    public static void post(final String url, Map<String, String> params, final Activity activity, final ApiBackCall listener) {
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json;charset=utf-8"), new JSONObject(params).toString());
        final Request request = new Request.Builder()
                .url(Urls.BASE_URL + url)
                .post(requestBody)
                .build();

        try {
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, url + ":onFaiure," + e.toString());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onFail();
                            }
                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    mMap.put(url, response.body().string());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (listener != null) {
                                    Log.i(TAG, "result=" + mMap.get(url));
                                    listener.onSuccess(mMap.get(url));
                                }
                                mMap.remove(url);
                            } catch (Exception e) {
                                Log.e(TAG, url + ":onResponse," + e.toString());
                            } finally {
                                mMap.remove(url);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(url, "onResponse,Catch Exception:" + e.toString());
        }
    }

    public static int getCode(String result) {
        int code = -1;
        if (result == null || result.trim().equals("")) {
            Log.e(TAG, "result is null or blank chars");
            return code;
        } else {
            if (result.contains("code")) {
                try {
                    code = JSON.parseObject(result).getIntValue("code");
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                return code;
            } else {
                Log.e(TAG, "result not contain 'code'");
                return code;
            }
        }
    }

    public static boolean isSuccessCode(String result) {
        if (getCode(result) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static String getMsg(String result) {
        if (result == null || result.trim().equals("")) {
            Log.e(TAG, "result is null or blank chars");
            return "";
        } else {
            if (result.contains("msg")) {
                String msg = "";
                try {
                    msg = JSON.parseObject(result).getString("msg");
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                return msg;
            } else {
                Log.e(TAG, "result not contain 'msg'");
                return "";
            }
        }
    }

    public static <T> T checkResponse(String result, Class<T> clazz) {
        if (result != null && !"".equals(result.trim())) {
            try {
                return JSON.parseObject(result, clazz);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return null;
            }
        } else {
            Log.e(TAG, "result is null or '' ");
            return null;
        }
    }
}
