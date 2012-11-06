package com.tortel.syslog;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.util.Log;

public class ZipWriter {
	private String outPath;
	private ZipOutputStream zWriter;
	private File[] files;
	
	public ZipWriter(String path, String zName){
		outPath = path;
		
		//Get the folder
		File outFolder = new File(outPath);
		files = outFolder.listFiles();
		for(File cur: files){
			Log.v("SysLog", "File to be zipped: "+cur.getPath());
		}
		try {
			zWriter = new ZipOutputStream(new FileOutputStream(outPath+zName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void createZip(){
		for(int i=0; i < files.length; i++){
			File cur = files[i];

			try{
				Log.v("SysLog", "Adding "+cur.getName()+" to zip");
				//Zip it
				BufferedInputStream reader = new BufferedInputStream(new FileInputStream(cur));
				ZipEntry entry = new ZipEntry(cur.getName());
				entry.setSize(cur.length());
				
				zWriter.putNextEntry(entry);
				
				int length;
				byte[] buffer = new byte[10240];
				
				while( (length = reader.read(buffer)) != -1 ){
					zWriter.write(buffer, 0, length);
				}
				
				//Clean up
				zWriter.closeEntry();
				
				reader.close();
			} catch(IOException e){
				e.printStackTrace();
				}
		}
		
		try {
			zWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
