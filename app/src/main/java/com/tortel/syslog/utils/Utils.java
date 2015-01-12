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
package com.tortel.syslog.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.tortel.syslog.GrepOption;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.ZipWriter;
import com.tortel.syslog.exception.CreateFolderException;
import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.exception.RunCommandException;

import eu.chainfire.libsuperuser.Shell;

/**
 * General utilities and the main run command code
 */
public class Utils {
    public static final String TAG = "SysLog";
    public static final String LAST_KMSG = "/proc/last_kmsg";
    public static final String PREF_PATH = "pref_root_path";
    public static final String ROOT_PATH = "/data/media/";

    private static final String PRESCRUB = "-prescrub";
    
    private static final int MB_TO_BYTE = 1048576;
    
    /**
     * Minimum amount of free space needed to not throw a LowSpaceException.
     * This is based on the average size of my runs (~5.5MB)
     */
    private static final double MIN_FREE_SPACE = 6;
    
    /**
     * Gets the free space of the primary storage, in MB
     * @return the space
     */
    @SuppressWarnings("deprecation")
    public static double getStorageFreeSpace(){
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double)stat.getAvailableBlocks()
                           * (double)stat.getBlockSize();
        return Math.floor(sdAvailSize / MB_TO_BYTE);
    }
    
    /**
     * Returns true if the build is SELinux protected, which is 4.3+
     * @return
     */
    public static boolean isSeAndroid(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
    
    /**
     * Runs the {@link com.tortel.syslog.RunCommand} contained within the {@link com.tortel.syslog.Result} passed in.<br />
     * On an error
     * @param result
     * @throws CreateFolderException
     * @throws RunCommandException
     * @throws IOException
     * @throws NoFilesException
     * @throws LowSpaceException 
     */
    public static void runCommand(Context context, Result result) throws CreateFolderException,
            RunCommandException, IOException, NoFilesException, LowSpaceException {
        RunCommand command = result.getCommand();
        String archivePath;
        
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //Check how much space is on the primary storage
            double freeSpace = getStorageFreeSpace();
            if(freeSpace < MIN_FREE_SPACE){
                throw new LowSpaceException(freeSpace);
            }
            
            //Commands to execute
            List<String> commands = new LinkedList<String>();

            //Create the directories
            String path = Environment.getExternalStorageDirectory().getPath();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm", Locale.US);
            Date date = new Date();
            File nomedia = new File(path+"/SysLog/.nomedia");
            path += "/SysLog/"+sdf.format(date)+"/";
            File outPath = new File(path);
            //Check if this path already exists (Happens if you run this multiple times a minute
            if(outPath.exists()){
                //Append the seconds
                path =  path.substring(0, path.length()-1) +"."+Calendar.getInstance().get(Calendar.SECOND)+"/";
                outPath = new File(path);
                Log.v(TAG, "Path already exists, added seconds");
            }

            Log.v(TAG, "Path: "+path);
            //Make the directory
            if(!outPath.mkdirs() && !outPath.isDirectory()){
                throw new CreateFolderException();
            }

            //Put a .nomedia file in the directory
            if(!nomedia.exists()){
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create .nomedia file", e);
                }
            }

            /*
             *  If the system is 4.3, some superuser setups (CM10.2) have issues accessing
             *  the normal external storage path. Replace the first portion of the path
             *  with /data/media/{UID}
             */
            String rootPath = path;
            if(isSeAndroid()){
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                
                rootPath = path.replaceAll("/storage/emulated/", prefs.getString(PREF_PATH, ROOT_PATH));
                Log.v(TAG, "Using path "+rootPath+" for root commands");
            }

            //Commands to dump the logs
            if(command.isMainLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MAIN
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -d | grep \""+command.getGrep()+"\" > "+rootPath+"logcat.log"+PRESCRUB);
                } else {
                    commands.add("logcat -v time -d -f "+rootPath+"logcat.log"+PRESCRUB);
                }
            }
            if(command.isEventLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.EVENT
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -b events -v time -d | grep \""+command.getGrep()+"\" > "+rootPath+"event.log"+PRESCRUB);
                } else {
                    commands.add("logcat -b events -v time -d -f "+rootPath+"event.log"+PRESCRUB);
                }
            }
            if(command.isKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("dmesg | grep \""+command.getGrep()+"\" > "+rootPath+"dmesg.log"+PRESCRUB);
                } else {
                    commands.add("dmesg > "+rootPath+"dmesg.log"+PRESCRUB);
                }
            }
            if(command.isModemLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MODEM
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -b radio -d | grep \""+command.getGrep()+"\" > "+rootPath+"modem.log"+PRESCRUB);
                } else {
                    commands.add("logcat -v time -b radio -d -f "+rootPath+"modem.log"+PRESCRUB);
                }
            }
            if(command.isLastKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.LAST_KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    //Log should be run through grep
                    commands.add("cat "+LAST_KMSG+" | grep \""+command.getGrep()+"\" > "+rootPath+"last_kmsg.log"+PRESCRUB);
                } else {
                    //Try copying the last_kmsg over
                    commands.add("cp "+LAST_KMSG+" "+rootPath+"last_kmsg.log"+PRESCRUB);
                }
            }

            /*
             * More 4.3+ SU issues - need to chown to media_rw
             */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                // Some kernels/systems may not work properly with /*
                // List the files explicitly
                commands.add("chown media_rw:media_rw "+rootPath+"/logcat.log"+PRESCRUB);
                commands.add("chown media_rw:media_rw "+rootPath+"/dmesg.log"+PRESCRUB);
                commands.add("chown media_rw:media_rw "+rootPath+"/modem.log"+PRESCRUB);
                commands.add("chown media_rw:media_rw "+rootPath+"/event.log"+PRESCRUB);
                commands.add("chown media_rw:media_rw "+rootPath+"/last_kmsg.log"+PRESCRUB);
                // Some Omni-based ROMs/kernels have issues even with the above
                // When in doubt, overkill it
                commands.add("chmod 666 "+rootPath+"/logcat.log"+PRESCRUB);
                commands.add("chmod 666 "+rootPath+"/event.log"+PRESCRUB);
                commands.add("chmod 666 "+rootPath+"/dmesg.log"+PRESCRUB);
                commands.add("chmod 666 "+rootPath+"/modem.log"+PRESCRUB);
                commands.add("chmod 666 "+rootPath+"/last_kmsg.log"+PRESCRUB);
            }

            //Run the commands
            if(command.hasRoot()){
                if(Shell.SU.run(commands) == null){
                    throw new RunCommandException();
                }
            } else {
                if(Shell.SH.run(commands) == null){
                    throw new RunCommandException();
                }
            }

            // Scrub the files
            scrubFiles(context, path, !command.isScrubEnabled());

            //If there are notes, write them to a notes file
            if(command.getNotes() != null && command.getNotes().length() > 0){
                File noteFile = new File(path+"/notes.txt");
                FileWriter writer = new FileWriter(noteFile);
                writer.write(command.getNotes());
                try{
                    writer.close();
                } catch(Exception e){
                    //Ignore
                }
            }

            //Append the users input into the zip
            if(command.getAppendText().length() > 0){
                archivePath = sdf.format(date)+"-"+command.getAppendText()+".zip";
            } else {
                archivePath = sdf.format(date)+".zip";
            }
            ZipWriter writer = new ZipWriter(path, archivePath);
            archivePath = path+archivePath;
            result.setArchivePath(archivePath);

            writer.createZip();
            result.setSuccess(true);
        } else {
            //Default storage not accessible
            result.setSuccess(false);
            result.setMessage(R.string.storage_err);
        }
    }

    /**
     * Runs the log file through the ScrubberUtils and removes the PRESCRUB extension
     * @param path
     */
    private static void scrubFiles(Context context, String path, boolean disable) {
        File logFolder = new File(path);
        File logFiles[] = logFolder.listFiles();
        for(File file : logFiles){
            // Save it to a file without the PRESCRUB extension
            File outFile = new File(path + "/" + file.getName().replace(PRESCRUB, ""));
            if(disable){
                Log.d(TAG, "Scrub disabled, renaming "+file.getName()+" to "+outFile.getName());
                file.renameTo(outFile);
            } else {
                Log.d(TAG, "Scrubbing " + file.getName() + " to " + outFile.getName());

                try {
                    ScrubberUtils.scrubFile(context, file, outFile);
                    file.delete();
                } catch (IOException e) {
                    Log.e(TAG, "Exception scrubbing file " + file.getName(), e);
                }
            }
        }
    }

    public static boolean isHandlerAvailable(Context ctx, Intent intent) {
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, 
              PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
     }
    
    /**
     * Clean all the saved log files
     */
    public static class CleanAllTask extends AsyncTask<Void, Void, Void>{
        private Context context;
        private double startingSpace;
        private double endingSpace;
        
        public CleanAllTask(Context context){
            this.context = context;
        }
        
        protected Void doInBackground(Void... params) {
            startingSpace = Utils.getStorageFreeSpace();
            String path = Environment.getExternalStorageDirectory().getPath();
            path += "/SysLog/*";
            Shell.SH.run("rm -rf "+path);
            endingSpace = Utils.getStorageFreeSpace();
            return null;
        }
        
        protected void onPostExecute(Void param){
            String spaceFreed = context.getResources().getString(R.string.space_freed,
                    endingSpace - startingSpace);
            
            Toast.makeText(context, spaceFreed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clean only the uncompressed logs
     */
    public static class CleanUncompressedTask extends AsyncTask<Void, Void, Void>{
        private Context context;
        private double startingSpace;
        private double endingSpace;
        
        public CleanUncompressedTask(Context context){
            this.context = context;
        }
        
        protected Void doInBackground(Void... params) {
            startingSpace = Utils.getStorageFreeSpace();
            String path = Environment.getExternalStorageDirectory().getPath();
            path += "/SysLog/*/";
            //All the log files end in .log, and there are also notes.txt files
            String commands[] = new String[2];
            commands[0] = "rm "+path+"*.log";
            commands[1] = "rm "+path+"*.txt";
            Shell.SH.run(commands);
            endingSpace = Utils.getStorageFreeSpace();
            return null;
        }
        
        protected void onPostExecute(Void param){
            String spaceFreed = context.getResources().getString(R.string.space_freed,
                    endingSpace - startingSpace);
            
            Toast.makeText(context, spaceFreed, Toast.LENGTH_SHORT).show();
        }
    }
}
