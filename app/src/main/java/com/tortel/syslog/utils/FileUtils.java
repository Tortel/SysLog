/* SysLog - A simple logging tool
 * Copyright (C) 2020 Scott Warner <Tortel1210@gmail.com>
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
import android.os.StatFs;

import androidx.annotation.NonNull;

import java.io.File;

public class FileUtils {
    private static final int MB_TO_BYTE = 1048576;

    /**
     * Minimum amount of free space needed to not throw a LowSpaceException.
     */
    public static final double MIN_FREE_SPACE = 10;

    /**
     * Gets the free space of the primary storage, in MB
     * @return the space
     */
    public static double getStorageFreeSpace(Context context){
        StatFs stat = new StatFs(getRootLogDir(context).getPath());
        double sdAvailSize = (double)stat.getAvailableBlocks()
                * (double)stat.getBlockSize();
        return Math.floor(sdAvailSize / MB_TO_BYTE);
    }

    /**
     * Gets the free space of the volume containing the specified path, in MB
     * @return the space
     */
    public static double getStorageFreeSpace(String path){
        StatFs stat = new StatFs(path);
        double sdAvailSize = (double)stat.getAvailableBlocks()
                * (double)stat.getBlockSize();
        return Math.floor(sdAvailSize / MB_TO_BYTE);
    }

    public static @NonNull File getRootLogDir(Context context) {
        return context.getCacheDir();
    }


}
