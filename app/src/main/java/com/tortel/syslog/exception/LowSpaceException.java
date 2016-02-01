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
package com.tortel.syslog.exception;

/**
 * And exception for when there is low free space on the primary storage
 */
public class LowSpaceException extends Exception {
    private static final long serialVersionUID = 4701987157679370945L;
    private double size;
    
    public LowSpaceException(double size){
        this.size = size;
    }
    
    /**
     * Get the free space (In MB)
     * @return
     */
    public double getFreeSpace(){
        return size;
    }
}
