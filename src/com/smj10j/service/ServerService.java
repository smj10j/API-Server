package com.smj10j.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.ApiMethodFilterType;
import com.smj10j.jaxb.ApiMethodFiltersType;
import com.smj10j.jaxb.ApiMethodParameterType;
import com.smj10j.jaxb.ApiMethodParametersType;
import com.smj10j.jaxb.ApiMethodType;
import com.smj10j.jaxb.ApiMethodsType;
import com.smj10j.jaxb.ErrorCodeType;
import com.smj10j.jaxb.ErrorCodesType;
import com.smj10j.model.APIRequest;
import com.smj10j.model.APIResponse;
import com.smj10j.model.MethodParameter;
import com.smj10j.util.Cache;

public abstract class ServerService {

	private static Logger logger = Logger.getLogger(ServerService.class);
	
	public static void getStatus(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {
		//ping the db
		MySQL mysql = MySQL.getInstance(true);
		mysql.isValid();	//refresh the db connection
		
		List<String> messages = new ArrayList<String>();
		messages.add("Tip-top shape, commander");
		messages.add("Things're lookin' quite nice!");
		messages.add("Quite right");
		messages.add("Bippity boppity gimme the zoppity!");
		messages.add("Goin' mach five");
		messages.add("Fire it up");
		messages.add("Let's get ready to ruuuummmmbbblee!");
		Collections.shuffle(messages);
		
		response.get().setMessage(messages.get(0));
	}
	
	
	public static void getErrorCodes(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {
	
		response.get().setErrorCodes(new ErrorCodesType());
		for(int code : Constants.Error.map.keySet()) {
			ErrorCodeType errorCodeType = new ErrorCodeType();
			errorCodeType.setName(Constants.Error.map.get(code));
			errorCodeType.setCode(code);
			response.get().getErrorCodes().getErrorCode().add(errorCodeType);
		}
	}
	
	public static void getMethods(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {

		Boolean publicOnly = request.getParameterBool(Constants.Request.PUBLIC, false);
		if(publicOnly == null) {
			publicOnly = true;
		}

		response.get().setApiMethods(new ApiMethodsType());
		response.get().setApiMethodFilters(new ApiMethodFiltersType());
		
		SortedSet<String> sortedApiMethodNames = new TreeSet<String>(Constants.methodParameters.keySet());

		for(String methodName : sortedApiMethodNames) {
			
			//don't show methods in the admin service
			if(publicOnly && Constants.privateMethods.containsKey(methodName)) {
				continue;
			}
			
			ApiMethodType apiMethodType = new ApiMethodType();
			apiMethodType.setName(methodName);
			apiMethodType.setDescription(Constants.methodDescriptions.get(methodName));
			apiMethodType.setApiMethodParameters(new ApiMethodParametersType());
			List<MethodParameter> methodParameters = Constants.methodParameters.get(methodName);
			for(MethodParameter methodParameter : methodParameters) {
				ApiMethodParameterType apiMethodParameterType = new ApiMethodParameterType();
				apiMethodParameterType.setName(methodParameter.getName());	
				apiMethodParameterType.setDefault(methodParameter.getDefaultValue());
				apiMethodParameterType.setRequired(methodParameter.isRequired());
				apiMethodParameterType.setType(methodParameter.getType());
				apiMethodParameterType.setDescription(methodParameter.getDescription());
				apiMethodType.getApiMethodParameters().getApiMethodParameter().add(apiMethodParameterType);
			}
			response.get().getApiMethods().getApiMethod().add(apiMethodType);
		}
		

		SortedSet<String> sortedApiMethodFilterNames = new TreeSet<String>(Constants.Request.methodFilters.keySet());

		for(String methodFilterName : sortedApiMethodFilterNames) {
			for(String matchesString : Constants.Request.methodFilters.get(methodFilterName)) {
				ApiMethodFilterType apiMethodFilterType = new ApiMethodFilterType();
				apiMethodFilterType.setName(methodFilterName);
				apiMethodFilterType.setMatches(matchesString);
				response.get().getApiMethodFilters().getApiMethodFilter().add(apiMethodFilterType);
			}
		}
		
	}
	
	public static void cacheClear(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {
		Cache.clear();
				
		response.get().setMessage("Cache cleared");
	}
}
