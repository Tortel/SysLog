/* SysLog - A simple logging tool
 * Copyright (C) 2013-2014  Scott Warner <Tortel1210@gmail.com>
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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.syslog.R;
import com.tortel.syslog.utils.Utils;

/**
 * A dialog used for setting a custom root path
 */
public class CustomPathDialog extends DialogFragment {
    
    private EditText pathEditText;
    private SharedPreferences prefs;
    private String path;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        path = pathEditText.getText().toString();
        super.onDestroyView();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        
        View view = inflator.inflate(R.layout.dialog_path, null);
        
        pathEditText = (EditText) view.findViewById(R.id.edit_path);
        
        pathEditText.setText(path != null ? prefs.getString(Utils.PREF_PATH,Utils.ROOT_PATH) : path);
        
        builder.customView(view, false);

        
        builder.positiveText(R.string.save);
        builder.neutralText(R.string.reset);
        builder.negativeText(R.string.cancel);
        
        builder.title(R.string.change_path);

        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                // Save it
                SharedPreferences.Editor editor = prefs.edit();
                String path = pathEditText.getText().toString().trim();
                editor.putString(Utils.PREF_PATH, path);
                editor.apply();
                dismiss();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                dismiss();
            }

            @Override
            public void onNeutral(MaterialDialog dialog) {
                // Reset
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(Utils.PREF_PATH);
                editor.apply();
                dismiss();
            }
        });
        
        return builder.build();
    }

}
