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
import com.tortel.syslog.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;

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

        Bundle args = getArguments();
        mCommand = args.getParcelable(COMMAND);
        mLogTask.execute();
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
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_running, null, false);
        builder.customView(view, false);
        return builder.build();
    }

    private AsyncTask<Void, Void, Result> mLogTask = new AsyncTask<Void, Void, Result>(){

        /**
         * Process the logs
         */
        @Override
        protected Result doInBackground(Void... params) {
            Result result = new Result(false);
            result.setCommand(mCommand);

            try{
                Utils.runCommand(getActivity().getApplicationContext(), result);
            } catch(CreateFolderException e){
                //Error creating the folder
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_folder);
            } catch (FileNotFoundException e) {
                //Exception creating zip
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_zip);
            } catch (RunCommandException e) {
                //Exception running commands
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_commands);
            } catch (NoFilesException e) {
                //No files to zip
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_zip_nofiles);
            } catch (IOException e) {
                //Exception writing zip
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_zip);
            } catch(LowSpaceException e){
                e.printStackTrace();
                result.setException(e);
                result.setMessage(R.string.exception_space);
            } catch(Exception e){
                //Unknown exception
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(Result result){
            if(result.success()){
                String msg = getResources().getString(R.string.save_path)+result.getShortPath();
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();

                //Display a share intent
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("application/zip");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + result.getArchivePath()));

                if(Utils.isHandlerAvailable(getActivity().getApplicationContext(), share)){
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
                if(result.getException() instanceof LowSpaceException){
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
        }
    };
}
