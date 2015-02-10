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

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.tortel.syslog.exception.LowSpaceException;
import com.tortel.syslog.exception.NoFilesException;
import com.tortel.syslog.utils.Utils;

import android.util.Log;

public class ZipWriter {
	private String outPath;
	private ZipOutputStream zWriter;
	private File[] files;
	
	public ZipWriter(String path, String zName) throws FileNotFoundException, NoFilesException{
		outPath = path;
		
		//Get the folder
		File outFolder = new File(outPath);
		files = outFolder.listFiles();
		if(files == null || files.length == 0){
			Log.e("SysLog", "Error - no files to zip.");
			throw new NoFilesException();
		}
		for(File cur: files){
			Log.v("SysLog", "File to be zipped: "+cur.getPath());
		}
		
		zWriter = new ZipOutputStream(new FileOutputStream(outPath+zName));
	}
	
    public void createZip() throws IOException, LowSpaceException {
        try {
            for (int i = 0; i < files.length; i++) {
                File cur = files[i];

                // Make sure we aren't adding the zip into its self
                if (cur.getName().endsWith(".zip")) {
                    continue;
                }

                Log.v("SysLog", "Adding " + cur.getName() + " to zip");
                // Zip it
                BufferedInputStream reader = new BufferedInputStream(new FileInputStream(cur));
                ZipEntry entry = new ZipEntry(cur.getName());
                entry.setSize(cur.length());

                zWriter.putNextEntry(entry);

                int length;
                byte[] buffer = new byte[10240];

                while ((length = reader.read(buffer)) != -1) {
                    zWriter.write(buffer, 0, length);
                }

                // Clean up
                zWriter.closeEntry();

                reader.close();
            }

            try {
                zWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            
            // Make sure that the IOException wasn't caused by running out of space
            double freeSpace = Utils.getStorageFreeSpace();
            if(freeSpace <= 1){
                throw new LowSpaceException(freeSpace);
            }
            
            // Nope, throw it on up
            throw e;
        }
    }
	
}
