package com.tortel.syslog.utils;

/**
 * All the preference keys
 */
public class Prefs {
    /**
     * Kernel log pref
     */
    public static final String KEY_KERNEL = "kernel";
    /**
     * Logcat pref
     */
    public static final String KEY_MAIN = "main";
    /**
     * Event log pref
     */
    public static final String KEY_EVENT = "event";
    /**
     * Modem log pref
     */
    public static final String KEY_MODEM = "modem";
    /**
     * Audit log pref
     */
    public static final String KEY_AUDIT = "audit";
    /**
     * Last kmsg pref
     */
    public static final String KEY_LASTKMSG = "lastKmsg";
    /**
     * PStore pref
     */
    public static final String KEY_PSTORE = "pstore";
    /**
     * Scrub the logs pref
     */
    public static final String KEY_SCRUB = "scrub";
    /**
     * Hide logcat buffer warning pref
     */
    public static final String KEY_NO_BUFFER_WARN = "buffer";

    public static final String KEY_LIVE_LOGCAT_ABOUT = "about_live_logcat";

    private Prefs(){
        // Prevent the class from being created
    }
}
