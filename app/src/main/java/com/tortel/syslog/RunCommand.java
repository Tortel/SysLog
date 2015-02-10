/* SysLog - A simple logging tool
 * Copyright (C) 2013-2015  Scott Warner <Tortel1210@gmail.com>
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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class that contains all the command details
 */
public class RunCommand implements Parcelable {
	private boolean kernelLog;
	private boolean lastKernelLog;
	private boolean mainLog;
	private boolean eventLog;
	private boolean modemLog;
    private boolean auditLog;
	private boolean root;

    private boolean scrubEnabled;
	
	private String appendText;
	private String notes;
	
	private GrepOption grepOption;
	private String grep;
	
	public boolean grep(){
		return grep != null && !"".equals(grep);
	}
	
	public String getNotes() {
	    String toRet = null;
	    if(notes != null && !"".equals(notes)){
	        toRet = notes;
	    }
	    // Only add the grep notes if grep was actually used
	    if(grep()){
	        toRet = notes == null ? "" : notes;
	        toRet += "\n"+grepOption.toString()+" grepped for "+grep;
	    }
		return toRet;
	}
	
	public void setGrep(String grep) {
        //Need to make sure all quotes are escaped
		grep = grep.trim();
        grep = grep.replace("\"", "\\\"");
		this.grep = grep;
	}
	
	public String getDebugString(){
		StringBuilder builder = new StringBuilder();
		builder.append("Command information:");
		builder.append("\nkernelLog: "+kernelLog);
		builder.append("\nlastKernelLog: "+lastKernelLog);
		builder.append("\nmainLog: "+mainLog);
		builder.append("\neventLog: "+eventLog);
		builder.append("\nmodemLog: "+modemLog);
        builder.append("\nauditLog: "+auditLog);
		builder.append("\ngrepOption: "+grepOption.toString());
		builder.append("\ngrep: "+grep);
		builder.append("\nfileAppendText: "+appendText);
		builder.append("\nnotes: "+notes);
		
		return builder.toString();
	}
	
	/**
	 * Strips all invalid characters from a string,
	 * for use in filenames. It replaces invalid characters
	 * with '_'
	 * @param name the string to clean
	 * @return the clean string
	 */
	private String cleanFileName(String name){
		return name.replaceAll("[^a-zA-Z0-9.-]", "_");
	}
	
	public boolean isKernelLog() {
		return kernelLog;
	}
	public void setKernelLog(boolean kernelLog) {
		this.kernelLog = kernelLog;
	}
	public boolean isLastKernelLog() {
		return lastKernelLog;
	}
	public void setLastKernelLog(boolean lastKernelLog) {
		this.lastKernelLog = lastKernelLog;
	}
	public boolean isMainLog() {
		return mainLog;
	}
	public void setMainLog(boolean mainLog) {
		this.mainLog = mainLog;
	}
	public boolean isModemLog() {
		return modemLog;
	}
	public void setModemLog(boolean radioLog) {
		this.modemLog = radioLog;
	}
	public String getAppendText() {
		return appendText;
	}
	public void setAppendText(String appendText) {
		this.appendText = cleanFileName(appendText.trim());
	}
	public void setNotes(String notes) {
		this.notes = notes.trim();
	}
	public GrepOption getGrepOption() {
		return grepOption;
	}
	public void setGrepOption(GrepOption grepOption) {
		this.grepOption = grepOption;
	}
	public String getGrep() {
		return grep;
	}
    public boolean hasRoot() {
        return root;
    }
    public void setRoot(boolean root) {
        this.root = root;
    }
    public boolean isEventLog() {
        return eventLog;
    }
    public void setEventLog(boolean eventLog) {
        this.eventLog = eventLog;
    }
    public boolean isAuditLog() {
        return auditLog;
    }
    public void setAuditLog(boolean auditLog) {
        this.auditLog = auditLog;
    }
    public boolean isScrubEnabled() {
        return scrubEnabled;
    }
    public void setScrubEnabled(boolean scrubEnabled) {
        this.scrubEnabled = scrubEnabled;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(kernelLog ? (byte) 1 : (byte) 0);
        dest.writeByte(lastKernelLog ? (byte) 1 : (byte) 0);
        dest.writeByte(mainLog ? (byte) 1 : (byte) 0);
        dest.writeByte(eventLog ? (byte) 1 : (byte) 0);
        dest.writeByte(modemLog ? (byte) 1 : (byte) 0);
        dest.writeByte(auditLog ? (byte) 1 : (byte) 0);
        dest.writeByte(root ? (byte) 1 : (byte) 0);
        dest.writeByte(scrubEnabled ? (byte) 1 : (byte) 0);
        dest.writeString(this.appendText);
        dest.writeString(this.notes);
        dest.writeInt(this.grepOption == null ? -1 : this.grepOption.ordinal());
        dest.writeString(this.grep);
    }

    public RunCommand() {
    }

    private RunCommand(Parcel in) {
        this.kernelLog = in.readByte() != 0;
        this.lastKernelLog = in.readByte() != 0;
        this.mainLog = in.readByte() != 0;
        this.eventLog = in.readByte() != 0;
        this.modemLog = in.readByte() != 0;
        this.auditLog = in.readByte() != 0;
        this.root = in.readByte() != 0;
        this.scrubEnabled = in.readByte() != 0;
        this.appendText = in.readString();
        this.notes = in.readString();
        int tmpGrepOption = in.readInt();
        this.grepOption = tmpGrepOption == -1 ? null : GrepOption.values()[tmpGrepOption];
        this.grep = in.readString();
    }

    public static final Parcelable.Creator<RunCommand> CREATOR = new Parcelable.Creator<RunCommand>() {
        public RunCommand createFromParcel(Parcel source) {
            return new RunCommand(source);
        }

        public RunCommand[] newArray(int size) {
            return new RunCommand[size];
        }
    };
}
