package com.smj10j.dao;



import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.DatabaseBackedObject;
import com.smj10j.model.Server;
import com.smj10j.model.User;
import com.smj10j.util.StringUtil;

/*
 * Currently will only save to tables that have primitive data fields
 */
public abstract class GeneralDAO {

	private static Logger logger = Logger.getLogger(GeneralDAO.class);

	private static final Set<Class<?>> PrimaryKeyTypes = new HashSet<Class<?>>();
	static {
		//If one of these types is a member variable of an object being saved, we'll try and save the object id
		PrimaryKeyTypes.add(User.class);
		PrimaryKeyTypes.add(Server.class);
	}
	private static final Set<Class<?>> UtilityTypes = new HashSet<Class<?>>();
	static {
		//We'll still save these object, but not by primary key
	}
	private static final Set<Class<?>> JavaTypes = new HashSet<Class<?>>();
	static {
		//These objects are straight-forward to save
		JavaTypes.add(String.class);
		JavaTypes.add(Integer.class);
		JavaTypes.add(Long.class);
		JavaTypes.add(Boolean.class);
		JavaTypes.add(Double.class);
		JavaTypes.add(Float.class);
		JavaTypes.add(Date.class);
	}
	
	public static <T extends DatabaseBackedObject> Object save(T object) throws InvalidParameterException, FatalException {

		Class<? extends DatabaseBackedObject> clazz = object.getClass();
		
		//get the table name
		String tableName = clazz.getAnnotation(MySQLTable.class).name();
		
		Object uniqueId = null;
		String columnNamesString = "";
		String valuesString = "";
		String updateString = "";
		String conditionString = "";
		List<Object> parameters = new ArrayList<Object>();
		
		try {
			//get the id field

			MySQLTable tableAnnotation = clazz.getAnnotation(MySQLTable.class);
			String uniqueIdName = tableAnnotation.primaryKey();
			uniqueId = object.getPrimaryId();
			
			
			boolean doInsert = (uniqueId == null);
			if(!doInsert) {
				try {	
					//if non-null make sure it's not 0
					doInsert = ((Long)uniqueId == 0);
				}catch(ClassCastException e) {
					//not null and not 0 because all our IDs are Longs
					doInsert = false;
				}
			}
			
			//if we're not doing an insert, try and get the list of changed fields
			Set<String> changedFieldNames = doInsert ? new HashSet<String>() : object.takeFieldValuesSnapshot();
			
			int counter = 0;
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
				
				if(!doInsert) {
					if(changedFieldNames.size() > 0 && !changedFieldNames.contains(fieldName) && !fieldName.equalsIgnoreCase(uniqueIdName)) {
						//logger.debug("Not adding fieldName="+fieldName+" to update clause because it has not changed");
						continue;
					}
				}
				
				//map apiKey => api_key, etc., remoteDeviceUDID => remote_device_udid
				String columnName = fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
				//for now let's just fix this special case
				columnName = columnName.replace("u_u_i_d", "uuid");
				
				//logger.debug("Getting value of " + fieldName + ", which we believe to be a " + method.getReturnType().getName());
				
				//finally get the field value
				Object fieldValue = null;
				if(	PrimaryKeyTypes.contains(method.getReturnType())){
					//for API model object we want to be clear we just save ID
					columnName+= "_id";
					Object modelObject = method.invoke(object);
					if(modelObject != null) {
						MySQLTable modelObjectTableAnnotation = method.getReturnType().getAnnotation(MySQLTable.class);
						String modelObjectUniqueIdName = modelObjectTableAnnotation.primaryKey();
						try {
							fieldValue = (Long)method.getReturnType().getDeclaredMethod("get"+modelObjectUniqueIdName.substring(0,1).toUpperCase()+modelObjectUniqueIdName.substring(1)).invoke(modelObject);
						}catch(ClassCastException e) {
							fieldValue = (String)method.getReturnType().getDeclaredMethod("get"+modelObjectUniqueIdName.substring(0,1).toUpperCase()+modelObjectUniqueIdName.substring(1)).invoke(modelObject);						
						}
					}else {
						fieldValue = null;
					}
				}else {
					fieldValue = method.invoke(object);					
				}
				
				
				if(fieldName.equalsIgnoreCase(uniqueIdName)) {
					if(doInsert) {
						
					}else {
						conditionString = "WHERE " + columnName + "=?";
					}
				}else {
					if(columnName.equals("updated")) {
						
						columnNamesString+= (counter>0 ? "," : "") + columnName;
						valuesString+= (counter>0 ? "," : "") + "utc_timestamp()";
						updateString+= (counter>0 ? "," : "") + columnName+"=utc_timestamp()";							
						counter++;								

					}else if(columnName.equals("created")) {
						
						if(doInsert) {
							columnNamesString+= (counter>0 ? "," : "") + columnName;
							valuesString+= (counter>0 ? "," : "") + "utc_timestamp()";
							updateString+= (counter>0 ? "," : "") + columnName+"=utc_timestamp()";//unused														
							counter++;								
						}
						
					}else {
						if(method.getReturnType().isEnum()) {
							parameters.add(fieldValue.toString());
						}else if(method.getReturnType().equals(JSONObject.class) || method.getReturnType().equals(JSONArray.class)) {
							parameters.add(fieldValue.toString());
						}else if(method.getReturnType().equals(Date.class)) {
							parameters.add(fieldValue != null ? fieldValue : "0000-00-00 00:00:00");
						}else if(fieldValue != null && JavaTypes.contains(method.getReturnType())) {
							parameters.add(fieldValue+"");							
						}else {
							// checks if the object needs to be serialized because it is not a primitive, string, or a object that is saved by just id
							if(fieldValue != null && !method.getReturnType().isPrimitive() 
									&& !method.getReturnType().equals(String.class) && !PrimaryKeyTypes.contains(method.getReturnType())) {
								
								
								fieldValue = MySQL.serialize(fieldValue);
							}
							parameters.add(fieldValue);								
						}
						columnNamesString+= (counter>0 ? "," : "") + columnName;
						valuesString+= (counter>0 ? "," : "") + "?";
						updateString+= (counter>0 ? "," : "") + columnName+"=?";

						counter++;
					}
				}
				

			}
			
		
			
			MySQL mysql = MySQL.getInstance(true);
						
			//mysql.debugNext();
			if(doInsert) {
				mysql.query("INSERT INTO " + tableName + " ("+columnNamesString+") VALUES ("+valuesString+")", parameters.toArray());
				Long newId = mysql.lastInsertId(tableName);
				clazz.getDeclaredMethod("set"+uniqueIdName.substring(0,1).toUpperCase()+uniqueIdName.substring(1), long.class).invoke(object,newId);
				
				try {
					Method setCreatedMethod = clazz.getDeclaredMethod("setCreated", Date.class);
					mysql.query("SELECT utc_timestamp() as 'now'");
					if(mysql.nextRow()) {
						setCreatedMethod.invoke(object, (Date)mysql.getColumn("now"));
					}
				}catch(NoSuchMethodException e) {
					//no created parameter - okay
				}
				
				return newId;
			}else {			
				parameters.add(uniqueId);
				mysql.query("UPDATE " + tableName + " SET " + updateString + " " + conditionString + " LIMIT 1", parameters.toArray());
				return uniqueId;
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
	
	public static <T extends DatabaseBackedObject> void remove(T object) throws InvalidParameterException, FatalException {
		
		Class<? extends DatabaseBackedObject> clazz = object.getClass();
		
		//get the table name
		String tableName = clazz.getAnnotation(MySQLTable.class).name();
		
		Object uniqueId = null;
		
		try {
			//get the id field
			MySQLTable tableAnnotation = clazz.getAnnotation(MySQLTable.class);
			String uniqueIdName = tableAnnotation.primaryKey();
			uniqueId = object.getPrimaryId();
			
			String uniqueIdColumnName = uniqueIdName.replaceAll("([A-Z])", "_$1").toLowerCase();
	
			MySQL mysql = MySQL.getInstance(true);
			
			boolean canRemove = (uniqueId != null);
			if(!canRemove) {
				try {	//if non-null make sure it's not 0
					canRemove = ((Long)uniqueId != 0);
				}catch(ClassCastException e) {
					//not null and not 0 because all our IDs are Longs
				}
			}
			
			//mysql.debugNext();
			if(canRemove) {
				mysql.query("DELETE FROM " + tableName + " WHERE " + uniqueIdColumnName + "=? LIMIT 1",uniqueId);
			}else {			
				throw new FatalException("Tried to remove an item from " + tableName + " but there was a null or 0 uniqueId");
			}
			
		} catch (IllegalArgumentException e) {
			throw new FatalException(e);
		} catch (SecurityException e) {
			throw new FatalException(e);
		}				
	}
	
}
