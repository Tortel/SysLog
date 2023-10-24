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
package com.tortel.syslog.dialog;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tortel.syslog.R;

/**
 * Class to easily show various informational dialogs
 */
public class AppDialogs {

    /**
     * Show a dialog with the provided title and body. Everything will be HTML formatted and linkified
     * @param context context
     * @param title the title resource ID
     * @param content the content resource ID
     */
    private static void showDialog(@NonNull Context context, @StringRes int title, @StringRes int content) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(context.getString(content), Html.FROM_HTML_MODE_LEGACY));
        } else {
            // No need for additional flag in fromHtml method if below Nougat
            builder.setMessage(Html.fromHtml(context.getString(content)));
        }
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Show the About dialog
     * @param context
     */
    public static void showAboutDialog(@NonNull Context context) {
        showDialog(context, R.string.about, R.string.dialog_about_content);
    }

    /**
     * Show the FAQ dialog
     * @param context
     */
    public static void showFaqDialog(@NonNull Context context) {
        showDialog(context, R.string.faq, R.string.dialog_faq_content);
    }

    /**
     * Show the About Live Logcat dialog
     * @param context
     */
    public static void showAboutLiveLogcatDialog(@NonNull Context context) {
        showDialog(context, R.string.about_live, R.string.dialog_livelog_content);
    }
}
