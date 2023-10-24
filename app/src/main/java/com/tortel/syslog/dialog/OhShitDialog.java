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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tortel.syslog.R;

/**
 * Well, shit dialog. Called if the send email intent fails.
 */
public class OhShitDialog {
    private static Throwable exception;
    private final MaterialAlertDialogBuilder builder;

    public OhShitDialog(Activity activity) {
        builder = new MaterialAlertDialogBuilder(activity);
        builder.setMessage(R.string.oh_shit_message);
        builder.setTitle(R.string.oh_shit_title);
        builder.setNegativeButton(activity.getString(R.string.close), (dialog, which) -> {
            throw new RuntimeException(exception);
        });
    }

    public void setException(Throwable exception){
        OhShitDialog.exception = exception;
    }

    public AlertDialog getDialog() {
        return builder.create();
    }
}
