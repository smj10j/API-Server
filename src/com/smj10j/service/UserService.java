package com.smj10j.service;

import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.jaxb.UserType;
import com.smj10j.model.APIRequest;
import com.smj10j.model.APIResponse;
import com.smj10j.model.User;

public abstract class UserService {
	
	private static Logger logger = Logger.getLogger(UserService.class);
		
	public static void getUser(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {
		User user = request.getUser();
			
		UserType userType = user.toUserType();
		
		response.get().setUser(userType);		
	}
		
}
