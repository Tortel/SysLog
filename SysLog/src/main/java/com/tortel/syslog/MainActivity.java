/* SysLog - A simple logging tool
 * Copyright (C) 2013-2014  Scott Warner <Tortel1210@gmail.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.tortel.syslog.exception.*;
import com.tortel.syslog.Utils.CleanAllTask;
import com.tortel.syslog.Utils.CleanUncompressedTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends ActionBarActivity {
	private static final String KEY_KERNEL = "kernel";
	private static final String KEY_MAIN = "main";
	private static final String KEY_EVENT = "event";
	private static final String KEY_MODEM = "modem";
	private static final String KEY_LASTKMSG = "lastKmsg";
    private static final String KEY_SCRUB = "scrub";
	
	//Flags for running threads
	private static boolean running;

	private boolean kernelLog;
	private boolean lastKmsg;
	private boolean mainLog;
	private boolean eventLog;
	private boolean modemLog;
    private boolean scrubLog;

	private static boolean root;
	private ProgressDialog dialog;
	private EditText fileEditText;
	private EditText notesEditText;
	private EditText grepEditText;
	private Spinner grepSpinner;

    @Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		fileEditText = (EditText) findViewById(R.id.file_name);
		notesEditText = (EditText) findViewById(R.id.notes);
		grepEditText = (EditText) findViewById(R.id.grep_string);
		grepSpinner = (Spinner) findViewById(R.id.grep_log);
		
        //Load the logging options
        loadSettings();
		
		//Create a new shell object
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
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		if(running){
			showRunningDialog();
		}
	}

    @Override
	public void onResume(){
		super.onResume();
		
		//Load the logging options
		loadSettings();
	}
	
	private void loadSettings(){
	    SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        kernelLog = prefs.getBoolean(KEY_KERNEL, true);
        mainLog = prefs.getBoolean(KEY_MAIN, true);
        eventLog = prefs.getBoolean(KEY_EVENT, true);
        modemLog = prefs.getBoolean(KEY_MODEM, true);
        lastKmsg = prefs.getBoolean(KEY_LASTKMSG, true);
        scrubLog = prefs.getBoolean(KEY_SCRUB, true);
        
        // Set the checkboxes
        setCheckBoxes();
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.clean_uncompressed:
			new CleanUncompressedTask(getBaseContext()).execute();
			return true;
		case R.id.clean_all:
			new CleanAllTask(getBaseContext()).execute();
			return true;
		case R.id.change_path:
		    showPathDialog();
		    return true;
		case R.id.about:
			showAboutDialog();
			return true;
		case R.id.faq:
		    showFaqDialog();
		    return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
     * Shows the change path dialog
     */
    private void showPathDialog(){
        CustomPathDialog dialog = new CustomPathDialog();
        dialog.show(getSupportFragmentManager(), "path");
    }
	
	/**
	 * Shows the About dialog box
	 */
	private void showAboutDialog(){
		AboutDialog dialog = new AboutDialog();
		dialog.show(getSupportFragmentManager(), "about");
	}
	
    /**
     * Shows the FAQ dialog box
     */
    private void showFaqDialog(){
        FaqDialog dialog = new FaqDialog();
        dialog.show(getSupportFragmentManager(), "faq");
    }
	
	/**
	 * Sets the checkboxes according to what the user selected. 
	 */
	private void setCheckBoxes(){
		CheckBox box = (CheckBox) findViewById(R.id.main_log);
		box.setChecked(mainLog);
        box = (CheckBox) findViewById(R.id.event_log);
        box.setChecked(eventLog);
		box = (CheckBox) findViewById(R.id.modem_log);
		box.setChecked(modemLog);
		box = (CheckBox) findViewById(R.id.kernel_log);
		box.setChecked(kernelLog);
		box = (CheckBox) findViewById(R.id.last_kmsg);
		box.setChecked(lastKmsg);
        box = (CheckBox) findViewById(R.id.scrub_logs);
        box.setChecked(scrubLog);
		
		// Set the warning for modem logs
		TextView view = (TextView) findViewById(R.id.warnings);
		if(modemLog){
		    view.setText(R.string.warn_modem);
		    view.setVisibility(View.VISIBLE);
		} else {
		    view.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Logging options were changed
	 * @param v
	 */
	public void logChange(View v){
		
		CheckBox box = (CheckBox) v;
		Editor prefs = getPreferences(Activity.MODE_PRIVATE).edit();
		
		switch(box.getId()){
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
	        TextView view = (TextView) findViewById(R.id.warnings);
	        if(modemLog){
	            view.setText(R.string.warn_modem);
	            view.setVisibility(View.VISIBLE);
	        } else {
	            view.setVisibility(View.GONE);
	        }
			break;
        case R.id.scrub_logs:
            scrubLog = box.isChecked();
            prefs.putBoolean(KEY_SCRUB, scrubLog);
            break;
		}
		
		//Make sure that at least one type is selected
		enableLogButton(mainLog || eventLog || lastKmsg
		        || modemLog || kernelLog);
		
		//Save the settings
		prefs.apply();
	}
	
	private void enableLogButton(boolean flag){
		Button button = (Button) findViewById(R.id.take_log);
		button.setEnabled(flag);
		button.setText(R.string.take_log);
	}
	
	/**
	 * Start the logging process
	 * @param v
	 */
	public void startLog(View v){
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
			
			//Grep options
			command.setGrepOption(GrepOption.fromString(grepSpinner.getSelectedItem().toString()));
			command.setGrep(grepEditText.getText().toString());
			
			//Notes/text
			command.setAppendText(fileEditText.getText().toString());
			command.setNotes(notesEditText.getText().toString());
			
			command.setRoot(root);
			
			new LogTask().execute(command);
		} else {
			Toast.makeText(this, R.string.storage_err, Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Show the running dialog box
	 */
	private void showRunningDialog(){
		dialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.working));
	}
	
	/**
	 * Checks if options are available, such as last_kmsg or a radio.
	 * If they are not available, disable the check boxes.
	 */
	private class CheckOptionsTask extends AsyncTask<Void, Void, Void>{
		private boolean hasLastKmsg = false;
		private boolean hasRadio = false;

        @Override
		protected Void doInBackground(Void... params) {
			File lastKmsg = new File(Utils.LAST_KMSG);
			hasLastKmsg = lastKmsg.exists();
			TelephonyManager manager = (TelephonyManager)getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
			hasRadio = manager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
			return null;
		}

        @Override
		protected void onPostExecute(Void param){
			if(!hasLastKmsg){
				CheckBox lastKmsgBox = (CheckBox) findViewById(R.id.last_kmsg);
				lastKmsgBox.setChecked(false);
				lastKmsgBox.setEnabled(false);
				logChange(lastKmsgBox);
			}
			if(!hasRadio){
				CheckBox modemCheckBox = (CheckBox) findViewById(R.id.modem_log);
				modemCheckBox.setChecked(false);
				modemCheckBox.setEnabled(false);
				logChange(modemCheckBox);
			}
		}
	}
	
	private class CheckRootTask extends AsyncTask<Void, Void, Boolean>{

        @Override
		protected Boolean doInBackground(Void... params) {
			root = Shell.SU.available();
			return root;
		}

        @Override
		protected void onPostExecute(Boolean root){
			//Check for root access
			if(!root){
				//Warn the user
				TextView noRoot = (TextView) findViewById(R.id.warn_root);
				noRoot.setVisibility(View.VISIBLE);
				if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN){
					//JB and higher needs a different warning
					noRoot.setText(R.string.noroot_jb);
				}
			}
			
			enableLogButton(true);
		}
		
	}
	
	private class LogTask extends AsyncTask<RunCommand, Void, Result> {
        @Override
		protected void onPreExecute(){
			showRunningDialog();
			running = true;
		}
		
		/**
		 * Process the logs
		 */
        @Override
		protected Result doInBackground(RunCommand... params) {
			RunCommand command = params[0];
			Result result = new Result(false);
			result.setCommand(command);

			try{
			    Utils.runCommand(getBaseContext(), result);
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
			running = false;
			try{
				dialog.dismiss();
			} catch (Exception e){
				// Should cover null pointer/leaked view exceptions from rotation/ect
			}

			if(result.success()){
				String msg = getResources().getString(R.string.save_path)+result.getShortPath();
				Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
				
				//Display a share intent
				Intent share = new Intent(android.content.Intent.ACTION_SEND);
				share.setType("application/zip");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+result.getArchivePath()));
				
				if(Utils.isHandlerAvailable(getApplicationContext(), share)){
					startActivity(share);
				} else {
				    result.setMessage(R.string.exception_send);
				    result.setException(null);

				    //Show the error dialog. It will have stacktrace/bugreport disabled
				    ExceptionDialog dialog = new ExceptionDialog();
	                dialog.setResult(result);
	                dialog.show(getSupportFragmentManager(), "exceptionDialog");
				}
			} else {
			    if(result.getException() instanceof LowSpaceException){
			        //Show the low space dialog
                    LowSpaceDialog dialog = new LowSpaceDialog();
                    dialog.setResult(result);
                    dialog.show(getSupportFragmentManager(), "exceptionDialog");
			    } else {
    			    //Show the error dialog
    				ExceptionDialog dialog = new ExceptionDialog();
    				dialog.setResult(result);
    				dialog.show(getSupportFragmentManager(), "exceptionDialog");
			    }
			}
			
			fileEditText.setText("");
			notesEditText.setText("");
			grepEditText.setText("");
		}
		
	}
}
