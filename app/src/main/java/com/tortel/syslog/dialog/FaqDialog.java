/* SysLog - A simple logging tool
 * Copyright (C) 2013-2020  Scott Warner <Tortel1210@gmail.com>
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
package com.tortel.syslog.dialog;

import android.app.Activity;
import android.os.Build;
import android.text.Html;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tortel.syslog.R;

/**
 * Shows a simple FAQ dialog
 */
public class FaqDialog {

    private final MaterialAlertDialogBuilder builder;
    private final static String htmlStr =
            "<html>" +
                    "<body>" +
                    "    <h4>Why does SysLog request root access?</h4>" +
                    "    <p>" +
                    "    Starting with Android 4.1, the system logs were restricted to system applications only.<br />" +
                    "    You can <a href=\"https://github.com/Tortel/SysLog/blob/master/Readme.md#enabling-log-access-via-adb-no-root-required\">manually enable access</a> to some logs using adb though." +
                    "    </p>" +
                    "    <h4>Where are the files saved?</h4>" +
                    "    <p>" +
                    "        The files are saved in the application's cache, so no other apps can access them. You can attach them in other applications through the system document picker -" +
                    "        select the SysLog application and you can pick a compressed log file." +
                    "    </p>" +
                    "    <h4>Why is the last kernel log grayed out?</h4>" +
                    "    <p>" +
                    "    Your device's kernel needs to support the last log feature, and SysLog checks for the '/proc/last_kmsg' file at start." +
                    "    Even if your kernel supports the feature, it may only save the log during a kernel crash. Check for more details with" +
                    "    the maintainer of the kernel/ROM you use." +
                    "    </p>" +
                    "    <h4>What is Grep?</h4>" +
                    "    <p>" +
                    "    Grep is a Unix utility to match lines based on regular expressions. If you are not familiar with it, I would not" +
                    "    recommend using it. For more details, check the" +
                    "    <a href=\"http://www.gnu.org/software/grep/manual/grep.html\">help page</a>." +
                    "    </p>" +
                    "    <h4>What information gets scrubbed?</h4>" +
                    "    <p>" +
                    "        The scrubber uses a set of regular expressions to scrub out email addresses, phone numbers, URLs, IP addresses," +
                    "        serial number, and some basic phone and user information. For more details, you can view" +
                    "        <a href=\"https://github.com/Tortel/SysLog/blob/master/app/src/main/java/com/tortel/syslog/utils/ScrubberUtils.java\">the source</a>." +
                    "    </p>" +
                    "</body>" +
                    "</html>";

    public FaqDialog(Activity activity) {
        // Show material you dialog
        builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle(activity.getString(R.string.faq));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(htmlStr, Html.FROM_HTML_MODE_LEGACY));
        } else {
            // No need for additional flag in fromHtml method if below Nougat
            builder.setMessage(Html.fromHtml(htmlStr));
        }
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
    }

    public AlertDialog getDialog() {
        return builder.create();
    }
}
