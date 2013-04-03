package com.smj10j.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.manager.CustomerManager;
import com.smj10j.manager.UserManager;
import com.smj10j.util.ArrayUtil;
import com.smj10j.util.DateUtil;
import com.smj10j.util.ListUtil;
import com.smj10j.util.StringUtil;

public class APIRequest implements Serializable  {

	private static final long serialVersionUID = -6958443196956320691L;
	private static Logger logger = Logger.getLogger(APIRequest.class);

	private HttpServletRequest request;
	private Map<String, String> defaultParameters = new HashMap<String, String>();
	private String methodName;
	private Customer customer;
	private User user;
	private boolean newRequest;	//if false, then the request is coming from inside the application
	private boolean requiresAdmin;
	private Map<String, String> overridenParameters = new LinkedHashMap<String, String>(); 
	private String secretKey;
	private String postBody;
	
	public APIRequest(HttpServletRequest request) {
		this.request = request;
		this.newRequest = true;
		overrideParameterMap();
	}
	
	public APIRequest(HttpServletRequest request, boolean newRequest) {
		this.request = request;
		this.newRequest = newRequest;
		if(this.newRequest) {
			overrideParameterMap();
		}
	}
	
	public HttpServletRequest getHttpServletRequest() {
		return request;
	}
	
	public String getHeader(String name) {
		return request.getHeader(name);
	}
	
	public String getIp() {
		String ip = getHeader("x-real-ip");
		if(ip == null || ip.equals("127.0.0.1")) {
			ip = request.getRemoteAddr();
		}
		if(ip == null)
			ip = "";
		return ip;
	}
	
	/**
	 * @return eg. http://api.server.com/warpath/api
	 */
	public String getRequestURL() {
		return request.getRequestURL().toString();
	}
		
	/**
	 * @return eg. http://api.server.com/warpathv1
	 */
	public String getBaseURL() {
		return request.getRequestURL().substring(0,request.getRequestURL().lastIndexOf("/"));
	}
	
	public void setDefaultParameter(String name, String value) {
		defaultParameters.put(name, value);
	}
	
	public String[] getParameterArray(String name, boolean required) throws InvalidParameterException {
		String[] paramValues = request.getParameterValues(name);
		if(paramValues == null) {
			if(required) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.MISSING_REQUIRED_PARAMETER, ListUtil.from(name));
			}
		}
		return paramValues;
	}
	
	//this is the normally called method - calling with required has already been handled
	//by parameter checking in the gateway servlet
	public String getParameter(String name) throws InvalidParameterException {
		return getParameter(name, false);
	}
	
	private String getParameterInternal(String name) {
		String value = overridenParameters.get(name);
		if(value == null) {
			String[] paramValues = request.getParameterValues(name);
			value = paramValues != null ? paramValues[0] : null;
		}
		return value;
	}
	
	//this method is normally not called directly, but it can be
	//an example when it would be useful is having a set of parameters that are only 
	//required depending upon the value of another parameter
	public String getParameter(String name, boolean required) throws InvalidParameterException {
		String value = getParameterInternal(name);
		if(value == null) {
			if(required) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.MISSING_REQUIRED_PARAMETER, ListUtil.from(name));
			}else {
				//get the default, if available
				String defaultValue = defaultParameters.get(name);
				if(defaultValue != null) {	//we don't return this if null because the original value may be empty string (and acceptable)
					return defaultValue;
				}
			}
		}
		return value;
	}
	
	public Integer getParameterInt(String name, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param)) {
			try {
				return Integer.parseInt(param);
			}catch(NumberFormatException e) {
				try {
					double doubleValue = Double.parseDouble(param);
					if(Math.floor(doubleValue) == doubleValue) {
						//make sure it's still an integer and not a long
						if(doubleValue > Integer.MAX_VALUE) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Integer", param));										
						}						
						return (int)(doubleValue);
					}else {
						throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Integer", param));										
					}
				}catch(NumberFormatException e1) {
					throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Integer", param));				
				}
			}	
		}
		return null;	
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParameterType(String name, Class<T> clazz, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		String typesStr = "";
		if(!StringUtil.isNullOrEmpty(param)) {
			try {
				typesStr = ArrayUtil.implode((T[])clazz.getMethod("values").invoke(null), ",");
				return (T)clazz.getMethod("valueOf", String.class).invoke(null, param.toUpperCase());
			}catch(Exception e) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(Constants.Request.TYPE, typesStr, param));
			}	
		}
		return null;	
	}
	
	public Long getParameterLong(String name, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param)) {
			try {
				return Long.parseLong(param);
			}catch(NumberFormatException e) {
				try {
					double doubleValue = Double.parseDouble(param);
					if(Math.floor(doubleValue) == doubleValue) {
						return (long)(doubleValue);
					}else {
						throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Long", param));										
					}
				}catch(NumberFormatException e1) {
					throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Long", param));				
				}
			}	
		}
		return null;
	}

	
	public Float getParameterFloat(String name, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param))
			try {
				return Float.parseFloat(param);
			}catch(NumberFormatException e1) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Float", param));				
			}
		return null;
	}
	
	public Double getParameterDouble(String name, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param))
			try {
				return Double.parseDouble(param);
			}catch(NumberFormatException e1) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "Float", param));				
			}
		return null;
	}
	
	public Boolean getParameterBool(String name, boolean required) throws InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param))
			return Boolean.parseBoolean(param);
		return null;		
	}
	
	public Date getParameterTimestamp(String name, boolean required) throws NumberFormatException, InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param)) {
			long longTime = Long.parseLong(param);
			if(longTime == 0) {
				return new Date();	//now!
			}
			return new Date(longTime);
		}
		return null;
	}
	
	public Date getParameterDate(String name, boolean required) throws NumberFormatException, InvalidParameterException {
		String param = getParameter(name, required);
		if(StringUtil.isNullOrEmpty(param)) {
			return null;
		}
		try {
			return DateUtil.parse(param);
		} catch (ParseException e) {
			throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(name, "A valid Java Date", param));
		}
	}
	
	
	public JSONObject getParameterJSONObject(String name, boolean required) throws NumberFormatException, InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param)) {
			try {
				JSONObject jsonData = new JSONObject(param);
				return jsonData;
			}catch(JSONException e) {
				throw new InvalidParameterException(Constants.Error.GENERAL.INVALID_JSON, ListUtil.from(name, e.getMessage()));
			}		
		}
		return null;
	}
	
	public JSONArray getParameterJSONArray(String name, boolean required) throws NumberFormatException, InvalidParameterException {
		String param = getParameter(name, required);
		if(!StringUtil.isNullOrEmpty(param)) {
			try {
				JSONArray jsonData = new JSONArray(param);
				return jsonData;
			}catch(JSONException e) {
				throw new InvalidParameterException(Constants.Error.GENERAL.INVALID_JSON, ListUtil.from(name, e.getMessage()));
			}		
		}
		return null;
	}
	
	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void setUserFromRequest() throws InvalidParameterException, FatalException {
		Long userId = getParameterLong(Constants.Request.USER_ID, false);		
		
		if(userId != null) {
			//validate the user
		
			//throws exception if the userId is invalid
			User user = UserManager.getUser(userId);
			setUser(user);		
			
		}
	}
	
	public void setCustomerFromRequest() throws FatalException, InvalidParameterException {
		if(customer == null) {
			Long customerId = getParameterLong(Constants.Request.CUSTOMER_ID, false);
			String apiKey = getParameter(Constants.Request.API_KEY, false);

			if(!StringUtil.isNullOrEmpty(apiKey) || customerId != null) {
				if(customerId != null) {
					this.customer = CustomerManager.getCustomer(customerId);					
				}else {
					this.customer = CustomerManager.getCustomer(apiKey);
				}
			}
		}
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setRequiresAdmin(boolean requiresAdmin) {
		this.requiresAdmin = requiresAdmin;
	}

	public boolean isRequiresAdmin() {
		return requiresAdmin;
	}

	//admin requests that are reads are not logged as admin requests since they change nothing
	//this is mostly used when creating a log of admin events
	public boolean isAdminWrite() throws FatalException {
		return isRequiresAdmin() && MySQL.wasWriteMade();
	}
	
	public void overrideParameter(String name, String value) {
		if(value == null) {
			overridenParameters.remove(name);
		}else {
			overridenParameters.put(name, value);
		}
	}
		
	public Map<String, String[]> getParameterMap() {
		Map<String, String[] > parameterMap = new LinkedHashMap<String, String[]>();
		
		for(String name : overridenParameters.keySet()) {
			String[] values = {
				overridenParameters.get(name)
			};
			parameterMap.put(name, values);
		}
		return parameterMap;
	}

	public void setNewRequest(boolean newRequest) {
		this.newRequest = newRequest;
	}

	public boolean isNewRequest() {
		return newRequest;
	}

	
	
	private void overrideParameterMap() {
					
		if(postBody == null) {
			//if we don't have the postBody stored in this APIRequest, try and get it from the HttpServletRequest
			postBody = (String)request.getAttribute("com.smj10j.model.APIRequest.postBody"); 
			if(postBody == null) {
				//if the HttpServletRequest doesn't have pre-stored, create it from scratch
				StringWriter queryStringWriter = new StringWriter();
				
				//for a GET request we can use the query string
				if(request.getMethod().equalsIgnoreCase("get")) {
					postBody = request.getQueryString();
					//logger.debug("postBody: " + postBody);
					
				}else {
					//otherwise, we have to read from the input stream
					try {
						BufferedReader reader = request.getReader();
						char[] buf = new char[4 * 1024]; // 4Kchar buffer
						int len;
						while ((len = reader.read(buf, 0, buf.length)) != -1) {
							queryStringWriter.write(buf, 0, len);
						}
						postBody = queryStringWriter.toString();
						
					}catch(IOException e) {
						logger.error("Failed to generate postBody from POST parameters", e);
					}
				}
				
				if(postBody != null) {
					request.setAttribute("com.smj10j.model.APIRequest.postBody", postBody); 					
				}
			}
		}
				
		String[] keyValuePairs = postBody == null ? new String[0] : postBody.split("&");

		String key = null;
		String value = null;

		for(String keyValuePair : keyValuePairs) {
			
			String[] keyAndValue = keyValuePair.split("=");
			if (keyAndValue.length < 1) {
				logger.warn("Failed to generate querystring from parameters - No name/value pair at: " + keyValuePair);
			} 
			try {
				key = URLDecoder.decode(keyAndValue[0], "UTF-8");
				if(keyAndValue.length == 1 || StringUtil.isNullOrEmpty(keyAndValue[1])) {
					//value is empty
					value = "";
				}else {
					value = URLDecoder.decode(keyAndValue[1], "UTF-8");
				}
			} catch (UnsupportedEncodingException e) {
				logger.warn("Failed to generate querystring from POST parameters - Bad encoding for name/value pair at: " + keyValuePair, e);
			}
			
			overrideParameter(key, value);
			//logger.debug("Overridden Parameter " + key + "=" + value);
		}
	}

	public String getPostBody() {
		return postBody;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

}
