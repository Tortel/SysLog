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
package com.tortel.syslog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * A dialog used for setting a custom root path
 */
public class CustomPathDialog extends DialogFragment implements DialogInterface.OnClickListener{
    
    private EditText pathEditText;
    private SharedPreferences prefs;
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        
        View view = inflator.inflate(R.layout.dialog_path, null);
        
        pathEditText = (EditText) view.findViewById(R.id.edit_path);
        
        pathEditText.setText(prefs.getString(Utils.PREF_PATH, Utils.ROOT_PATH));
        
        builder.setView(view);

        
        builder.setPositiveButton(R.string.save, this);
        builder.setNeutralButton(R.string.reset, this);
        builder.setNegativeButton(R.string.cancel, this);
        
        builder.setTitle(R.string.change_path);
        
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        SharedPreferences.Editor editor = prefs.edit();
        
        switch(which){
        case DialogInterface.BUTTON_POSITIVE:
            // Save it
            String path = pathEditText.getText().toString().trim();
            editor.putString(Utils.PREF_PATH, path);
            editor.apply();
            this.dismiss();
            return;
        case DialogInterface.BUTTON_NEUTRAL:
            // Reset
            editor.remove(Utils.PREF_PATH);
            editor.apply();
            this.dismiss();
            return;
        case DialogInterface.BUTTON_NEGATIVE:
            this.dismiss();
            return;
        }
    }

}
