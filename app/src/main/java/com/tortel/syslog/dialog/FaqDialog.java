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

    public FaqDialog(Activity activity) {
        // Show material you dialog
        builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle(activity.getString(R.string.faq));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(activity.getString(R.string.dialog_faq_content), Html.FROM_HTML_MODE_LEGACY));
        } else {
            // No need for additional flag in fromHtml method if below Nougat
            builder.setMessage(Html.fromHtml(activity.getString(R.string.dialog_faq_content)));
        }
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
    }

    public AlertDialog getDialog() {
        return builder.create();
    }
}
