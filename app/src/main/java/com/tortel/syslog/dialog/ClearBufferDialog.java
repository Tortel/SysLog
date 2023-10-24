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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tortel.syslog.R;
import com.tortel.syslog.utils.Prefs;
import com.tortel.syslog.utils.Utils;

/**
 * Shows a dialog warning about clearing the logcat buffers
 */
public class ClearBufferDialog {

    private final MaterialAlertDialogBuilder builder;
    private CheckBox mCheckBox;

    public ClearBufferDialog(Activity activity) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_buffer, null);
        mCheckBox = view.findViewById(R.id.dont_show_again);
        // Show material you dialog
        builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle(activity.getString(R.string.clear_buffer));
        builder.setView(R.layout.dialog_buffer);
        builder.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCheckBox.isChecked()) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(Prefs.KEY_NO_BUFFER_WARN, true);
                    editor.apply();
                }
                // Run the task to clear the buffer
                Utils.clearLogcatBuffer(activity);
            }
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
    }

    public AlertDialog getDialog() {
        return builder.create();
    }
}
