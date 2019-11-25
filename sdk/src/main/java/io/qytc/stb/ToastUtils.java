package io.qytc.stb;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param resId
     */
    public static void toast(Context context, int resId) {
        Toast.makeText(context, context.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
