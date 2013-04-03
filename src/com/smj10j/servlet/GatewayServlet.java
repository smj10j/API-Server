package com.smj10j.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.ErrorType;
import com.smj10j.jaxb.ResponseType;
import com.smj10j.model.APIRequest;
import com.smj10j.model.APIResponse;
import com.smj10j.model.MethodParameter;
import com.smj10j.util.CSVUtil;
import com.smj10j.util.DateUtil;
import com.smj10j.util.EmailUtil;
import com.smj10j.util.JAXBUtil;
import com.smj10j.util.JSONUtil;
import com.smj10j.util.ListUtil;
import com.smj10j.util.OutputUtil;
import com.smj10j.util.RemoteRequestUtil;
import com.smj10j.util.SecurityUtil;
import com.smj10j.util.StringUtil;

public class GatewayServlet extends HttpServlet {


	private static final long serialVersionUID = -1049804512043767495L;
	private static Logger logger = Logger.getLogger(GatewayServlet.class);
	private static String SERVER_BASE_URL = "";
	private static String EXTERNAL_SERVER_BASE_URL = "";
	private static String LOCAL_SERVER_BASE_URL = "";
	private static String HOSTNAME = "";

	public static Map<Long, Long> threadToStartTimestamp = new ConcurrentHashMap<Long, Long>();


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		APIRequest apiRequest = new APIRequest(request);
    	APIResponse apiResponse = new APIResponse(response);
    	processRequest(apiRequest, apiResponse, true);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		APIRequest apiRequest = new APIRequest(request);
    	APIResponse apiResponse = new APIResponse(response);
    	processRequest(apiRequest, apiResponse, true);
    }
    
    public static boolean isProduction() {
    	return SERVER_BASE_URL.length() > 0 && !isSandbox() && !isBeta();
    }
    
    public static boolean isSandbox() {
    	return GatewayServlet.getExternalServerBaseUrl().contains("sandbox");
    }
    
    public static boolean isBeta() {
    	return GatewayServlet.getExternalServerBaseUrl().contains("beta");
    }
    
    public static String getLocalServerBaseUrl() {
    	return LOCAL_SERVER_BASE_URL;
    }
    
    public static String getHostname() {
    	return HOSTNAME;
    }
    
    public static String getExternalServerBaseUrl () {
    	return EXTERNAL_SERVER_BASE_URL;
    }
   
    
	@SuppressWarnings("unchecked")
	public void processRequest(APIRequest apiRequest, APIResponse apiResponse, boolean newRequest) throws ServletException, IOException {
		
		//logger.debug("Request URL: " + request.getRequestURL());			//eg. http://api.HOST_TLD.com/warv1/api
		//logger.debug("Request URI: " + request.getRequestURI());			//eg. /warv1/api
		//logger.debug("Request QueryString: " + request.getQueryString());	//eg. method=server.status

		if(StringUtil.isNullOrEmpty(SERVER_BASE_URL)) {	
			String requestUrl = apiRequest.getRequestURL();
			SERVER_BASE_URL = requestUrl.substring(0,requestUrl.lastIndexOf("/api")+1);
			SERVER_BASE_URL = SERVER_BASE_URL.replace(":80", "").replace(":443", "").replace("http://", "https://");
			
			// Sets the URL that will be used when making asynchronous logging requests
			LOCAL_SERVER_BASE_URL = SERVER_BASE_URL.replaceFirst("https://[^/]*/", "http://localhost:8080/");		
			
			// Sets the URL that will be used by callback services (Twilio, Facebook, etc.)
			if(SERVER_BASE_URL.contains("beta") ) {
				EXTERNAL_SERVER_BASE_URL = SERVER_BASE_URL;
	    	}else if (SERVER_BASE_URL.contains("sandbox")) {
				EXTERNAL_SERVER_BASE_URL = SERVER_BASE_URL;
	    	}else {
				EXTERNAL_SERVER_BASE_URL = SERVER_BASE_URL.replaceFirst("https://[^/]*/", "https://api."+Constants.HOST_TLD+".com/");
	    	}
			
			//hostname is returned in requests
			HOSTNAME = EXTERNAL_SERVER_BASE_URL.replaceFirst("https://", "");
			HOSTNAME = HOSTNAME.substring(0, HOSTNAME.indexOf("/"));
			
			logger.info("Setting SERVER_BASE_URL to: " + SERVER_BASE_URL);
			logger.info("Setting EXTERNAL_SERVER_BASE_URL to: " + getExternalServerBaseUrl());
			logger.info("Setting LOCAL_SERVER_BASE_URL to: " + getLocalServerBaseUrl());
			logger.info("Setting HOSTNAME to: " + HOSTNAME);
		}
		
		boolean canReturnResponse = true;	//can be set during execution to prevent structured output. this is currently only used by the race condition checks

		long requestStartTs = System.currentTimeMillis();
		threadToStartTimestamp.put(Thread.currentThread().getId(), requestStartTs);

    	apiResponse.get().setTimestamp(requestStartTs);
    	apiResponse.get().setServer(getHostname());
    	apiResponse.get().setStatus("ok");	//will be overwritten on error
    	try {
			String asyncToken = apiRequest.getParameter(Constants.Request.ASYNC_TOKEN);
			apiResponse.get().setAsyncToken(asyncToken);
		} catch (InvalidParameterException e1) {
			//okay not to have one
		}
	
		PrintWriter out = apiResponse.getWriter();
    	
		String responseFormat = null;
		String methodName = null;
		try {
			responseFormat = apiRequest.getParameter(Constants.Request.RESPONSE_FORMAT);
			methodName = apiRequest.getParameter(Constants.Request.METHOD);
	    	if(StringUtil.isNullOrEmpty(methodName)) {
	    		throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_METHOD, ListUtil.from(""));
	        }
	    	apiRequest.setMethodName(methodName);
	    	apiResponse.get().setMethod(methodName);
	    	
	    	//this list of available methods is generated by conf/services.xml
	    	Method method = Constants.allMethods.get(methodName);
	    	if(method != null) {
	    		try {
	    			logger.info("-- Begin Method " + methodName + " --");	
	    			
	    			//validate the parameters
	    			validateRequestParameters(apiRequest, apiResponse);	    			

	    			//before we invoke the method we want to do some security-related stuff
	    			//as well as attach this request to a site/session/user
	    			//this method will throw exceptions on failure
	    			validateRequest(apiRequest, apiResponse);
	    			
	    			
	    			//and off we go!
	    			logger.info("-- Enter " + methodName + " --");	    			
	    			method.invoke(null, apiRequest, apiResponse);
	    			
				} catch(UndeclaredThrowableException e) {	//thrown by HBase methods on connection timeout
					throw new FatalException(e);
				} catch (IllegalArgumentException e) {
					throw new FatalException(e);
				} catch (IllegalAccessException e) {
					throw new FatalException(e);
				} catch (InvocationTargetException e) {
					if(e.getCause().getClass().equals(InvalidParameterException.class)) {
						throw (InvalidParameterException)e.getCause();
					} else {
						throw new FatalException(e);
					}
				}
	    	}else {
	    		throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_METHOD, ListUtil.from(methodName));
	    	}	   
	    	
    		//commit the database changes
			MySQL.commit();
			
    	} catch(FatalException e) {
    		
			//email a notice to admins			
			try {
				String subject = "Fatal Error on ("+getHostname()+")";
				String body = "Fatal Error on - " + apiRequest.getHttpServletRequest().getRequestURL() + "?" + RemoteRequestUtil.getQueryString(apiRequest) + "\n\n";
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(baos);
				e.printStackTrace(printStream);
				printStream.flush();
				body+= baos.toString();
				
				EmailUtil.email(null, "error-notifier", ListUtil.from(EmailUtil.getInternalAdminEmail()), subject, body.getBytes(), "txt", null);

			} catch (FatalException e2) {
				//oh bonkers
    			logger.error("Error while trying to email admins about fatal error! " + e2.getMessage());	    			
			} catch (InvalidParameterException e2) {
    			logger.error("Error while trying to email admins about fatal error! " + e2.getMessage());	    			
			}
    		
    		//we don't know what the hell happened
    		e.printStackTrace();
    		logger.error(e);
    		
    		//rollback the database changes
    		try {
				MySQL.rollback();
			} catch (FatalException e1) {
				//wut.
				e1.printStackTrace();
			}
			
	    	//log unsuccessful api calls
    		try {
    			//(new Event(apiRequest.getCustomer(), apiRequest.getUser(), apiRequest.getDevice(), null, methodName, "fatal error", apiRequest.isAdminWrite(), apiRequest.getAdminSecretKey())).saveAsync();	    				
				MySQL.commit();
    		}catch(FatalException e1) {
    			logger.error("Caught FatalException while create LogEvent about method failure");
    			logger.error(e1);
    		}
			
    		apiResponse.get().setStatus("error");
    		apiResponse.get().setError(new ErrorType());
    		apiResponse.get().getError().setCode(Constants.Error.GATEWAY.INTERNAL_SERVER_ERROR);
    		apiResponse.get().getError().setMessage(Constants.Error.map.get(Constants.Error.GATEWAY.INTERNAL_SERVER_ERROR));
    	} catch(InvalidParameterException e) {
    		
    		//rollback the database changes
    		try {
				MySQL.rollback();
			} catch (FatalException e1) {
				//wut.
				e1.printStackTrace();
			}    		
		
	    	//log unsuccessful api calls
    		try {
    			//(new Event(apiRequest.getCustomer(), apiRequest.getUser(), apiRequest.getDevice(), apiRequest.getAddress(), methodName, "invalid parameter error - code: " + e.code, apiRequest.isAdminWrite(), apiRequest.getAdminSecretKey())).saveAsync();	    				
				MySQL.commit();
    		}catch(FatalException e1) {
    			logger.error("Caught FatalException while create LogEvent about method failure");
    			logger.error(e1);
    		}		
		
    		//this is a pretty message with a well-known error code
    		logger.warn(e.getMessage());	//just warn us in the logs, it's not a goof on our part.  ha. goof.
    		apiResponse.get().setStatus("error");
    		apiResponse.get().setError(new ErrorType());
    		apiResponse.get().getError().setCode(e.code);
    		apiResponse.get().getError().setMessage(e.getMessage());
    	}finally {		
    		
    		if(!canReturnResponse) {
    			out.close();
        		return;
    		}
			
			//elapsed time
			int elapsed = (int) (System.currentTimeMillis() - requestStartTs);
			apiResponse.get().setElapsed(elapsed);
			logger.info("-- End Method " + methodName + OutputUtil.getElapsedString() + " --");
			
    		if(!apiResponse.canGenerateResponse()) {
    			out.close();
    			return;
    		}

			if(newRequest) {
				//finally, generate the response
		    	try {
		    		//default is XML, can also be JSON
		    		if(StringUtil.isNullOrEmpty(responseFormat)) {
		    			responseFormat = "xml";
		    		}
		    		
		    		Object rootResponseElement = null;
	    			rootResponseElement = apiResponse.get();
	    			JAXBElement rootJaxbResponseElement;
	    			rootJaxbResponseElement = new JAXBElement<ResponseType>(new QName("Response"), ResponseType.class, (ResponseType)rootResponseElement);
		    		
		    		if(responseFormat.equalsIgnoreCase("json")){
		    			JSONUtil.marshal(apiRequest, rootResponseElement, out);
		    		}else if(responseFormat.equalsIgnoreCase("csv")){
						CSVUtil.marshal(apiRequest, rootJaxbResponseElement, out);	    			
		    		}else {
						JAXBUtil.marshal(apiRequest, rootJaxbResponseElement, out);
		    		}
		    		
				} catch (FatalException e) {
		    		//this is the worse-possible situation. we show the user a default error page
		    		e.printStackTrace();
		    		logger.error(e);
		    		apiResponse.getHttpServletResponse().sendError(500, "We're having a bit of a problem with that request. We're working on it - please email customer support or check status."+Constants.HOST_TLD+".com for updates");
				}			
				out.close();
			}
    	}
    	return;
    }

	protected void validateRequest(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {
		
		boolean requiresAdminAuth = false;
		Pattern pattern = null;
		Matcher matcher = null;
		for(String methodPatternString : Constants.Request.methodFilters.get(Constants.Request.METHOD_FILTER_REQUIRES_ADMIN_AUTHENTICATION)){
			pattern = Pattern.compile(methodPatternString);
			matcher = pattern.matcher(request.getMethodName());
			requiresAdminAuth = matcher.find();
			if(requiresAdminAuth) {
		    	logger.debug("Method " + request.getMethodName() + " requires authentication (matched: " + methodPatternString + ")");
				break;
			}
		}
		boolean requiresApiKey = true;
		for(String methodPatternString : Constants.Request.methodFilters.get(Constants.Request.METHOD_FILTER_NO_API_REQUIRED)){
			pattern = Pattern.compile(methodPatternString);
			matcher = pattern.matcher(request.getMethodName());
			requiresApiKey = !matcher.find();
			if(!requiresApiKey) {
		    	logger.debug("Method " + request.getMethodName() + " does not require an apiKey (matched: " + methodPatternString + ")");
				break;
			}
		}	
		boolean requiresSignature = false;
		for(String methodPatternString : Constants.Request.methodFilters.get(Constants.Request.METHOD_FILTER_REQUIRES_SIGNATURE)){
			pattern = Pattern.compile(methodPatternString);
			matcher = pattern.matcher(request.getMethodName());
			requiresSignature = matcher.find();
			
			if(requiresSignature) {
		    	logger.debug("Method " + request.getMethodName() + " requires a signature (matched: " + methodPatternString + ")");
				break;
			}
		}	
		
		boolean requiresAdminSignature = false;
		for(String methodPatternString : Constants.Request.methodFilters.get(Constants.Request.METHOD_FILTER_REQUIRES_ADMIN_SIGNATURE)){
			pattern = Pattern.compile(methodPatternString);
			matcher = pattern.matcher(request.getMethodName());
			requiresAdminSignature = matcher.find();
			if(requiresAdminSignature) {
		    	logger.debug("Method " + request.getMethodName() + " requires an ADMIN signature (matched: " + methodPatternString + ")");
				break;
			}
		}		
		
		//get the user
		request.setUserFromRequest();
		if(request.getUser() != null) {
			response.get().setUserId(request.getUser().getUserId());
		}
		
		//get the customer
		/*
		request.setCustomerFromRequest();
		if(request.getCustomer() != null) {
			response.get().setApiKey(request.getCustomer().getApiKey());
		}else {
			//no api key given
			if(requiresApiKey) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_REQUEST_MISSING_APIKEY);
			}
		}
		*/
		
		
		/*
		 * 
		 * validate a signature
		 *if the method filter specifies that this method doesn't require an apiKey, let that rule supersede requiring a signature
		 *if the method requires an admin signature as well - use the admin signature instead
		 *admin signatures work for any request - so try those first
		 *
		 */
		if((requiresSignature && requiresApiKey) || requiresAdminSignature) {

			String apiKey = request.getParameter(Constants.Request.API_KEY, false);
			Long customerId = request.getParameterLong(Constants.Request.CUSTOMER_ID, false);
			String signature = request.getParameter(Constants.Request.SIGNATURE, true);
			long timestamp = request.getParameterLong(Constants.Request.TIMESTAMP, true);
			String queryString = RemoteRequestUtil.getQueryString(request);

			//signature MUST be at the end of the queryString
			queryString = queryString.replaceFirst("(&signature=.*)&?", "");

			signature = SecurityUtil.cleanHash(signature);
			
			String dataForSignature = (customerId == null ? apiKey : customerId)+"|"+timestamp+"|"+queryString;
			String targetSignature = null;
			
			boolean signatureIsValid = false;
			
			for(String adminSecretKey : Constants.Request.ADMIN_SECRET_KEYS) {
				targetSignature = SecurityUtil.calculateSignature(dataForSignature, adminSecretKey);
				/*
				 * if(GatewayServlet.isSandbox()) {
				 *	logger.debug("Testing secretKey=" + adminSecretKey.getSecretKeyId() + ", generatedSig=" + targetSignature);
				 *}
				 */
				if(targetSignature.equals(signature)) {
					signatureIsValid = true;
					request.setSecretKey(adminSecretKey);
					logger.debug("Signature matched an admin secret key");
					break;
				}
			}
				
			if(!signatureIsValid && !requiresAdminSignature) {
				//try the customer secret key
				String secretKey = null;//TODO: enable request.getCustomer().getSecretKey();
				targetSignature = SecurityUtil.calculateSignature(dataForSignature, secretKey);
				if(targetSignature.equals(signature)) {
					request.setRequiresAdmin(true);
					request.setSecretKey(secretKey);
					logger.debug("Signature matched the customer secret key");
					signatureIsValid = true;
				}
			}

			if(!signatureIsValid) {
				logger.warn("Invalid signature. signature="+signature+", expected="+targetSignature+", data="+dataForSignature);
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_SIGNATURE);				
			}

			long now = Calendar.getInstance().getTimeInMillis()/1000;
			if(now + 300 < timestamp) {
				//5 minute leeway - this prevents forward dating a request
				logger.warn("Invalid signature timestamp (in the future). timestamp="+timestamp + ", now="+now + ", diff="+(timestamp-now)+" seconds");
				throw new InvalidParameterException(Constants.Error.GATEWAY.SIGNATURE_INVALID_SIGNATURE_IN_THE_FUTURE, ListUtil.from((timestamp-now)+""));								
			}
			if(now - timestamp > 300) {
				//requests are only valid for 5 minutes after generating the signature
				logger.warn("Invalid signature timestamp (created too long ago). timestamp="+timestamp + ", now="+now + ", diff="+(now-timestamp)+" seconds");
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_SIGNATURE_EXPIRED, ListUtil.from((now-timestamp)+""));								
			}
			
			logger.info("Signature is valid");
		}		
		
		if(requiresAdminSignature) {
			request.setRequiresAdmin(true);
		}
	}
		
	protected void validateRequestParameters(APIRequest request, APIResponse response) throws InvalidParameterException, FatalException {

		List<MethodParameter> parameters = Constants.methodParameters.get(request.getMethodName());
		for(MethodParameter parameter : parameters) {
			String value = null;
			String parameterType = parameter.getType();
			boolean positiveOnly = false;
			if(parameterType.endsWith("+")) {
				parameterType = parameterType.substring(0, parameterType.length()-1);
				positiveOnly = true;
			}
			
			try {
				//this will fail if it's required and not present
				value = request.getParameter(parameter.getName(), parameter.isRequired());
			}catch(InvalidParameterException e) {
				if(parameterType.equals("UserIdentifier")) {
					//must be a userId, phoneNumber, etc in the request - even if not matching this name
					Long userId = request.getParameterLong(Constants.Request.USER_ID, false);
					if(userId == null) {
						throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));							
					}

				}else if(parameterType.equals("CustomerIdentifier")) {
					//must be a customerId or apiKey in the request - even if not matching this name
					Long customerId = request.getParameterLong(Constants.Request.CUSTOMER_ID, false);
					String apiKey = request.getParameter(Constants.Request.API_KEY, false);
					if(StringUtil.isNullOrEmpty(apiKey) && customerId == null) {
						throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));							
					}

				}else {
					throw e;
				}
			}

			//default values will be read from getParameter()
			if(parameter.getDefaultValue() != null) {
				request.setDefaultParameter(parameter.getName(), parameter.getDefaultValue());
				if(value == null) {
					value = parameter.getDefaultValue();
				}
			}
			
			//validate the type
			try {
				if(!StringUtil.isNullOrEmpty(value)) {
					if(parameterType.equals("String")) {
						//we're good to go
					}else if(parameterType.equals("UserIdentifier")) {
						//we're good to go
					}else if(parameterType.equals("CustomerIdentifier")) {
						//we're good to go
					}else if(parameterType.equals("Integer")) {
						Integer intVal = null;
						try {
							intVal = Integer.parseInt(value);	//android sends over integers that are doubles... (eg. 5.0 instead of 5) - so we allow it and cast later
						}catch(NumberFormatException e) {
							double doubleValue = Double.parseDouble(value);
							if(Math.floor(doubleValue) != doubleValue) {
								throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));
							}
							intVal = (int)doubleValue;
						}
						if(positiveOnly && intVal < 0) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), "Positive " + parameterType, value));															
						}
						
					}else if(parameterType.equals("Long") || parameterType.equals("Timestamp")) {
						Long longVal = null;
						try {
							longVal = Long.parseLong(value);						
						}catch(NumberFormatException e) {
							double doubleValue = Double.parseDouble(value);
							if(Math.floor(doubleValue) != doubleValue) {
								throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));
							}
							longVal = (long)doubleValue;
						}
						
						if(positiveOnly && longVal < 0) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), "Positive " + parameterType, value));															
						}						
						
					}else if(parameterType.equals("Double")) {
						Double doubleVal = Double.parseDouble(value);
						
						if(positiveOnly && doubleVal < 0) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), "Positive " + parameterType, value));															
						}
						
					}else if(parameterType.equals("Float")) {
						Float floatVal = Float.parseFloat(value);
						
						if(positiveOnly && floatVal < 0) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), "Positive " + parameterType, value));															
						}
						
					}else if(parameterType.equals("Boolean")) {
						//parse boolean always succeeds - let's force the value to be true or false
						if(!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
							throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));
						}
					}else if(parameterType.equals("Date")) {
						DateUtil.parse(value);
					}else if(parameterType.equals("Time")) {
						(new SimpleDateFormat("hh:mm a")).parse(value);
					}else if(parameterType.equals("JSONArray")) {
						try {
							new JSONArray(value);
						}catch(JSONException e) {
							throw new InvalidParameterException(Constants.Error.GENERAL.INVALID_JSON, ListUtil.from(parameter.getName(), e.getMessage()));
						}
					}else if(parameterType.equals("JSONObject")) {
						try {
							new JSONObject(value);
						}catch(JSONException e) {
							throw new InvalidParameterException(Constants.Error.GENERAL.INVALID_JSON, ListUtil.from(parameter.getName(), e.getMessage()));
						}
					}
				}
			}catch(NumberFormatException e) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));				
			} catch (ParseException e) {
				throw new InvalidParameterException(Constants.Error.GATEWAY.INVALID_PARAMETER_TYPE, ListUtil.from(parameter.getName(), parameterType, value));				
			}
		}
	}
}
