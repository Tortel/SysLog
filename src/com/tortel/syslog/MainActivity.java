/* SysLog - A simple logging tool
 * Copyright (C) 2013  Scott Warner <Tortel1210@gmail.com>
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
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends Activity {
	private static final String TAG = "SysLog";

	private boolean kernelLog;
	private boolean lastKmsg;
	private boolean mainLog;
	private boolean modemLog;
	private boolean root;
	private ProgressDialog dialog;
	private EditText fileEditText;
	private EditText notesEditText;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//Load the logging options
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		kernelLog = prefs.getBoolean("kernel", true);
		mainLog = prefs.getBoolean("main", true);
		modemLog = prefs.getBoolean("modem", true);
		lastKmsg = prefs.getBoolean("lastKmsg", true);
		
		fileEditText = (EditText) findViewById(R.id.file_name);
		notesEditText = (EditText) findViewById(R.id.notes);
		
		//Create a new shell object
		new CheckRootTask().execute();
		
		//Set the checkboxes
		setCheckBoxes();
	}
	
	public void onResume(){
		super.onResume();
		
		//Load the logging options
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		kernelLog = prefs.getBoolean("kernel", true);
		mainLog = prefs.getBoolean("main", true);
		modemLog = prefs.getBoolean("modem", true);
		lastKmsg = prefs.getBoolean("lastKmsg", true);
		fileEditText = (EditText) findViewById(R.id.file_name);
		notesEditText = (EditText) findViewById(R.id.notes);
		
		setCheckBoxes();
	}
	
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.clean_uncompressed:
			new CleanUncompressedTask().execute();
			return true;
		case R.id.clean_all:
			new CleanAllTask().execute();
			return true;
		case R.id.about:
			showAboutDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void showAboutDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.information).setTitle(R.string.app_name);
		
		builder.setPositiveButton(R.string.close, null);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	private void setCheckBoxes(){
		CheckBox box = (CheckBox) findViewById(R.id.main_log);
		box.setChecked(mainLog);
		box = (CheckBox) findViewById(R.id.modem_log);
		box.setChecked(modemLog);
		box = (CheckBox) findViewById(R.id.kernel_log);
		box.setChecked(kernelLog);
		box = (CheckBox) findViewById(R.id.last_kmsg);
		box.setChecked(lastKmsg);
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
			prefs.putBoolean("kernel", kernelLog);
			break;
		case R.id.last_kmsg:
			lastKmsg = box.isChecked();
			prefs.putBoolean("lastKmsg", lastKmsg);
			break;
		case R.id.main_log:
			mainLog = box.isChecked();
			prefs.putBoolean("main", mainLog);
			break;
		case R.id.modem_log:
			modemLog = box.isChecked();
			prefs.putBoolean("modem", modemLog);
			break;
		}
		
		//Save the settings
		prefs.apply();
	}
	
	/**
	 * Start the logging process
	 * @param v
	 */
	public void startLog(View v){
		//Check for external storage
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
			new LogTask().execute();
		} else {
			Toast.makeText(this, R.string.storage_err, Toast.LENGTH_LONG).show();
		}
	}
	
	private class CleanAllTask extends AsyncTask<Void, Void, Void>{
		protected Void doInBackground(Void... params) {
			String path = Environment.getExternalStorageDirectory().getPath();
			path += "/SysLog/*";
			Shell.SH.run("rm -rf "+path);
			return null;
		}
		
		protected void onPostExecute(Void param){
			Toast.makeText(getBaseContext(), R.string.done, Toast.LENGTH_SHORT).show();
		}
	}
	
	private class CleanUncompressedTask extends AsyncTask<Void, Void, Void>{
		protected Void doInBackground(Void... params) {
			String path = Environment.getExternalStorageDirectory().getPath();
			path += "/SysLog/*/";
			//All the log files end in .log, and there are also notes.txt files
			String commands[] = new String[2];
			commands[0] = "rm "+path+"*.log";
			commands[1] = "rm "+path+"*.txt";
			Shell.SH.run(commands);
			return null;
		}
		
		protected void onPostExecute(Void param){
			Toast.makeText(getBaseContext(), R.string.done, Toast.LENGTH_SHORT).show();
		}
	}
	
	private class CheckRootTask extends AsyncTask<Void, Void, Boolean>{

		protected Boolean doInBackground(Void... params) {
			root = Shell.SU.available();
			return root;
		}
		
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
			
			Button button = (Button) findViewById(R.id.take_log);
			button.setEnabled(true);
			button.setText(R.string.take_log);
		}
		
	}
	
	private class LogTask extends AsyncTask<Void, Void, Boolean> {
		private String archivePath;
		
		protected void onPreExecute(){
			dialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.working));
		}
		
		/**
		 * Process the logs
		 */
		protected Boolean doInBackground(Void... params) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				//Commands to execute
				ArrayList<String> commands = new ArrayList<String>(5);
				
				//Get the notes and string to append to file name
				String fileAppend = fileEditText.getText().toString().trim();
				String notes = notesEditText.getText().toString();
				
				//Create the directories
			    String path = Environment.getExternalStorageDirectory().getPath();
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm", Locale.US);
			    Date date = new Date();
				path += "/SysLog/"+sdf.format(date)+"/";
			    File outPath = new File(path);
			    Log.v(TAG, "Path: "+path);
			    if(!outPath.mkdirs()){
			    	//If Java wont do it, just run the command
			    	commands.add("mkdir -p "+path);
			    }
			    
			    //Commands to dump the logs
			    if(lastKmsg){
			    	//Try copying the last_kmsg over
			    	commands.add("cp /proc/last_kmsg "+path+"last_kmsg.log");
			    }
			    if(kernelLog){
			    	commands.add("dmesg > "+path+"dmesg.log && echo ''");
			    }
			    if(mainLog){
			    	commands.add("logcat -v time -d -f "+path+"logcat.log");
			    }
			    if(modemLog){
			    	commands.add("logcat -v time -b radio -d -f "+path+"modem.log");
			    }
			    
			    //Run the commands
			    if(root){
			    	Shell.SU.run(commands);
			    } else {
			    	Shell.SH.run(commands);
			    }
			    
			    //If there are notes, write them to a notes file
			    if(notes.length() > 0){
			    	File noteFile = new File(path+"/notes.txt");
			    	try{
			    		FileWriter writer = new FileWriter(noteFile);
			    		writer.write(notes);
			    		writer.close();
			    	} catch(Exception e){
			    		Log.e(TAG, "Exception writing notes", e);
			    	}
			    }
			    
			    //Append the users input into the zip
			    if(fileAppend.length() > 0){
			    	archivePath = sdf.format(date)+"-"+fileAppend+".zip";
			    } else {
			    	archivePath = sdf.format(date)+".zip";
			    }
			    ZipWriter writer = new ZipWriter(path, archivePath);
			    archivePath = path+archivePath;

			    return writer.createZip();
			}
			return false;
		}
		
		protected void onPostExecute(Boolean result){
			dialog.dismiss();
			if(result){
				Toast.makeText(getBaseContext(), R.string.done, Toast.LENGTH_SHORT).show();
				
				//Display a share intent
				Intent share = new Intent(android.content.Intent.ACTION_SEND);
				//share.setType("application/x-tar");
				share.setType("application/zip");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+archivePath));
				
				startActivity(share);
			} else {
				Toast.makeText(getBaseContext(), R.string.error, Toast.LENGTH_LONG).show();
			}
			
			fileEditText.setText("");
			notesEditText.setText("");
		}
		
	}

}
