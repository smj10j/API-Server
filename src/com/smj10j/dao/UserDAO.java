package com.smj10j.dao;


import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.User;
import com.smj10j.util.ListUtil;

public abstract class UserDAO {

	private static Logger logger = Logger.getLogger(UserDAO.class);
	
	public static User getUser(long userId) throws InvalidParameterException, FatalException {

		MySQL mysql = MySQL.getInstance(true);
		
		mysql.query("" +
				"SELECT * FROM " + MySQL.TABLES.USER + " " +
				"WHERE user_id=? LIMIT 1",
				userId);
		if(mysql.nextRow()) {
			return User.from(mysql);
		}else {
			throw new InvalidParameterException(Constants.Error.INVALID_ID.USER_ID, ListUtil.from(userId+""));
		}
	}
}
