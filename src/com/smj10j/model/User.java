package com.smj10j.model;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.UserType;

@MySQLTable(name=MySQL.TABLES.USERS, 
		primaryKey="id",
		transients={

		}
)

public class User extends DatabaseBackedObject implements Serializable {

	private static Logger logger = Logger.getLogger(User.class);
	
	private static final long serialVersionUID = 7114517961367041976L;

	private long id;
	private String email;
			
	public User() {

	}
	
	public User(long id) {
		setId(id);
	}
	
	public String toString() {
		return "Id: " + id;
	}

	public UserType toUserType() throws InvalidParameterException, FatalException {
					
		UserType userType = new UserType();
		userType.setId(getId());
		userType.setEmail(getEmail());
		
		return userType;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}
	
	public static User from(MySQL mysql) throws FatalException, InvalidParameterException {
		
		User user = new User((Long)mysql.getColumn("id"));
		user.setEmail((String)mysql.getColumn("email"));

		user.takeFieldValuesSnapshot();
		
		return user;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
