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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.utils.FileUtils;
import com.tortel.syslog.exception.LowSpaceException;

/**
 * Dialog for low space
 */
public class LowSpaceDialog {

    private final MaterialAlertDialogBuilder builder;
    private static Result result;

    public LowSpaceDialog(Activity activity) {
        builder = new MaterialAlertDialogBuilder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_exception, null);
        Button stackTraceButton = view.findViewById(R.id.button_stacktrace);
        stackTraceButton.setVisibility(View.GONE);
        view.findViewById(R.id.bugreport_notice).setVisibility(View.GONE);

        TextView stackTraceView = view.findViewById(R.id.exception_stacktrace);
        stackTraceView.setVisibility(View.GONE);

        TextView messageText = view.findViewById(R.id.exception_message);
        LowSpaceException e = (LowSpaceException) result.getException();

        messageText.setText(activity.getResources().getString(R.string.exception_space,
                e.getFreeSpace()));

        builder.setView(view);

        builder.setPositiveButton(R.string.clean_all, (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE -> {
                    FileUtils.cleanAllLogs(activity);
                    result = null;
                    dialog.dismiss();
                }
                case DialogInterface.BUTTON_NEGATIVE -> {
                    //Clear the static variable
                    result = null;
                    dialog.dismiss();
                }
            }
        });

        builder.setTitle(R.string.error_dialog_title);
    }
    public void setResult(Result result){
        LowSpaceDialog.result = result;
    }
    public AlertDialog getDialog() {
        return builder.create();
    }
}
