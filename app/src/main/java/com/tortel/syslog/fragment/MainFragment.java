/* SysLog - A simple logging tool
 * Copyright (C) 2013-2019  Scott Warner <Tortel1210@gmail.com>
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
package com.tortel.syslog.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tortel.syslog.FragmentMainActivity;
import com.tortel.syslog.GrepOption;
import com.tortel.syslog.R;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.dialog.RunningDialog;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Prefs;
import com.tortel.syslog.utils.Utils;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

/**
 * Fragment which contains the logic for the main UI
 */
public class MainFragment extends Fragment implements View.OnClickListener {
    private boolean kernelLog;
    private boolean lastKmsg;
    private boolean pstore;
    private boolean mainLog;
    private boolean eventLog;
    private boolean modemLog;
    private boolean auditLog;
    private boolean scrubLog;

    private boolean root;
    private EditText fileEditText;
    private EditText notesEditText;
    private EditText grepEditText;
    private Spinner grepSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        loadSettings();
    }

    private void loadSettings(){
        Log.d("Loading settings");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        kernelLog = prefs.getBoolean(Prefs.KEY_KERNEL, true);
        mainLog = prefs.getBoolean(Prefs.KEY_MAIN, true);
        eventLog = prefs.getBoolean(Prefs.KEY_EVENT, true);
        modemLog = prefs.getBoolean(Prefs.KEY_MODEM, true);
        lastKmsg = prefs.getBoolean(Prefs.KEY_LASTKMSG, true);
        auditLog = prefs.getBoolean(Prefs.KEY_AUDIT, Utils.isSeAndroid());
        scrubLog = prefs.getBoolean(Prefs.KEY_SCRUB, true);
        pstore = prefs.getBoolean(Prefs.KEY_PSTORE, true);
    }

    /**
     * Sets the checkboxes according to what the user selected.
     */
    private void setCheckBoxes(){
        // prevent NPEs
        if(getView() == null){
            return;
        }
        Log.d("Setting the checkboxes");

        CheckBox box = getView().findViewById(R.id.main_log);
        box.setChecked(mainLog);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.event_log);
        box.setChecked(eventLog);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.modem_log);
        box.setChecked(modemLog);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.kernel_log);
        box.setChecked(kernelLog);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.last_kmsg);
        box.setChecked(lastKmsg);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.pstore);
        box.setChecked(pstore);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.scrub_logs);
        box.setChecked(scrubLog);
        box.setOnClickListener(this);
        box = getView().findViewById(R.id.audit_log);
        box.setChecked(auditLog);
        box.setOnClickListener(this);
        // Hide the audit logs if the android version doesn't support selinux
        if(!Utils.isSeAndroid()){
            box.setVisibility(View.GONE);
        }

        // Set the warning for modem logs
        TextView view = (TextView) getView().findViewById(R.id.warnings);
        if(modemLog){
            view.setText(R.string.warn_modem);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void enableLogButton(boolean flag){
        if(getView() == null){
            return;
        }
        Log.d("Enabling log button: "+flag);
        Button button = (Button) getView().findViewById(R.id.take_log);
        button.setEnabled(flag);
        button.setText(R.string.take_log);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main, container, false);
        view.findViewById(R.id.take_log).setOnClickListener(this);

        fileEditText = view.findViewById(R.id.file_name);
        notesEditText = view.findViewById(R.id.notes);
        grepEditText = view.findViewById(R.id.grep_string);
        grepSpinner = view.findViewById(R.id.grep_log);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check for root
        if(!root){
            new CheckRootTask().execute();
        } else {
            enableLogButton(true);
        }

        //Check for last_kmsg and modem
        new CheckOptionsTask().execute();

        //Set the checkboxes
        setCheckBoxes();

        //Hide the keyboard on open
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onClick(View v) {
        if(v instanceof CheckBox) {
            CheckBox box = (CheckBox) v;
            SharedPreferences.Editor prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()).edit();

            switch (box.getId()) {
                case R.id.kernel_log:
                    kernelLog = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_KERNEL, kernelLog);
                    break;
                case R.id.last_kmsg:
                    lastKmsg = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_LASTKMSG, lastKmsg);
                    break;
                case R.id.pstore:
                    pstore = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_PSTORE, pstore);
                    break;
                case R.id.main_log:
                    mainLog = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_MAIN, mainLog);
                    break;
                case R.id.event_log:
                    eventLog = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_EVENT, eventLog);
                    break;
                case R.id.modem_log:
                    modemLog = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_MODEM, modemLog);
                    // Set the warning for modem logs
                    TextView view = getView().findViewById(R.id.warnings);
                    if (modemLog) {
                        view.setText(R.string.warn_modem);
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                    break;
                case R.id.audit_log:
                    auditLog = box.isChecked();
                    prefs.putBoolean(Prefs.KEY_AUDIT, auditLog);
                    break;
                case R.id.scrub_logs:
                    scrubLog = box.isChecked();
                    if(scrubLog){
                        // Force a check of permissions needed to scrub the logs
                        ((FragmentMainActivity) getActivity()).checkScrubPermissions();
                    }
                    prefs.putBoolean(Prefs.KEY_SCRUB, scrubLog);
                    break;
            }

            //Make sure that at least one type is selected
            enableLogButton(mainLog || eventLog || lastKmsg
                    || modemLog || kernelLog || auditLog);

            //Save the settings
            prefs.apply();
        } else if(v.getId() == R.id.take_log) {
            // Force a check of our permissions. Stop if we don't have them
            if(!((FragmentMainActivity) getActivity()).checkRequiredPermissions()){
                return;
            }
            if(scrubLog){
                if(!((FragmentMainActivity) getActivity()).checkScrubPermissions()){
                    return;
                }
            }

            //Check for external storage
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                //Build the command
                RunCommand command = new RunCommand();

                //Log flags
                command.setKernelLog(kernelLog);
                command.setLastKernelLog(lastKmsg);
                command.setPstore(pstore);
                command.setMainLog(mainLog);
                command.setEventLog(eventLog);
                command.setModemLog(modemLog);
                command.setScrubEnabled(scrubLog);
                command.setAuditLog(auditLog);

                //Grep options
                command.setGrepOption(GrepOption.fromString(grepSpinner.getSelectedItem().toString()));
                command.setGrep(grepEditText.getText().toString());

                //Notes/text
                command.setAppendText(fileEditText.getText().toString());
                command.setNotes(notesEditText.getText().toString());

                command.setRoot(root);

                fileEditText.setText("");
                notesEditText.setText("");
                grepEditText.setText("");

                RunningDialog dialog = new RunningDialog();
                Bundle args = new Bundle();
                args.putParcelable(RunningDialog.COMMAND, command);
                dialog.setArguments(args);

                dialog.show(getFragmentManager(), "run");
            } else {
                Toast.makeText(getActivity(), R.string.storage_err, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Checks if options are available, such as last_kmsg or a radio.
     * If they are not available, disable the check boxes.
     */
    private class CheckOptionsTask extends AsyncTask<Void, Void, Void> {
        private boolean hasLastKmsg = false;
        private boolean hasRadio = false;

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("Checking if last_kmsg and radio are available");
            File lastKmsg = new File(Utils.LAST_KMSG);
            hasLastKmsg = lastKmsg.exists();
            try {
                TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                hasRadio = manager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
            } catch (Exception e) {
                // Default to true - most of the crashes here are on phones
                hasRadio = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param){
            if(!hasLastKmsg){
                CheckBox lastKmsgBox = (CheckBox) getView().findViewById(R.id.last_kmsg);
                lastKmsgBox.setChecked(false);
                lastKmsgBox.setEnabled(false);
                onClick(lastKmsgBox);
            }
            if(!hasRadio){
                CheckBox modemCheckBox = (CheckBox) getView().findViewById(R.id.modem_log);
                modemCheckBox.setChecked(false);
                modemCheckBox.setEnabled(false);
                onClick(modemCheckBox);
            }
        }
    }

    private class CheckRootTask extends AsyncTask<Void, Void, Boolean>{
        @Override
        protected Boolean doInBackground(Void... params) {
            Log.d("Checking for root");
            root = Shell.SU.available();
            return root;
        }

        @Override
        protected void onPostExecute(Boolean root){
            try {
                //Check for root access
                if (!root) {
                    Log.d("Root not available");
                    //Warn the user
                    TextView noRoot = getView().findViewById(R.id.warn_root);
                    Linkify.addLinks(noRoot, Linkify.ALL);
                    noRoot.setMovementMethod(LinkMovementMethod.getInstance());
                    noRoot.setVisibility(View.VISIBLE);
                }

                enableLogButton(true);
            } catch (NullPointerException e) {
                // Supress it
            }
        }
    }

}
