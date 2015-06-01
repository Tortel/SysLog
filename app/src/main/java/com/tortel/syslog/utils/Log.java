package com.tortel.syslog.utils;

/**
 * Log wrapper
 */
public class Log {
    private static final String TAG = "SysLog";

    public static void v(String msg){
        android.util.Log.v(TAG, msg);
    }

    public static void d(String msg){
        android.util.Log.d(TAG, msg);
    }

    public static void e(String msg, Throwable th){
        android.util.Log.e(TAG, msg, th);
    }
}
