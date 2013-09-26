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
package com.tortel.syslog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Shows a simple FAQ dialog
 */
public class FaqDialog extends SherlockDialogFragment {
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.about, null);
        TextView text = (TextView) view.findViewById(R.id.text);
        
        text.setText(Html.fromHtml(readRawTextFile(R.raw.faq)));
        Linkify.addLinks(text, Linkify.ALL);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        
        builder.setView(view);
        builder.setTitle(R.string.syslog_faq);
        builder.setPositiveButton(R.string.close, null);
        
        return builder.create();
    }
    
    private String readRawTextFile(int id) {
        InputStream inputStream = getActivity().getResources().openRawResource(id);
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);
        String line;
        StringBuilder text = new StringBuilder();
        try {
            while ((line = buf.readLine()) != null)
                text.append(line);
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
