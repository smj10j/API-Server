package com.smj10j.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import com.smj10j.conf.FatalException;

public abstract class FileUtil {
   
	public static String readFile(String filename) throws FatalException {
    	return readFile(new File(filename));
    }
	
	public static String readFile(File file) throws FatalException {
    	return readFile(file, null, null);
    }
    
    public static String readFile(File file, String fromEncoding, String toEncoding) throws FatalException {	

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);

			byte fileContent[] = new byte[(int)file.length()];
			fstream.read(fileContent);
			
			String origDataStr = null;
			if(fromEncoding != null) {
				origDataStr = new String(fileContent, fromEncoding);
			}else {
				origDataStr = new String(fileContent);				
			}
			
			if(toEncoding == null) {
				//no new encoding specified, use the file's encoding
				return origDataStr;
			}else if(fromEncoding == null || !fromEncoding.equals(toEncoding)) {
				//new encoding specified, convert from the file's encoding to the new encoding
				String convertedDataStr = new String(origDataStr.getBytes(), toEncoding);
				return convertedDataStr;
			}else {
				//from encoding and to encoding are equal - return the file
				return origDataStr;
			}
			
		} catch (FileNotFoundException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
			try {
				fstream.close();
			} catch (IOException e) {
				throw new FatalException(e);
			}
		}
    }
    
    public static File createTempFile(String prefix, String suffix) throws FatalException {		
		try {
			File file = File.createTempFile(prefix, suffix);
			file.setWritable(true, false);
			return file;
		} catch (IOException e) {
			throw new FatalException(e);
		}
    }
    
    public static File writeTempFile(String data, String prefix, String suffix) throws FatalException {		
		File file = createTempFile(prefix, suffix);
		return writeFile(file, data);
    }
    
    public static File writeFile(String filename, String data) throws FatalException {
		File file = new File(filename);
		return writeFile(file, data);
    }
    
    
    public static File writeFile(File file, String data) throws FatalException {
		try{
			file.mkdirs();
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(data);
			out.close();
			return file;
		}catch (IOException e){
			throw new FatalException(e);
		}
    }
}
