/*
 * Copyright (C) 2013 Scott Warner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tortel.syslog.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.tortel.syslog.R;
import com.tortel.syslog.utils.Prefs;
import com.tortel.syslog.utils.Utils;

/**
 * Shows a dialog warning about clearing the logcat buffers
 */
public class ClearBufferDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private CheckBox mCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_buffer, null);
        mCheckBox = view.findViewById(R.id.dont_show_again);

        builder.setView(view);
        builder.setTitle(R.string.about);

        builder.setPositiveButton(R.string.yes, this);
        builder.setNegativeButton(R.string.no, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // If they don't want to see the dialog again, save the pref
            if (mCheckBox.isChecked()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Prefs.KEY_NO_BUFFER_WARN, true);
                editor.apply();
            }
            // Run the task to clear the buffer
            new Utils.ClearLogcatBufferTask(getContext()).execute();
        }
    }
}
