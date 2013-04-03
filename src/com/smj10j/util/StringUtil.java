package com.smj10j.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Random;

import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;

public abstract class StringUtil {
	
	private static Logger logger = Logger.getLogger(StringUtil.class);

	public static boolean isNullOrEmpty(String str) {
		return str == null || str.equals("");
	}
	
	public static String toAsciiFromUTF8(String original) {
		String s1a= original.replaceAll("\n", "_-NL-_");	//preserves newlines
		String s1b= s1a.replaceAll("\t", "_-TAB-_");		//preserves tabs
		String s2 = s1b.replaceAll("\\p{C}", "?");			//remove unicode control chars
		String s3 = s2.replaceAll("\\?.{1}\\?", "");		//remove ?X? remnants
		String s4 = s3.replaceAll("\\?|\\!", "");			//remove ? and ! remnants
		String s5a = s4.replaceAll("_-NL-_", "\n");			//restore newlines
		String s5b = s5a.replaceAll("_-TAB-_", "\t");		//restore tabs
		return s5b;
	}
	
	public static String normalizeUnicodeToAscii(String original) throws FatalException {
	    String normalized = Normalizer.normalize(original, Normalizer.Form.NFKD);
	    String regex = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";

	    try {
			String ascii = new String(normalized.replaceAll(regex, "").getBytes("ascii"), "ascii");
		    return ascii;
		} catch (UnsupportedEncodingException e) {
			throw new FatalException(e);
		}	    
	}
	
	public static String utf8Encode(String str) {
		try {
			str = URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
		return str;
	}
	
	public static boolean equalsIgnoreCase(String str1, String str2) {
		if(str1 == null) {
			if(str2 == null)
				return true;
			return false;
		}else if(str2 == null) {
			return false;
		}else {
			return str1.equalsIgnoreCase(str2);
		}
	}
	
	public static boolean equals(String str1, String str2) {
		if(str1 == null) {
			if(str2 == null)
				return true;
			return false;
		}else if(str2 == null) {
			return false;
		}else {
			return str1.equals(str2);
		}
	}
	
	public static String convertStreamToString(InputStream is)
            throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8")
                );
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {        
            return "";
        }
    }
	
	private static Random rnd = new Random();
	public static String getRandomNumber(int length) {
	    StringBuilder sb = new StringBuilder(length);
	    for(int i=0; i < length; i++)
	        sb.append((char)('0' + rnd.nextInt(10)));
	    return sb.toString();
	}
}
