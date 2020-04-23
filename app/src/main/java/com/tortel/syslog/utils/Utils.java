/* SysLog - A simple logging tool
 * Copyright (C) 2013-2020 Scott Warner <Tortel1210@gmail.com>
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
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.tortel.syslog.R;
import com.tortel.syslog.dialog.RunningDialog;

import org.greenrobot.eventbus.EventBus;

import eu.chainfire.libsuperuser.Shell;

/**
 * General utilities and the main run command code
 */
public class Utils {
    public static final String LAST_KMSG = "/proc/last_kmsg";
    static final String AUDIT_LOG = "/data/misc/audit/audit.log";
    static final String AUDIT_OLD_LOG = "/data/misc/audit/audit.old";
    static final String PSTORE_CONSOLE = "/sys/fs/pstore/console-ramoops*";
    static final String PSTORE_DEVINFO = "/sys/fs/pstore/device*ramoops*";

    static final String PRESCRUB = "-prescrub";

    /**
     * Runs the log file through the ScrubberUtils and removes the PRESCRUB extension
     * @param context context
     * @param path where to look for files
     * @param disable flag to simply rename the files to their final names
     */
    static void scrubFiles(Context context, String path, boolean disable) {
        // Send a progress update
        RunningDialog.ProgressUpdate update = new RunningDialog.ProgressUpdate();
        update.messageResource = R.string.scrubbing_logs;
        EventBus.getDefault().post(update);

        File logFolder = new File(path);
        File[] logFiles = logFolder.listFiles();
        for(File file : logFiles){
            // Save it to a file without the PRESCRUB extension
            File outFile = new File(path + "/" + file.getName().replace(PRESCRUB, ""));
            if(disable){
                Log.d("Scrub disabled, renaming "+file.getName()+" to "+outFile.getName());
                file.renameTo(outFile);
            } else {
                Log.d("Scrubbing " + file.getName() + " to " + outFile.getName());

                try {
                    ScrubberUtils.scrubFile(context, file, outFile);
                    file.delete();
                } catch (IOException e) {
                    Log.e("Exception scrubbing file " + file.getName(), e);
                }
            }
        }
    }

    /**
     * Check if there is an app available to handle the provided intent
     */
    public static boolean isHandlerAvailable(Context ctx, Intent intent) {
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, 
              PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
     }

    /**
     * Send a request to clear the logcat buffer (logcat -c)
     */
     public static void clearLogcatBuffer(@NonNull final Context context) {
         (new Thread() {
            @Override
            public void run() {
                if (Shell.SU.available()) {
                    Shell.SU.run("logcat -c");
                } else {
                    Shell.SH.run("logcat -c");
                }
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, R.string.buffer_cleared, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
     }

    /**
     * Clean all the saved log files
     */
     public static void cleanAll(@NonNull final Context context) {
         (new Thread() {
             @Override
             public void run() {
                 final double startingSpace = FileUtils.getStorageFreeSpace(context);
                 String path = FileUtils.getRootLogDir(context).getPath();
                 path += "/*";
                 Shell.SH.run("rm -rf " + path);
                 final double endingSpace = FileUtils.getStorageFreeSpace(context);
                 Handler mainHandler = new Handler(context.getMainLooper());
                 mainHandler.post(() -> {
                     Toast.makeText(context, context.getResources().getString(R.string.space_freed,
                             endingSpace - startingSpace), Toast.LENGTH_SHORT).show();
                 });
             }
         }).start();
     }
}
