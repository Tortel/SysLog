/* SysLog - A simple logging tool
 * Copyright (C) 2013-2023  Scott Warner <Tortel1210@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
