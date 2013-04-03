package com.smj10j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.smj10j.conf.FatalException;

public abstract class SetUtil {
	
	public static <T> String implode(Set<T> items, String glue) {
		return ArrayUtil.implode(items.toArray(), glue);
	}
	
	public static <T> Set<String> explode(String str, String separator) {
		Set<String> set = new HashSet<String>();
		String[] arr = str.split(separator);
		for(String s : arr) {
			set.add(s);
		}
		return set;
	}	
	
	@SuppressWarnings("unchecked")
	public static <T> Set<T> deserialize(byte[] data) throws IOException, FatalException {
		//deserialize
		Set<T> set = null;
		if(data != null) {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(bis);
			try {
				set = (Set<T>) is.readObject();
			} catch (ClassNotFoundException e) {
				throw new FatalException(e);
			}
		}else {
			set = new HashSet<T>();
		}
		return set;
	}
	
	public static <T> byte[] serialize(Set<T> set) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(set);
		return bos.toByteArray();
	}
	
	public static <T> Set<T> fromList(List<T> list) {
		Set<T> set = new HashSet<T>();
		for(T item : list) {
			set.add(item);
		}
		return set;
	}
}
