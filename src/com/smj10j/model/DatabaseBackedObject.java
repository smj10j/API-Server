package com.smj10j.model;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.GeneralDAO;
import com.smj10j.util.SecurityUtil;
import com.smj10j.util.StringUtil;


public class DatabaseBackedObject implements Serializable {

	private static final long serialVersionUID = -5750650481592179611L;

	private static Logger logger = Logger.getLogger(DatabaseBackedObject.class);

	private Map<String, String> fieldValueHashes;
	
	public final void save() throws InvalidParameterException, FatalException {
		GeneralDAO.save(this);
	}
	
	public final void remove() throws InvalidParameterException, FatalException {
		GeneralDAO.remove(this);
	}
	
	public final Object getPrimaryId() throws FatalException {
		try {
			Class<?> clazz = this.getClass();
			MySQLTable tableAnnotation = clazz.getAnnotation(MySQLTable.class);
			String uniqueIdName = tableAnnotation.primaryKey();
			try {
				return (Long)clazz.getDeclaredMethod("get"+uniqueIdName.substring(0,1).toUpperCase()+uniqueIdName.substring(1)).invoke(this);
			}catch(ClassCastException e) {
				//not a Long primary key
				return (String)clazz.getDeclaredMethod("get"+uniqueIdName.substring(0,1).toUpperCase()+uniqueIdName.substring(1)).invoke(this);
			}
		} catch (IllegalArgumentException e) {
			throw new FatalException(e);
		} catch (IllegalAccessException e) {
			throw new FatalException(e);
		} catch (SecurityException e) {
			throw new FatalException(e);
		} catch (InvocationTargetException e) {
			throw new FatalException(e);
		} catch (NoSuchMethodException e) {
			throw new FatalException(e);
		}	
	}
	
	public final Set<String> takeFieldValuesSnapshot() throws FatalException {
		
		Set<String> changedFieldNames = new HashSet<String>();
		Map<String, String> oldFieldValueHashes = fieldValueHashes;
		fieldValueHashes = new HashMap<String, String>();

		try {
			
			Class<?> clazz = this.getClass();
			MySQLTable tableAnnotation = clazz.getAnnotation(MySQLTable.class);
	
			for(Method method : clazz.getDeclaredMethods()) {
				String methodName = method.getName();
				if(!methodName.startsWith("get") && !methodName.startsWith("is")) {
					continue;
				}
	
				String fieldName = methodName.startsWith("get") ? methodName.replaceFirst("get","") : methodName.replaceFirst("is", "");
				if(StringUtil.isNullOrEmpty(fieldName)) {
					//this occurs when the method name is simply "get" or "is"
					continue;
				}
				fieldName = (fieldName.substring(0,1).toLowerCase()+fieldName.substring(1));
				
				//ignore transients
				boolean isTransient = false;
				for(String name : tableAnnotation.transients()) {
					if(name.equals(fieldName)) {
						isTransient = true;
						break;
					}
				}
				if(isTransient) {
					continue;
				}				

				Object fieldValue = method.invoke(this);
				String fieldValueStr = fieldValue == null ? "null" : fieldValue.toString();
				
				fieldValueHashes.put(fieldName, SecurityUtil.md5(fieldValueStr));
			}
			
			if(oldFieldValueHashes != null) {
				//now check the new hashes against the old
				for(String fieldName : fieldValueHashes.keySet()) {
					String newHash = fieldValueHashes.get(fieldName);
					String oldHash = oldFieldValueHashes.get(fieldName);
					//logger.debug("Comparing fieldName="+fieldName+", oldHash="+oldHash+", newHash="+newHash);
					if(oldHash != null && oldHash.equals(newHash)) {
						continue;
					}
					changedFieldNames.add(fieldName);
				}
			}
			
			return changedFieldNames;			
			
		} catch (IllegalArgumentException e) {
			throw new FatalException(e);
		} catch (IllegalAccessException e) {
			throw new FatalException(e);
		} catch (InvocationTargetException e) {
			throw new FatalException(e);
		}
	}
	
}
