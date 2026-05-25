/* SysLog - A simple logging tool
 * Copyright (C) 2013-2026 Scott Warner <Tortel1210@gmail.com>
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

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tortel.syslog.GrepOption;
import com.tortel.syslog.R;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.databinding.MainBinding;
import com.tortel.syslog.dialog.RunningDialog;
import com.tortel.syslog.utils.FileUtils;
import com.tortel.syslog.utils.GrabLogThread;
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
    private boolean hasReadLogs;
    private MainBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        loadSettings();

        if (savedInstanceState == null && !GrabLogThread.isRunning()) {
            // Clean all uncompressed logs
            FileUtils.cleanAllUncompressed(requireContext());
        }
    }

    private void loadSettings() {
        Log.d("Loading settings");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        kernelLog = prefs.getBoolean(Prefs.KEY_KERNEL, true);
        mainLog = prefs.getBoolean(Prefs.KEY_MAIN, true);
        eventLog = prefs.getBoolean(Prefs.KEY_EVENT, true);
        modemLog = prefs.getBoolean(Prefs.KEY_MODEM, true);
        lastKmsg = prefs.getBoolean(Prefs.KEY_LASTKMSG, true);
        auditLog = prefs.getBoolean(Prefs.KEY_AUDIT, true);
        scrubLog = prefs.getBoolean(Prefs.KEY_SCRUB, true);
        pstore = prefs.getBoolean(Prefs.KEY_PSTORE, true);
    }

    /**
     * Sets the checkboxes according to what the user selected.
     */
    private void setCheckBoxes() {
        // Prevent NPEs
        if (binding == null) {
            return;
        }
        Log.d("Setting the checkboxes");

        binding.mainLog.setChecked(mainLog);
        binding.mainLog.setOnClickListener(this);

        binding.eventLog.setChecked(eventLog);
        binding.eventLog.setOnClickListener(this);

        binding.modemLog.setChecked(modemLog);
        binding.modemLog.setOnClickListener(this);

        binding.kernelLog.setChecked(kernelLog);
        binding.kernelLog.setOnClickListener(this);

        binding.lastKmsg.setChecked(lastKmsg);
        binding.lastKmsg.setOnClickListener(this);

        binding.pstore.setChecked(pstore);
        binding.pstore.setOnClickListener(this);

        binding.scrubLogs.setChecked(scrubLog);
        binding.scrubLogs.setOnClickListener(this);

        binding.auditLog.setChecked(auditLog);
        binding.auditLog.setOnClickListener(this);

        // Set the warning for modem logs
        if (modemLog) {
            binding.warnings.setText(R.string.warn_modem);
            binding.warningsCardView.setVisibility(View.VISIBLE);
            binding.warnings.setVisibility(View.VISIBLE);
        } else {
            binding.warningsCardView.setVisibility(View.GONE);
            binding.warnings.setVisibility(View.GONE);
        }
    }

    private void enableLogButton(boolean flag) {
        if (binding == null) {
            return;
        }
        Log.d("Enabling log button: " + flag);

        binding.takeLog.setEnabled(flag);
        MaterialButton takeLog = (MaterialButton) binding.takeLog;
        takeLog.setText(getString(R.string.take_log));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = MainBinding.inflate(inflater, container, false);
        binding.takeLog.setOnClickListener(this);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clear the binding
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check for root
        if (!root) {
            checkAccess();
        } else {
            enableLogButton(true);
        }

        // Check for last_kmsg and modem
        checkOptions();

        // Set the checkboxes
        setCheckBoxes();

        // Hide the keyboard on open
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    /**
     * Wraper to get the text value from an edittext with null checks
     *
     * @param input edit text
     * @return the text content or an empty string
     */
    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CheckBox box) {
            SharedPreferences.Editor prefs = PreferenceManager
                    .getDefaultSharedPreferences(requireContext()).edit();

            if (box.getId() == R.id.kernel_log) {
                kernelLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_KERNEL, kernelLog);
            } else if (box.getId() == R.id.last_kmsg) {
                lastKmsg = box.isChecked();
                prefs.putBoolean(Prefs.KEY_LASTKMSG, lastKmsg);
            } else if (box.getId() == R.id.pstore) {
                pstore = box.isChecked();
                prefs.putBoolean(Prefs.KEY_PSTORE, pstore);
            } else if (box.getId() == R.id.main_log) {
                mainLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_MAIN, mainLog);
            } else if (box.getId() == R.id.event_log) {
                eventLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_EVENT, eventLog);
            } else if (box.getId() == R.id.modem_log) {
                modemLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_MODEM, modemLog);
                // Set the warning for modem logs
                if (modemLog) {
                    binding.warnings.setText(R.string.warn_modem);
                    binding.warnings.setVisibility(View.VISIBLE);
                } else {
                    binding.warnings.setVisibility(View.GONE);
                }
            } else if (box.getId() == R.id.audit_log) {
                auditLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_AUDIT, auditLog);
            } else if (box.getId() == R.id.scrub_logs) {
                scrubLog = box.isChecked();
                prefs.putBoolean(Prefs.KEY_SCRUB, scrubLog);
            }

            // Make sure that at least one type is selected
            enableLogButton(mainLog || eventLog || lastKmsg
                    || modemLog || kernelLog || auditLog);

            // Save the settings
            prefs.apply();
        } else if (v.getId() == R.id.take_log) {
            // Build the command
            RunCommand command = new RunCommand();

            // Log flags
            command.setKernelLog(kernelLog);
            command.setLastKernelLog(lastKmsg);
            command.setPstore(pstore);
            command.setMainLog(mainLog);
            command.setEventLog(eventLog);
            command.setModemLog(modemLog);
            command.setScrubEnabled(scrubLog);
            command.setAuditLog(auditLog);

            // Grep options
            command.setGrepOption(GrepOption.fromString(binding.grepLog.getSelectedItem().toString()));
            command.setGrep(getText(binding.grepString));

            // Notes/text
            command.setAppendText(getText(binding.fileName));
            command.setNotes(getText(binding.notes));

            command.setRoot(root);

            binding.fileName.setText("");
            binding.notes.setText("");
            binding.grepString.setText("");

            RunningDialog dialog = new RunningDialog();
            Bundle args = new Bundle();
            args.putParcelable(RunningDialog.COMMAND, command);
            dialog.setArguments(args);

            dialog.show(getParentFragmentManager(), "run");
        }
    }

    /**
     * Checks if options are available, such as last_kmsg or a radio.
     * If they are not available, disable the check boxes.
     */
    private void checkOptions() {
        final Context context = requireContext();
        (new Thread() {
            @Override
            public void run() {
                final boolean hasLastKmsg;
                final boolean hasRadio;
                boolean hasRadioTemp;
                Log.d("Checking if last_kmsg and radio are available");
                File lastKmsg = new File(Utils.LAST_KMSG);
                hasLastKmsg = lastKmsg.exists();
                try {
                    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    hasRadioTemp = manager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
                } catch (Exception e) {
                    // Default to true - most of the crashes here are on phones
                    hasRadioTemp = true;
                }

                hasRadio = hasRadioTemp;
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    if (binding != null) {
                        if (!hasLastKmsg) {
                            binding.lastKmsg.setChecked(false);
                            binding.lastKmsg.setEnabled(false);
                            onClick(binding.lastKmsg);
                        }
                        if (!hasRadio) {
                            binding.modemLog.setChecked(false);
                            binding.modemLog.setEnabled(false);
                            onClick(binding.modemLog);
                        }
                    }
                });
            }
        }).start();
    }

    private void checkAccess() {
        final Context context = requireContext();
        (new Thread() {

            @Override
            public void run() {
                Log.d("Checking for root");
                root = Shell.SU.available();

                hasReadLogs = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED;

                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    try {
                        // Check for root access or READ_LOGS permission
                        if (!root && !hasReadLogs) {
                            Log.d("Root not available and READ_LOGS permission not granted");
                            // Warn the user
                            Linkify.addLinks(binding.warnRoot, Linkify.ALL);
                            binding.warnRoot.setMovementMethod(LinkMovementMethod.getInstance());
                            binding.warningsCardView.setVisibility(View.VISIBLE);
                            binding.warnRoot.setVisibility(View.VISIBLE);
                        }

                        enableLogButton(true);
                    } catch (NullPointerException e) {
                        // Suppress it
                    }
                });
            }
        }).start();
    }

}
