package com.tortel.syslog.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.exception.CreateFolderException;
import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.exception.RunCommandException;
import com.tortel.syslog.utils.GrabLogThread;
import com.tortel.syslog.utils.Log;
import com.tortel.syslog.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Fragment which actually runs the commands
 */
public class RunningDialog extends DialogFragment {
    public static final String COMMAND = "command";

    private RunCommand mCommand;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
        // Register for EventBus notifications
        EventBus.getDefault().register(this);

        Bundle args = getArguments();
        mCommand = args.getParcelable(COMMAND);
        // Start the background thread
        Thread thread = new Thread(new GrabLogThread(mCommand, getActivity()));
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister from event bus
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.autoDismiss(false);
        builder.progress(true, 0);
        builder.content(R.string.working);
        return builder.build();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogResult(Result result){
        Log.v("Received Result via EventBus");
        try {
            if (result.success()) {
                String msg = getResources().getString(R.string.save_path) + result.getShortPath();
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();

                //Display a share intent
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("application/zip");
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + result.getArchivePath()));

                if (Utils.isHandlerAvailable(getActivity().getApplicationContext(), share)) {
                    startActivity(share);
                } else {
                    result.setMessage(R.string.exception_send);
                    result.setException(null);

                    //Show the error dialog. It will have stacktrace/bugreport disabled
                    ExceptionDialog dialog = new ExceptionDialog();
                    dialog.setResult(result);
                    dialog.show(getFragmentManager(), "exceptionDialog");
                }
            } else {
                if (result.getException() instanceof LowSpaceException) {
                    //Show the low space dialog
                    LowSpaceDialog dialog = new LowSpaceDialog();
                    dialog.setResult(result);
                    dialog.show(getFragmentManager(), "exceptionDialog");
                } else {
                    //Show the error dialog
                    ExceptionDialog dialog = new ExceptionDialog();
                    dialog.setResult(result);
                    dialog.show(getFragmentManager(), "exceptionDialog");
                }
            }
            // Close the dialog
            dismiss();
        } catch(IllegalStateException e){
            Log.v("Ignorning IllegalStateException - The user probably left the application, and we tried to show a dialog");
        }
    }

}
