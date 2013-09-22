/* SysLog - A simple logging tool
 * Copyright (C) 2013  Scott Warner <Tortel1210@gmail.com>
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

import java.util.ArrayList;
import java.util.List;

/**
 * A class to contain various information about the
 * status of running
 *
 */
public class Result {
	private boolean success;
	private List<Throwable> exceptions;
	private List<Integer> messages;
	
	public Result(){
	    exceptions = new ArrayList<Throwable>();
	    messages = new ArrayList<Integer>();
	}
	
	public Result(boolean success){
	    this();
		this.success = success;
	}
	
	public boolean success(){
		return success;
	}
	
	public void setSuccess(boolean success){
		this.success = success;
	}
	
	public void addException(Throwable exception){
	    this.success = false;
		exceptions.add(exception);
	}
	
	public List<Throwable> getExceptions(){
		return exceptions;
	}
	
	public void addMessage(int message){
		messages.add(message);
	}

	public List<Integer> getMessages(){
		return messages;
	}
}
