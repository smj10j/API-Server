package com.smj10j.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.smj10j.conf.Constants;

public abstract class DateUtil {
	
	//private static Logger logger = Logger.getLogger(DateUtil.class);
	
	public static Date getStartOfDay(Date date) {
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
		cal.set(Calendar.MINUTE, 0);                 // set minute in hour
		cal.set(Calendar.SECOND, 0);                 // set second in minute
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime();             // actually computes the new Date
		return zeroedDate;
	}
	
	public static Date getStartOfMonth(Date date) {
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.DAY_OF_MONTH, 1);			 // set to first day of month
		cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
		cal.set(Calendar.MINUTE, 0);                 // set minute in hour
		cal.set(Calendar.SECOND, 0);                 // set second in minute
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime();             // actually computes the new Date
		return zeroedDate;		
	}	
	
	public static Date getStartOfYear(Date date) {
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.MONTH, 0);			 		 // set month to january
		cal.set(Calendar.DAY_OF_MONTH, 1);			 // set to first day of month		
		cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
		cal.set(Calendar.MINUTE, 0);                 // set minute in hour
		cal.set(Calendar.SECOND, 0);                 // set second in minute
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime();             // actually computes the new Date
		return zeroedDate;		
	}
	
	public static Date parse(String value) throws ParseException {
		if(!value.equals("0")) {	//0 is special
			try { 
				return DateFormat.getInstance().parse(value);
			}catch(ParseException e) {
				//try and use some fancy interval parsing
				String temp = value.toUpperCase();
				if(temp.startsWith("NOW")) {
					temp = temp.replace("NOW + ", "");
					Date date = new Date();
					Scanner scanner = new Scanner(temp);
					Integer field = null;
					Integer amount = null;
					String match = null;
					while((match = scanner.findInLine(Pattern.compile("[a-zA-Z0-9]+"))) != null) {
						if(amount == null) {
							amount = Integer.parseInt(match);
						}else if(field == null) {
							if(match.equals("MINUTE")) {
								field = Calendar.MINUTE;
							}else if(match.equals("HOUR")) {
								field = Calendar.HOUR;								
							}else if(match.equals("DAY")) {
								field = Calendar.DATE;								
							}else if(match.equals("WEEK")) {
								field = Calendar.WEEK_OF_YEAR;								
							}else if(match.equals("MONTH")) {
								field = Calendar.MONTH;								
							}else {
								throw e;
							}
						}else {
							throw e;
						}
					}
					
					if(field != null && amount != null) {
						return DateUtil.addToDate(date, field, amount);
					}else {
						throw e;
					}
					
				}else {
					throw e;
				}
			}
		}else {
			return new Date();
		}
	}

	public static Date getDate(long secondsAgo) {
		long startMs = System.currentTimeMillis() - secondsAgo*1000;
		Date date = new Date(startMs);               // timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime();             // actually computes the new Date
		return zeroedDate;
	}
	
	public static Date getTodaysStartDate() {
		return getStartDate(System.currentTimeMillis(),0);
	}
	
	public static Date getStartDate(long timestamp, int daysAgo) {
		long startMs = timestamp - daysAgo*Constants.Time.DAY*1000;
		Date date = new Date(startMs);               // timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
		cal.set(Calendar.MINUTE, 0);                 // set minute in hour
		cal.set(Calendar.SECOND, 0);                 // set second in minute
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime();             // actually computes the new Date
		return zeroedDate;
	}
	
	public static int getCurrentHour() {
		Date date = new Date();             		  // timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		return cal.get(Calendar.HOUR_OF_DAY);
	}
	
	public static int getCurrentMinute() {
		Date date = new Date();             		  // timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		return cal.get(Calendar.MINUTE);
	}
	
	public static int getCurrentSecond() {
		Date date = new Date();             		  // timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		return cal.get(Calendar.SECOND);
	}
	
	public static String getTodaysStartDateAsString() {
		return getStartDateAsString(0);
	}
	
	public static String getStartDateAsString(long timestamp, int daysAgo) {
		Date date = getStartDate(timestamp, daysAgo);
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DATE);
		return cal.get(Calendar.YEAR) + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;		
	}
	
	public static String getStartDateAsString(int daysAgo) {
		return getStartDateAsString(System.currentTimeMillis(), daysAgo);
	}
	
	public static int getDaysSinceBeginningOfMonth() {
		long now = System.currentTimeMillis();
		return getDaysSinceBeginningOfMonth(now);
	}
	
	public static int getDaysSinceBeginningOfMonth(long timestamp) {
		Date date = new Date(timestamp);               	// timestamp 
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		return cal.get(Calendar.DAY_OF_MONTH);
	}
	
	public static String getTimeString(long seconds) {
		String timeString = "";
		if(seconds >= 60) {
			long minutes = seconds/60;
			if(minutes >= 60) {
				long hours = (long) Math.ceil(minutes/60.0f);
				if(hours >= 24) {
					long days = (long) Math.ceil(hours/24.0f);
					timeString = days + " day" + (days > 1 ? "s" : "");					
				}else {
					timeString = hours + " hour" + (hours > 1 ? "s" : "");
				}
			}else {
				timeString = minutes + " minute" + (minutes > 1 ? "s" : "");
			}
		}else {
			timeString = seconds + " second" + (seconds > 1 ? "s" : "");
		}
		return timeString;
	}
	
	public static Date addToDate( Date date, int field, int amount) {
	    Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.add( field, amount );
	    
	    return cal.getTime();
	}
}
