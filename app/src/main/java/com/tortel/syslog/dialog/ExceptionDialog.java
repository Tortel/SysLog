/* SysLog - A simple logging tool
 * Copyright (C) 2013-2016  Scott Warner <Tortel1210@gmail.com>
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.utils.Utils;

/**
 * Dialog for general exceptions
 */
public class ExceptionDialog extends DialogFragment implements android.view.View.OnClickListener {
    
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
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        
        View view = inflator.inflate(R.layout.dialog_exception, null);
        stackTraceButton = (Button) view.findViewById(R.id.button_stacktrace);
        stackTraceButton.setOnClickListener(this);
        TextView reportNotice = (TextView) view.findViewById(R.id.bugreport_notice);
        if(result.getException() == null){
            stackTraceButton.setVisibility(View.GONE);
            reportNotice.setVisibility(View.GONE);
        }

        stackTraceView = (TextView) view.findViewById(R.id.exception_stacktrace);
        stackTraceView.setText(getStackTrace(result.getException()));

        TextView messageText = (TextView) view.findViewById(R.id.exception_message);
        int messageId = result.getMessage();
        if(messageId == 0){
            messageId = R.string.error;
        }
        messageText.setText(messageId);
        builder.customView(view, false);
        //Skip the bugreport button if there is no stack trace or if 4.3+ without root
        if(result.getException() != null){
            if(result.disableReporting()){
                reportNotice.setText(R.string.bugreport_disabled);
            } else {
                builder.positiveText(R.string.send_bugreport);
            }
        }

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Intent intent = getEmailIntent();
                if(intent != null){
                    getActivity().startActivity(intent);
                }
                dismiss();
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                result = null;
                dismiss();
            }
        });
        
        builder.negativeText(R.string.dismiss);
        builder.title(R.string.error_dialog_title);
        
        return builder.build();
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
}
