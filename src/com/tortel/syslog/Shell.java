package com.tortel.syslog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class Shell {
	//Debug
	public static final boolean D = true;
	public static final String TAG = "SysLog";
	
	//Process variables
	private Process p;
	private DataOutputStream os;
	private DataInputStream is;
	
	//Root access variable
	private boolean root;
	
	
	//Initializes the shell object
	public Shell(){
		try {
			p = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(p.getOutputStream());
			is = new DataInputStream(p.getInputStream());
			checkRoot();
		} catch (IOException e) {
			Log.d(TAG,"Error: "+e.toString());
		} catch(NullPointerException e){
			Log.d(TAG,"No root access");
			root = false;
		}
	}
	
	/**
	 * Check for root access
	 */
	@SuppressWarnings("deprecation")
	private void checkRoot(){
		try {
			os.writeBytes("whoami\n");
			os.flush();
		} catch (IOException e) {
			Log.d(TAG,"Error: "+e.toString());
		}
		try {
			if(is.readLine().equals("root")){
				if(D) Log.d(TAG,"Root access");
				root = true;
				return;
			}
		} catch (IOException e) {
			root = false;
		}
		root = false;
	}
	
	/**
	 * Executes the given command
	 * @param cmd the command to execute
	 */
	public void exec(String cmd){
		if(D) Log.d(TAG,"Running:  "+cmd);
		if(root){
			try {
				os.writeBytes(cmd+" \n");
				os.flush();
			} catch (IOException e) {
				Log.e(TAG,"Error: "+e.toString(), e);
			}
		} else {
			//No root, just try and run it
			try {
				p = Runtime.getRuntime().exec(cmd);
				p.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Closes the shell object
	 */
	public void exit(){
		//Without root access, no shell is kept open
		if(root){
			try {
				os.writeBytes("exit\n");
				os.flush();
				p.waitFor();
				if(D && p.exitValue() != 255){
					Log.d(TAG,"Sucess");
				}
			} catch (IOException e) {
				Log.e(TAG,"Error: "+e.toString(),e);
			} catch (InterruptedException e) {
				Log.e(TAG,"Error: "+e.toString(),e);
			}
		}
	}

	public boolean root() {
		return root;
	}
	
}