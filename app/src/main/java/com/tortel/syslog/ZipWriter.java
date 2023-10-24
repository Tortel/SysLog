/* SysLog - A simple logging tool
 * Copyright (C) 2013-2023 Scott Warner <Tortel1210@gmail.com>
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

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.utils.FileUtils;

import android.content.Context;
import android.util.Log;

public class ZipWriter {
	private ZipOutputStream mZipWriter;
	private File[] mFiles;
	private Context mContext;
	
	public ZipWriter(Context context, String logPath, String zipName) throws FileNotFoundException, NoFilesException {
	    mContext = context;
		//Get the folder
		File logDir = new File(logPath);
		mFiles = logDir.listFiles();
		if (mFiles == null || mFiles.length == 0) {
			Log.e("SysLog", "Error - no files to zip.");
			throw new NoFilesException();
		}
		for(File cur: mFiles){
			Log.v("SysLog", "File to be zipped: "+cur.getPath());
		}
		
		mZipWriter = new ZipOutputStream(new FileOutputStream(FileUtils.getZipPath(context) +
                "/" + zipName));
	}
	
    public void createZip() throws IOException, LowSpaceException {
        try {
            for (File cur : mFiles) {
                // Make sure we aren't adding the zip into its self
                if (cur.getName().endsWith(".zip")) {
                    continue;
                }

                Log.v("SysLog", "Adding " + cur.getName() + " to zip");
                // Zip it
                BufferedInputStream reader = new BufferedInputStream(new FileInputStream(cur));
                ZipEntry entry = new ZipEntry(cur.getName());
                entry.setSize(cur.length());

                mZipWriter.putNextEntry(entry);

                int length;
                byte[] buffer = new byte[10240];

                while ((length = reader.read(buffer)) != -1) {
                    mZipWriter.write(buffer, 0, length);
                }

                // Clean up
                mZipWriter.closeEntry();

                reader.close();
            }

            try {
                mZipWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            
            // Make sure that the IOException wasn't caused by running out of space
            double freeSpace = FileUtils.getStorageFreeSpace(mContext);
            if(freeSpace <= 1){
                throw new LowSpaceException(freeSpace);
            }
            
            // Nope, throw it on up
            throw e;
        }
    }
	
}
