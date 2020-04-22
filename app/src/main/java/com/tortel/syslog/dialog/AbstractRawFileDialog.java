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

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.tortel.syslog.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Dialog that reads its content from a raw resource
 */
public abstract class AbstractRawFileDialog extends DialogFragment {

    /**
     * Return the resource ID to read for populating the dialog
     */
    abstract @RawRes int getContentRes();

    /**
     * Get the title resource for the dialog
     */
    abstract @StringRes int getTitleRes();

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
        View view = inflater.inflate(R.layout.dialog_rawfile, null);
        TextView text = view.findViewById(R.id.text);

        text.setText(Html.fromHtml(readRawTextFile(getContentRes())));
        Linkify.addLinks(text, Linkify.ALL);
        text.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(view);
        builder.setTitle(getTitleRes());
        builder.setPositiveButton(R.string.close, null);

        return builder.create();
    }

    private String readRawTextFile(@RawRes int id) {
        InputStream inputStream = getResources().openRawResource(id);
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
