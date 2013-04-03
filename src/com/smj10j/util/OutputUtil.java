package com.smj10j.util;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.smj10j.servlet.GatewayServlet;

public abstract class OutputUtil {
	
	private static Logger logger = Logger.getLogger(OutputUtil.class);

	public static <T> String tokenReplace(String str, String tokenString, List<T> tokens) {
		String newStr = str;
		if(StringUtil.isNullOrEmpty(str) || tokens == null)
			return newStr;
		
		for(T token : tokens) {
			String replacement =  token == null ? "null" : token.toString();
			newStr = newStr.replaceFirst(tokenString, Matcher.quoteReplacement(replacement));
		}
		
		return newStr;
	}
	
	public static String getElapsedString() {
		if(GatewayServlet.threadToStartTimestamp != null) {	//expects to be run within the GatewayServlet context
			Long threadStartTs = GatewayServlet.threadToStartTimestamp.get(Thread.currentThread().getId());
			if(threadStartTs != null) {
				return " - Thread " + Thread.currentThread().getId() + ": " + (System.currentTimeMillis() - threadStartTs) + "ms elapsed";
			}
		}
		return "";
	}
	
	public static double round(double number, int places) {
		String array[] = new String[places];
	    Arrays.fill(array, "#");
		DecimalFormat twoDForm = new DecimalFormat("#." + ArrayUtil.implode(array, ""));
		return Double.valueOf(twoDForm.format(number));
	}
}
