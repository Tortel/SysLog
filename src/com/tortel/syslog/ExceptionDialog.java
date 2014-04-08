/* SysLog - A simple logging tool
 * Copyright (C) 2013  Scott Warner <Tortel1210@gmail.com>
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

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class ExceptionDialog extends SherlockDialogFragment implements android.view.View.OnClickListener,
        DialogInterface.OnClickListener {
    
    private static Result result;
    
    private Button stackTraceButton;
    private TextView stackTraceView;
    
    public void setResult(Result result){
        ExceptionDialog.result = result;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        
        View view = inflator.inflate(R.layout.dialog_exception, null);
        stackTraceButton = (Button) view.findViewById(R.id.button_stacktrace);
        stackTraceButton.setOnClickListener(this);
        if(result.getException() == null){
            stackTraceButton.setVisibility(View.GONE);
            view.findViewById(R.id.bugreport_notice).setVisibility(View.GONE);
        }

        stackTraceView = (TextView) view.findViewById(R.id.exception_stacktrace);
        stackTraceView.setText(getStackTrace(result.getException()));

        TextView messageText = (TextView) view.findViewById(R.id.exception_message);
        messageText.setText(result.getMessage());
        builder.setView(view);
        //Skip the bugreport button if there is no stack trace
        if(result.getException() != null){
            builder.setPositiveButton(R.string.send_bugreport, this);
        }
        
        builder.setNegativeButton(R.string.dismiss, this);
        builder.setTitle(R.string.error_dialog_title);
        
        return builder.create();
    }

    @Override
    public void onClick(View v) {
        //Hide the stacktrace button, show the stacktrace
        stackTraceButton.setVisibility(View.GONE);
        stackTraceView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which){
        case DialogInterface.BUTTON_POSITIVE:
            Intent intent = getEmailIntent();
            if(intent != null){
                getActivity().startActivity(intent);
            }
        case DialogInterface.BUTTON_NEGATIVE:
            //Clear the static variable
            result = null;
        }
    }
    
    private Intent getEmailIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"Swarner.dev@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "SysLog bug report");
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_TEXT, getEmailReportBody());
        if(!MainActivity.isAvailable(getActivity(), intent)){
            OhShitDialog dialog = new OhShitDialog();
            dialog.setException(result.getException());
            dialog.show(getActivity().getSupportFragmentManager(), "ohshit");
            this.dismiss();
            return null;
        }
        return intent;
    }
    
    private String getEmailReportBody(){
        Context context = getActivity();
        StringBuilder body = new StringBuilder();
        
        body.append("Device model: "+android.os.Build.MODEL+"\n");
        try{
            PackageInfo manager = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            body.append("SysLog version: "+manager.versionCode+"\n");
        } catch(Exception e){
            //Should not happen
        }
        body.append("Android version: "+android.os.Build.VERSION.SDK_INT+"\n");
        body.append("Kernel version: "+System.getProperty("os.version")+"\n");
        body.append("Storage state: "+Environment.getExternalStorageState()+"\n");
        body.append("Free space: "+Utils.getStorageFreeSpace()+"mb \n");
        body.append("Storage path: "+Environment.getExternalStorageDirectory().getPath()+"\n");
        body.append("Using root: "+result.getCommand().hasRoot()+"\n");
        body.append(result.getCommand().getDebugString()+"\n");
        body.append("Stacktrace:\n");
        body.append(getStackTrace(result.getException()));
        body.append("\n -- Please leave all the information above --\n");
        body.append("Please add any additional details:\n");
        return body.toString();
    }
    
    private String getStackTrace(Throwable t){
        if(t == null){
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        
        return sw.toString();
    }
}
