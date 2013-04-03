package com.smj10j.util;

public abstract class ArrayUtil {
	
	public static <T> String implode(T[] items, String glue) {
		StringBuilder str = new StringBuilder();
		int i = 0;
		for(T item : items) {
			if(i++ != 0)
				str.append(glue);
			str.append(item);
		}
		return str.toString();
	}
}
