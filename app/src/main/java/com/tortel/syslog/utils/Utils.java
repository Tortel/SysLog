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

    /**
     * Check if there is an app available to handle the provided intent
     * @param ctx
     * @param intent
     * @return
     */
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
