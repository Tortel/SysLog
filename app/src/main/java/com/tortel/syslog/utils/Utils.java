/* SysLog - A simple logging tool
 * Copyright (C) 2013-2016  Scott Warner <Tortel1210@gmail.com>
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

import java.io.BufferedWriter;
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
    public static final String AUDIT_LOG = "/data/misc/audit/audit.log";
    public static final String AUDIT_OLD_LOG = "/data/misc/audit/audit.old";

    public static final String PRESCRUB = "-prescrub";
    
    private static final int MB_TO_BYTE = 1048576;
    
    /**
     * Minimum amount of free space needed to not throw a LowSpaceException.
     * This is based on the average size of my runs (~5.5MB)
     */
    public static final double MIN_FREE_SPACE = 6;
    
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
     * Returns true if the android version is M, or 6.0+
     * @return
     */
    public static boolean isMarshmallow(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
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
    public static void runCommand(final Context context, final Result result) throws CreateFolderException,
            RunCommandException, IOException, NoFilesException, LowSpaceException {
        final RunCommand command = result.getCommand();
        
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //Check how much space is on the primary storage
            double freeSpace = getStorageFreeSpace();
            if(freeSpace < MIN_FREE_SPACE){
                throw new LowSpaceException(freeSpace);
            }
            
            // Commands to execute
            List<String> commands = new LinkedList<>();
            // Where to write the command output
            List<String> files = new LinkedList<>();

            // Create the directories
            String path = Environment.getExternalStorageDirectory().getPath();

            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm", Locale.US);
            final Date date = new Date();
            File nomedia = new File(path+"/SysLog/.nomedia");
            path += "/SysLog/"+sdf.format(date)+"/";
            File outPath = new File(path);
            // Check if this path already exists (Happens if you run this multiple times a minute
            if(outPath.exists()){
                // Append the seconds
                path =  path.substring(0, path.length()-1) +"."+Calendar.getInstance().get(Calendar.SECOND)+"/";
                outPath = new File(path);
                Log.v(TAG, "Path already exists, added seconds");
            }

            final String finalPath = path;

            Log.v(TAG, "Path: "+path);
            // Make the directory
            if(!outPath.mkdirs() || !outPath.isDirectory()){
                throw new CreateFolderException();
            }

            // Put a .nomedia file in the directory
            if(!nomedia.exists()){
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create .nomedia file", e);
                }
            }

            // Commands to dump the logs
            if(command.isMainLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MAIN
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -d | grep \""+command.getGrep()+"\"");
                } else {
                    commands.add("logcat -v time -d");
                }
                files.add(outPath.getAbsolutePath()+"/logcat.log"+PRESCRUB);
            }
            if(command.isEventLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.EVENT
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -b events -v time -d | grep \""+command.getGrep()+"\"");
                } else {
                    commands.add("logcat -b events -v time -d");
                }
                files.add(outPath.getAbsolutePath()+"/event.log"+PRESCRUB);
            }
            if(command.isKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("dmesg | grep \""+command.getGrep()+"\"");
                } else {
                    commands.add("dmesg");
                }
                files.add(outPath.getAbsolutePath()+"/dmesg.log"+PRESCRUB);
            }
            if(command.isModemLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MODEM
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -b radio -d | grep \""+command.getGrep()+"\"");
                } else {
                    commands.add("logcat -v time -b radio -d");
                }
                files.add(outPath.getAbsolutePath()+"/modem.log"+PRESCRUB);
            }
            if(command.isLastKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.LAST_KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    //Log should be run through grep
                    commands.add("cat "+LAST_KMSG+" | grep \""+command.getGrep()+"\"");
                } else {
                    //Try copying the last_kmsg over
                    commands.add("cat "+LAST_KMSG);
                }
                files.add(outPath.getAbsolutePath()+"/last_kmsg.log"+PRESCRUB);
            }
            if(command.isAuditLog()){
                commands.add("cat "+AUDIT_LOG);
                files.add(outPath.getAbsolutePath()+"/audit.log");
                commands.add("cat "+AUDIT_OLD_LOG);
                files.add(outPath.getAbsolutePath()+"/audit.old");
            }

            Shell.Builder builder = new Shell.Builder();

            //Run the commands
            if(command.hasRoot()){
                builder.useSU();
            } else {
                builder.useSH();
            }

            Shell.Interactive shell = builder.open();
            runComamnds(shell, commands, files, new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    // Scrub the files
                    scrubFiles(context, finalPath, !command.isScrubEnabled());

                    //If there are notes, write them to a notes file
                    if(command.getNotes() != null && command.getNotes().length() > 0){
                        try{
                            File noteFile = new File(finalPath+"/notes.txt");
                            FileWriter writer = new FileWriter(noteFile);
                            writer.write(command.getNotes());
                            writer.close();
                        } catch(Exception e){
                            //Ignore
                        }
                    }

                    // Append the users input into the zip
                    String archivePath;
                    if(command.getAppendText().length() > 0){
                        archivePath = sdf.format(date)+"-"+command.getAppendText()+".zip";
                    } else {
                        archivePath = sdf.format(date)+".zip";
                    }
                    try {
                        ZipWriter writer = new ZipWriter(finalPath, archivePath);
                        archivePath = finalPath + archivePath;
                        result.setArchivePath(archivePath);

                        writer.createZip();
                        result.setSuccess(true);
                    } catch(Exception e){
                        Log.e(TAG, "Exception creating zip", e);
                        result.setSuccess(false);
                        result.setMessage(R.string.error);
                    }
                }
            });
        } else {
            //Default storage not accessible
            result.setSuccess(false);
            result.setMessage(R.string.storage_err);
        }
    }

    private static void runComamnds(final Shell.Interactive shell, final List<String> commands, final List<String> outputFiles, final Shell.OnCommandResultListener callback){
        if(commands.size() == 0){
            callback.onCommandResult(0, 0, null);
            return;
        }
        try {
            String command = commands.remove(0);
            String outputFileName = outputFiles.remove(0);
            File outputFile = new File(outputFileName);
            final BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

            shell.addCommand(command, 0, new Shell.OnCommandLineListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode) {
                    try{
                        output.flush();
                        output.close();
                    } catch(IOException e){
                        Log.e(TAG, "Exception closing writer", e);
                    }
                    runComamnds(shell, commands, outputFiles, callback);
                }

                @Override
                public void onLine(String line) {
                    try {
                        // Make sure to include a newline
                        output.write(line+"\n");
                    } catch (IOException e) {
                        Log.e(TAG, "Exception writing line", e);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            callback.onCommandResult(0, -1, null);
        }
    }

    /**
     * Runs the log file through the ScrubberUtils and removes the PRESCRUB extension
     * @param context
     * @param path where to look for files
     * @param disable flag to simply rename the files to their final names
     */
    public static void scrubFiles(Context context, String path, boolean disable) {
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
     * Task to clear the logcat buffer (logcat -c)
     */
    public static class ClearLogcatBufferTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public ClearLogcatBufferTask(Context context){
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params){
            Shell.SU.run("logcat -c");
            return null;
        }

        @Override
        protected void onPostExecute(Void param){
            Toast.makeText(context, R.string.buffer_cleared, Toast.LENGTH_SHORT).show();
        }

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

        @Override
        protected Void doInBackground(Void... params) {
            startingSpace = Utils.getStorageFreeSpace();
            String path = Environment.getExternalStorageDirectory().getPath();
            path += "/SysLog/*";
            Shell.SH.run("rm -rf "+path);
            endingSpace = Utils.getStorageFreeSpace();
            return null;
        }

        @Override
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

        @Override
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

        @Override
        protected void onPostExecute(Void param){
            String spaceFreed = context.getResources().getString(R.string.space_freed,
                    endingSpace - startingSpace);
            
            Toast.makeText(context, spaceFreed, Toast.LENGTH_SHORT).show();
        }
    }
}
