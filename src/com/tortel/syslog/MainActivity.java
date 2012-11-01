package com.tortel.syslog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;


public class MainActivity extends Activity {
	private boolean kernelLog;
	private boolean mainLog;
	private boolean modemLog;
	private ProgressDialog dialog;
	private Shell shell;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//Load the logging options
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		kernelLog = prefs.getBoolean("kerel", true);
		mainLog = prefs.getBoolean("main", true);
		modemLog = prefs.getBoolean("modem", true);
		
		//Set the checkboxes
		setCheckBoxes();
		
		shell = new Shell();
		
		//Check for root access
		if(!shell.root()){
			Toast.makeText(this, R.string.noroot, Toast.LENGTH_LONG).show();
		}
	}
	
	public void onDestroy(){
		super.onDestroy();
		
		if(shell != null){
			shell.exit();
		}
	}
	
	
	public void onResume(){
		super.onResume();
		
		if(shell == null){
			shell = new Shell();
		}
		
		//Load the logging options
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		kernelLog = prefs.getBoolean("kerel", true);
		mainLog = prefs.getBoolean("main", true);
		modemLog = prefs.getBoolean("modem", true);
		setCheckBoxes();
	}
	
	private void setCheckBoxes(){
		CheckBox box = (CheckBox) findViewById(R.id.kernel_log);
		box.setChecked(kernelLog);
		box = (CheckBox) findViewById(R.id.main_log);
		box.setChecked(mainLog);
		box = (CheckBox) findViewById(R.id.modem_log);
		box.setChecked(modemLog);
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
	
	private class LogTask extends AsyncTask<Void, Void, Boolean> {
		private String tarPath;
		
		protected void onPreExecute(){
			dialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.working));
		}
		
		/**
		 * Process the logs
		 */
		protected Boolean doInBackground(Void... params) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				//Check the shell
				if(shell == null){
					shell = new Shell();
				}
				
				//Create the directories
			    String path = Environment.getExternalStorageDirectory().getPath();
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm", Locale.US);
			    Date date = new Date();
				path += "/SysLog/"+sdf.format(date)+"/";
			    File outPath = new File(path);
			    Log.v(Shell.TAG, "Path: "+path);
			    if(!outPath.mkdirs()){
			    	//If Java wont do it, just run the command
			    	shell.exec("mkdir -p "+path);
			    }
			    
			    //Dump the logs
			    if(kernelLog){
			    	shell.exec("dmesg > "+path+"dmesg.log && echo ''");
			    }
			    if(mainLog){
			    	shell.exec("logcat -v time -d -f "+path+"logcat.log");
			    }
			    if(modemLog){
			    	shell.exec("logcat -v time -b radio -d -f "+path+"modem.log");
			    }
			    
			    //Change directory
			    shell.exec("cd "+path);
			    
			    //Compress them
			    tarPath = path+"logs-"+sdf.format(date)+".tar";
			    
			    shell.exec("tar -cf "+tarPath+" *.log");
			    
			    
				return true;
			}
			return false;
		}
		
		protected void onPostExecute(Boolean result){
			dialog.dismiss();
			if(result){
				Toast.makeText(getBaseContext(), R.string.done, Toast.LENGTH_SHORT).show();
				
				//Display a share intent
				Intent share = new Intent(android.content.Intent.ACTION_SEND);
				share.setType("application/x-tar");
				share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+tarPath));
				
				startActivity(share);
			} else {
				Toast.makeText(getBaseContext(), R.string.error, Toast.LENGTH_LONG).show();
			}
		}
		
	}

}
