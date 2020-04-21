/* SysLog - A simple logging tool
 * Copyright (C) 2016-2020  Scott Warner <Tortel1210@gmail.com>
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

import android.content.Context;

import com.tortel.syslog.GrepOption;
import com.tortel.syslog.R;
import com.tortel.syslog.Result;
import com.tortel.syslog.RunCommand;
import com.tortel.syslog.ZipWriter;
import com.tortel.syslog.dialog.RunningDialog;
import com.tortel.syslog.exception.CreateFolderException;
import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.exception.RunCommandException;

import org.greenrobot.eventbus.EventBus;

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

import eu.chainfire.libsuperuser.Shell;

/**
 * Thread for grabbing the logs via a {@link RunCommand}
 */
public class GrabLogThread implements Runnable {
    // Flag is a thread is running
    private static boolean isRunning;

    private RunCommand mCommand;
    private Result mResult;
    private Context mContext;
    private String workingPath;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm", Locale.US);
    private Date date = new Date();

    public GrabLogThread(RunCommand command, Context context){
        mCommand = command;
        mResult = new Result(false);
        mResult.setCommand(command);
        mContext = context.getApplicationContext();
    }

    /**
     * Grab the logs in a background thread
     */
    @Override
    public void run() {
        isRunning = true;

        try {
            runCommand();
        } catch (Exception e) {
            Log.e("Exception while getting logs", e);
            // Mark the thread as no longer running
            isRunning = false;
            // Post the result
            mResult.setSuccess(false);
            mResult.setException(e);
            EventBus.getDefault().post(mResult);
        }

        Log.v("Done grabbing logs");
    }

    /**
     * Runs the {@link com.tortel.syslog.RunCommand} contained within the {@link com.tortel.syslog.Result} passed in.<br />
     * On an error
     * @throws CreateFolderException
     * @throws RunCommandException
     * @throws IOException
     * @throws NoFilesException
     * @throws LowSpaceException
     */
    private void runCommand() throws CreateFolderException,
            RunCommandException, IOException, NoFilesException, LowSpaceException {

        //Check how much space is on the primary storage
        double freeSpace = FileUtils.getStorageFreeSpace(mContext);
        if(freeSpace < FileUtils.MIN_FREE_SPACE){
            throw new LowSpaceException(freeSpace);
        }

        // Commands to execute
        List<String> commands = new LinkedList<>();
        // Where to write the command output
        List<String> files = new LinkedList<>();

        // Create the directories
        workingPath = FileUtils.getRootLogDir(mContext).getPath();

        workingPath += "/" + sdf.format(date)+"/";
        File outDirectory = new File(workingPath);
        // Check if this path already exists (Happens if you run this multiple times a minute
        if(outDirectory.exists()){
            // Append the seconds
            workingPath =  workingPath.substring(0, workingPath.length()-1) +"."+ Calendar.getInstance().get(Calendar.SECOND)+"/";
            outDirectory = new File(workingPath);
            Log.v("Path already exists, added seconds");
        }

        Log.v("Path: " + workingPath);
        // Make the directory
        if (!outDirectory.mkdirs() || !outDirectory.isDirectory()) {
            throw new CreateFolderException();
        }

        // Commands to dump the logs
        if (mCommand.isMainLog()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.MAIN
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                commands.add("logcat -v time -d | grep \"" + mCommand.getGrep() + "\"");
            } else {
                commands.add("logcat -v time -d");
            }
            files.add(workingPath + "/logcat.log" + Utils.PRESCRUB);
        }
        if (mCommand.isEventLog()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.EVENT
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                commands.add("logcat -b events -v time -d | grep \"" + mCommand.getGrep() + "\"");
            } else {
                commands.add("logcat -b events -v time -d");
            }
            files.add(workingPath + "/event.log" + Utils.PRESCRUB);
        }
        if (mCommand.isKernelLog()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.KERNEL
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                commands.add("dmesg | grep \"" + mCommand.getGrep() + "\"");
            } else {
                commands.add("dmesg");
            }
            files.add(workingPath + "/dmesg.log" + Utils.PRESCRUB);
        }
        if (mCommand.isPstore()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.KERNEL
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                commands.add("cat " + Utils.PSTORE_CONSOLE + "| grep \"" + mCommand.getGrep() + "\"");
                commands.add("cat " + Utils.PSTORE_DEVINFO + "| grep \"" + mCommand.getGrep() + "\"");
            } else {
                commands.add("cat " + Utils.PSTORE_CONSOLE);
                commands.add("cat " + Utils.PSTORE_DEVINFO);
            }
            files.add(workingPath + "/pstore_console" + Utils.PRESCRUB);
            files.add(workingPath + "/pstore_devinfo" + Utils.PRESCRUB);
        }
        if (mCommand.isModemLog()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.MODEM
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                commands.add("logcat -v time -b radio -d | grep \"" + mCommand.getGrep() + "\"");
            } else {
                commands.add("logcat -v time -b radio -d");
            }
            files.add(workingPath + "/modem.log" + Utils.PRESCRUB);
        }
        if (mCommand.isLastKernelLog()) {
            if (mCommand.grep() && mCommand.getGrepOption() == GrepOption.LAST_KERNEL
                    || mCommand.getGrepOption() == GrepOption.ALL) {
                //Log should be run through grep
                commands.add("cat "+Utils.LAST_KMSG+" | grep \"" + mCommand.getGrep() + "\"");
            } else {
                //Try copying the last_kmsg over
                commands.add("cat "+Utils.LAST_KMSG);
            }
            files.add(workingPath + "/last_kmsg.log" + Utils.PRESCRUB);
        }
        if (mCommand.isAuditLog()) {
            commands.add("cat " + Utils.AUDIT_LOG);
            files.add(workingPath + "/audit.log");
            commands.add("cat " + Utils.AUDIT_OLD_LOG);
            files.add(workingPath + "/audit.old");
        }

        Shell.Builder builder = new Shell.Builder();
        //Run the commands
        if (mCommand.hasRoot()) {
            builder.useSU();
        } else {
            builder.useSH();
        }

        // Send an update
        RunningDialog.ProgressUpdate update = new RunningDialog.ProgressUpdate();
        update.messageResource = R.string.getting_logs;
        EventBus.getDefault().post(update);

        final Shell.Interactive shell = builder.open();
        runCommands(shell, commands, files);
    }

    /**
     * Called when the commands are complete
     * @param success
     */
    private void commandsComplete(boolean success) {
        if (!success) {
            mResult.setSuccess(false);
            mResult.setMessage(R.string.error);
            postResult();
            return;
        }

        // Scrub the files
        Utils.scrubFiles(mContext, workingPath, !mCommand.isScrubEnabled());

        //If there are notes, write them to a notes file
        if (mCommand.getNotes() != null && mCommand.getNotes().length() > 0) {
            try {
                File noteFile = new File(workingPath + "/notes.txt");
                FileWriter writer = new FileWriter(noteFile);
                writer.write(mCommand.getNotes());
                writer.close();
            } catch (Exception e) {
                //Ignore
            }
        }

        // Append the users input into the zip
        String archiveName;
        if (mCommand.getAppendText().length() > 0) {
            archiveName = sdf.format(date) + "-" + mCommand.getAppendText() + ".zip";
        } else {
            archiveName = sdf.format(date) + ".zip";
        }
        try {
            // Send a progress update
            RunningDialog.ProgressUpdate update = new RunningDialog.ProgressUpdate();
            update.messageResource = R.string.compressing_logs;
            EventBus.getDefault().post(update);

            ZipWriter writer = new ZipWriter(mContext, workingPath, archiveName);
            mResult.setArchiveName(archiveName);

            writer.createZip();
            mResult.setSuccess(true);
        } catch (Exception e) {
            Log.e("Exception creating zip", e);
            mResult.setSuccess(false);
            mResult.setMessage(R.string.error);
        }
        postResult();
    }

    /**
     * Publish the result
     */
    private void postResult(){
        Log.v("Done getting logs");
        // Mark the thread as no longer running
        isRunning = false;

        // Post the result
        EventBus.getDefault().post(mResult);
    }

    /**
     * Run the commands provided, and write the output to the path provided
     * @param shell
     * @param commands
     * @param outputFiles
     */
    private void runCommands(final Shell.Interactive shell, final List<String> commands, final List<String> outputFiles){
        if(commands.size() == 0){
            commandsComplete(true);
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
                        Log.e("Exception closing writer", e);
                    }
                    runCommands(shell, commands, outputFiles);
                }

                @Override
                public void onLine(String line) {
                    try {
                        // Make sure to include a newline
                        output.write(line+"\n");
                    } catch (IOException e) {
                        Log.e("Exception writing line", e);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            commandsComplete(false);
        }
    }

    /**
     * Check if a thread is currently running
     * @return
     */
    public static boolean isRunning(){
        return isRunning;
    }
}
