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
package com.tortel.syslog;

import android.os.Environment;

import androidx.annotation.StringRes;

import com.tortel.syslog.utils.Utils;

/**
 * A class to contain various information about the
 * status of running
 */
public class Result {
	private boolean success;
	private Throwable exceptions;
	private int message;
	private RunCommand command;
	private String archivePath;
	
	public Result(boolean success){
		this.success = success;
	}
	
	/**
     * Returns if exception reporting should be disabled.
     * On Android 4.3+, you need to run it with root
     * @return
     */
    public boolean disableReporting(){
        return !command.hasRoot();
    }
	
	public boolean success(){
		return success;
	}
	
	public void setSuccess(boolean success){
		this.success = success;
	}
	
	public void setException(Throwable exception){
	    this.success = false;
		this.exceptions = exception;
	}
	
	public Throwable getException(){
		return exceptions;
	}
	
	public void setMessage(@StringRes int message){
		this.message= message; 
	}

	@StringRes
	public int getMessage(){
		return message;
	}

	public RunCommand getCommand() {
		return command;
	}

	public void setCommand(RunCommand command) {
		this.command = command;
	}

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

}
