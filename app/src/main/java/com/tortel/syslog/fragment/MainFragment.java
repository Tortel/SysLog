package com.tortel.syslog.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
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

import com.tortel.syslog.GrepOption;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.dialog.AboutDialog;
import com.tortel.syslog.dialog.CustomPathDialog;
import com.tortel.syslog.dialog.ExceptionDialog;
import com.tortel.syslog.dialog.FaqDialog;
import com.tortel.syslog.dialog.LowSpaceDialog;
import com.tortel.syslog.dialog.RunningDialog;
import com.tortel.syslog.exception.CreateFolderException;
import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.exception.RunCommandException;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import eu.chainfire.libsuperuser.Shell;

/**
 * Fragment which contains the logic for the main UI
 */
public class MainFragment extends Fragment implements View.OnClickListener {
    private static final String KEY_KERNEL = "kernel";
    private static final String KEY_MAIN = "main";
    private static final String KEY_EVENT = "event";
    private static final String KEY_MODEM = "modem";
    private static final String KEY_AUDIT = "audit";
    private static final String KEY_LASTKMSG = "lastKmsg";
    private static final String KEY_SCRUB = "scrub";

    private boolean kernelLog;
    private boolean lastKmsg;
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
        kernelLog = prefs.getBoolean(KEY_KERNEL, true);
        mainLog = prefs.getBoolean(KEY_MAIN, true);
        eventLog = prefs.getBoolean(KEY_EVENT, true);
        modemLog = prefs.getBoolean(KEY_MODEM, true);
        lastKmsg = prefs.getBoolean(KEY_LASTKMSG, true);
        auditLog = prefs.getBoolean(KEY_AUDIT, Utils.isSeAndroid());
        scrubLog = prefs.getBoolean(KEY_SCRUB, true);
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

        CheckBox box = (CheckBox) getView().findViewById(R.id.main_log);
        box.setChecked(mainLog);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.event_log);
        box.setChecked(eventLog);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.modem_log);
        box.setChecked(modemLog);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.kernel_log);
        box.setChecked(kernelLog);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.last_kmsg);
        box.setChecked(lastKmsg);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.scrub_logs);
        box.setChecked(scrubLog);
        box.setOnClickListener(this);
        box = (CheckBox) getView().findViewById(R.id.audit_log);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main, container, false);
        view.findViewById(R.id.take_log).setOnClickListener(this);

        fileEditText = (EditText) view.findViewById(R.id.file_name);
        notesEditText = (EditText) view.findViewById(R.id.notes);
        grepEditText = (EditText) view.findViewById(R.id.grep_string);
        grepSpinner = (Spinner) view.findViewById(R.id.grep_log);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
                    prefs.putBoolean(KEY_KERNEL, kernelLog);
                    break;
                case R.id.last_kmsg:
                    lastKmsg = box.isChecked();
                    prefs.putBoolean(KEY_LASTKMSG, lastKmsg);
                    break;
                case R.id.main_log:
                    mainLog = box.isChecked();
                    prefs.putBoolean(KEY_MAIN, mainLog);
                    break;
                case R.id.event_log:
                    eventLog = box.isChecked();
                    prefs.putBoolean(KEY_EVENT, eventLog);
                    break;
                case R.id.modem_log:
                    modemLog = box.isChecked();
                    prefs.putBoolean(KEY_MODEM, modemLog);
                    // Set the warning for modem logs
                    TextView view = (TextView) getView().findViewById(R.id.warnings);
                    if (modemLog) {
                        view.setText(R.string.warn_modem);
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                    break;
                case R.id.audit_log:
                    auditLog = box.isChecked();
                    prefs.putBoolean(KEY_AUDIT, auditLog);
                    break;
                case R.id.scrub_logs:
                    scrubLog = box.isChecked();
                    prefs.putBoolean(KEY_SCRUB, scrubLog);
                    break;
            }

            //Make sure that at least one type is selected
            enableLogButton(mainLog || eventLog || lastKmsg
                    || modemLog || kernelLog || auditLog);

            //Save the settings
            prefs.apply();
        } else if(v.getId() == R.id.take_log) {
            //Check for external storage
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                //Build the command
                RunCommand command = new RunCommand();

                //Log flags
                command.setKernelLog(kernelLog);
                command.setLastKernelLog(lastKmsg);
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
            TelephonyManager manager = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            hasRadio = manager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
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
            //Check for root access
            if(!root){
                Log.d("Root not available");
                //Warn the user
                TextView noRoot = (TextView) getView().findViewById(R.id.warn_root);
                noRoot.setVisibility(View.VISIBLE);
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN){
                    //JB and higher needs a different warning
                    noRoot.setText(R.string.noroot_jb);
                }
            }

            enableLogButton(true);
        }
    }

}
