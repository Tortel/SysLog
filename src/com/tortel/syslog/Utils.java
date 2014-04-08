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
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

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
    
    private static final int MB_TO_BYTE = 1048576;
    
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
     * Runs the {@link RunCommand} contained within the {@link Result} passed in.<br />
     * On an error
     * @param result
     * @throws CreateFolderException
     * @throws RunCommandException
     * @throws IOException
     * @throws NoFilesException
     * @throws LowSpaceException 
     */
    public static void runCommand(Result result) throws CreateFolderException, RunCommandException, IOException, NoFilesException, LowSpaceException{
        RunCommand command = result.getCommand();
        String archivePath;
        
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //Check how much space is on the primary storage
            double freeSpace = getStorageFreeSpace();
            if(freeSpace < 7.0){
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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                rootPath = path.replaceAll("/storage/emulated/", "/data/media/");
                Log.v(TAG, "Using path "+rootPath+" for root commands");
            }

            //Commands to dump the logs
            if(command.isMainLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MAIN
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -d | grep \""+command.getGrep()+"\" > "+rootPath+"logcat.log");
                } else {
                    commands.add("logcat -v time -d -f "+rootPath+"logcat.log");
                }
            }
            if(command.isKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("dmesg | grep \""+command.getGrep()+"\" > "+rootPath+"dmesg.log");
                } else {
                    commands.add("dmesg > "+rootPath+"dmesg.log");
                }
            }
            if(command.isModemLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.MODEM
                        || command.getGrepOption() == GrepOption.ALL){
                    commands.add("logcat -v time -b radio -d | grep \""+command.getGrep()+"\" > "+rootPath+"modem.log");
                } else {
                    commands.add("logcat -v time -b radio -d -f "+rootPath+"modem.log");
                }
            }
            if(command.isLastKernelLog()){
                if(command.grep() && command.getGrepOption() == GrepOption.LAST_KERNEL
                        || command.getGrepOption() == GrepOption.ALL){
                    //Log should be run through grep
                    commands.add("cat "+LAST_KMSG+" | grep \""+command.getGrep()+"\" > "+rootPath+"last_kmsg.log");
                } else {
                    //Try copying the last_kmsg over
                    commands.add("cp "+LAST_KMSG+" "+rootPath+"last_kmsg.log");
                }
            }

            /*
             * More 4.3+ SU issues - need to chown to media_rw
             */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
                // Some kernels/systems may not work properly with /*
                // List the files explicitly
                commands.add("chown media_rw:media_rw "+rootPath+"/logcat.log");
                commands.add("chown media_rw:media_rw "+rootPath+"/dmesg.log");
                commands.add("chown media_rw:media_rw "+rootPath+"/modem.log");
                commands.add("chown media_rw:media_rw "+rootPath+"/last_kmsg.log");
                // Some Omni-based ROMs/kernels have issues even with the above
                // When in doubt, overkill it
                commands.add("chmod 666 "+rootPath+"/logcat.log");
                commands.add("chmod 666 "+rootPath+"/dmesg.log");
                commands.add("chmod 666 "+rootPath+"/modem.log");
                commands.add("chmod 666 "+rootPath+"/last_kmsg.log");
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

}
