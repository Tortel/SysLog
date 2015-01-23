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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.utils.Utils;
import com.tortel.syslog.exception.LowSpaceException;

/**
 * Dialog for low space
 */
public class LowSpaceDialog extends DialogFragment implements DialogInterface.OnClickListener {

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
    
    private static Result result;
    
    private Button stackTraceButton;
    private TextView stackTraceView;
    
    public void setResult(Result result){
        LowSpaceDialog.result = result;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        
        View view = inflator.inflate(R.layout.dialog_exception, null);
        stackTraceButton = (Button) view.findViewById(R.id.button_stacktrace);
        stackTraceButton.setVisibility(View.GONE);
        view.findViewById(R.id.bugreport_notice).setVisibility(View.GONE);

        stackTraceView = (TextView) view.findViewById(R.id.exception_stacktrace);
        stackTraceView.setVisibility(View.GONE);

        TextView messageText = (TextView) view.findViewById(R.id.exception_message);
        LowSpaceException e = (LowSpaceException) result.getException();
        
        messageText.setText(getActivity().getResources().getString(result.getMessage(),
                e.getFreeSpace()));
        
        builder.setView(view);

        
        builder.setPositiveButton(R.string.clean_uncompressed, this);
        builder.setNeutralButton(R.string.clean_all, this);
        
        builder.setTitle(R.string.error_dialog_title);
        
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which){
        case DialogInterface.BUTTON_POSITIVE:
            new Utils.CleanUncompressedTask(getActivity()).execute();
            result = null;
            this.dismiss();
            return;
        case DialogInterface.BUTTON_NEUTRAL:
            new Utils.CleanAllTask(getActivity()).execute();
            result = null;
            this.dismiss();
            return;
        case DialogInterface.BUTTON_NEGATIVE:
            //Clear the static variable
            result = null;
            this.dismiss();
            return;
        }
    }
}
