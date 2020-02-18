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

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.exception.CreateFolderException;
import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.exception.RunCommandException;
import com.tortel.syslog.utils.Utils;

/**
 * Dialog for general exceptions
 */
public class ExceptionDialog extends DialogFragment implements android.view.View.OnClickListener, DialogInterface.OnClickListener {
    
    private static Result result;
    
    private Button stackTraceButton;
    private TextView stackTraceView;
    
    public void setResult(Result result){
        ExceptionDialog.result = result;
    }

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
        
        View view = inflater.inflate(R.layout.dialog_exception, null);
        stackTraceButton = view.findViewById(R.id.button_stacktrace);
        stackTraceButton.setOnClickListener(this);
        TextView reportNotice = view.findViewById(R.id.bugreport_notice);
        if(result.getException() == null){
            stackTraceButton.setVisibility(View.GONE);
            reportNotice.setVisibility(View.GONE);
        }

        stackTraceView = view.findViewById(R.id.exception_stacktrace);
        stackTraceView.setText(getStackTrace(result.getException()));

        TextView messageText = view.findViewById(R.id.exception_message);
        @StringRes int messageId = result.getMessage();
        if(messageId == 0){
            messageId = R.string.error;
            if (result.getException() != null) {
                Throwable e = result.getException();
                if (e instanceof CreateFolderException) {
                    messageId = R.string.exception_folder;
                } else if (e instanceof LowSpaceException) {
                    messageId = R.string.exception_space;
                } else if (e instanceof NoFilesException) {
                    messageId = R.string.exception_zip_nofiles;
                } else if (e instanceof RunCommandException) {
                    messageId = R.string.exception_commands;
                }
            }
        }
        messageText.setText(messageId);
        builder.setView(view);
        //Skip the bugreport button if there is no stack trace or if 4.3+ without root
        if(result.getException() != null){
            if(result.disableReporting()){
                reportNotice.setText(R.string.bugreport_disabled);
            } else {
                builder.setPositiveButton(R.string.send_bugreport, this);
            }
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
    
    private Intent getEmailIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"Swarner.dev@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "SysLog bug report");
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_TEXT, getEmailReportBody());
        if(!Utils.isHandlerAvailable(getActivity(), intent)){
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Intent intent = getEmailIntent();
            if(intent != null){
                getActivity().startActivity(intent);
            }
        } else {
            result = null;
        }
        dismiss();
    }
}
