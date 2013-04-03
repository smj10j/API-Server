package com.smj10j.manager;

import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.UserDAO;
import com.smj10j.model.User;
import com.smj10j.util.Cache;

public abstract class UserManager {

	private static Logger logger = Logger.getLogger(UserManager.class);

	public static User getUser(Long userId) throws InvalidParameterException, FatalException {	
		User user = Cache.get(userId+"", Cache.namespace.USER_BY_ID);
		if(user == null) {
			user = UserDAO.getUser(userId);
			Cache.put(user, userId+"", Cache.namespace.USER_BY_ID);
		}
				
		//logger.debug("Got userId: " + user.getUserId() + OutputUtil.getElapsedString());
		return user;
	}		
		
	public static void save(User user) throws InvalidParameterException, FatalException {
		user.save();
		Cache.remove(user.getId()+"", Cache.namespace.USER_BY_ID);
	}
	
}
