package com.smj10j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.smj10j.conf.FatalException;

public abstract class ListUtil {

	public static <T> List<T> from(T... items) {
		List<T> newList = new ArrayList<T>();
		
		for(T item : items) {
			newList.add(item);
		}
		
		return newList;
	}
		
	public static <T> String implode(List<T> items, String glue) {
		return ArrayUtil.implode(items.toArray(), glue);
	}
	
	public static <T> T random(List<T> list) {
		int index = (int)Math.floor(Math.random()*list.size());
		return list.get(index);
	}
	
	public static <T> List<String> explode(String str, String separator) {
		List<String> list = new ArrayList<String>();
		String[] arr = str.split(separator);
		for(String s : arr) {
			list.add(s);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> deserialize(byte[] data) throws IOException, FatalException {
		//deserialize
		List<T> list = null;
		if(data != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(bis);
			try {
				list = (List<T>) is.readObject();
			} catch (ClassNotFoundException e) {
				throw new FatalException(e);
			}
		}else {
			list = new ArrayList<T>();
		}
		return list;
	}
	
	public static <T> byte[] serialize(List<T> list) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(list);
		return bos.toByteArray();
	}
}
