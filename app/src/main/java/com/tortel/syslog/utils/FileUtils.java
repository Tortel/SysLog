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
    private static final String LOG_DIR = "/logs/";
    private static final String ZIP_DIR = "/compressed/";

    /**
     * Minimum amount of free space needed to not throw a LowSpaceException.
     */
    static final double MIN_FREE_SPACE = 10;

    /**
     * Gets the free space of the primary storage, in MB
     * @return the space
     */
    public static double getStorageFreeSpace(Context context){
        StatFs stat = new StatFs(getRootLogDir(context).getPath());
        double sdAvailSize = (double)stat.getAvailableBlocksLong()
                * (double)stat.getBlockSizeLong();
        return Math.floor(sdAvailSize / MB_TO_BYTE);
    }

    /**
     * Return the root log working directory.
     * @return the working directory. This is a directory that will always exist
     */
    public static @NonNull File getRootLogDir(Context context) {
        File logDir = new File(context.getCacheDir().getAbsolutePath() + LOG_DIR);
        if (!logDir.isDirectory()) {
            logDir.mkdir();
        }
        return logDir;
    }

    /**
     * Get the directory with all of the compressed logs
     * @return the compressed logs directory. This will always exist
     */
    public static @NonNull File getZipDir(Context context) {
        File zipDir = new File(context.getCacheDir().getAbsolutePath() + ZIP_DIR);
        // Make sure the directory exists
        if (!zipDir.isDirectory()) {
            zipDir.mkdir();
        }
        return zipDir;
    }

    /**
     * Get the path to the directory with all of the compressed logs
     */
    public static @NonNull String getZipPath(Context context) {
        return getZipDir(context).getAbsolutePath();
    }

}
