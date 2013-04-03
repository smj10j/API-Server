package com.smj10j.model;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.UserType;

@MySQLTable(name=MySQL.TABLES.USER, 
		primaryKey="userId",
		transients={

		}
)

public class User extends DatabaseBackedObject implements Serializable {

	private static Logger logger = Logger.getLogger(User.class);
	
	private static final long serialVersionUID = 7114517961367041976L;

	private long userId;
			
	public User() {

	}
	
	public User(long userId) {
		setUserId(userId);
	}
	
	public String toString() {
		return "Id: " + userId;
	}

	public UserType toUserType() throws InvalidParameterException, FatalException {
					
		UserType userType = new UserType();
		userType.setUserId(getUserId());
		
		return userType;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getUserId() {
		return userId;
	}
	
	public static User from(MySQL mysql) throws FatalException, InvalidParameterException {
		
		User user = new User((Long)mysql.getColumn("user_id"));

		user.takeFieldValuesSnapshot();
		
		return user;
	}
}
