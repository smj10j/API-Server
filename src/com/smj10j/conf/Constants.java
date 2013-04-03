package com.smj10j.conf;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.smj10j.model.MethodParameter;

public abstract class Constants {

	public static final String HOST_TLD = "server.com";
	public static final String API_SERVER_BASE = "api."+HOST_TLD;
	public static final String API_VERSION = "1";
	public static final boolean DEBUG_MODE = false;
	public static String SERVLET_CONTEXT_PATH = null;

	public static final String[] memcacheServers = { "localhost:11211" };

	public static class Boolean {
		public static final String YES = "true";
		public static final String NO = "false";		
	}
	
	public static class Date {
		public static final String FORMAT = "yyyy-MM-dd";		
	}
	
	// Seconds
	public static class Time {
		public static final long MINUTE = 60;
		public static final long HOUR = MINUTE * 60;
		public static final long DAY = HOUR * 24;
	}

	// Meters
	public static class Distance {
		public static final double RADIUS_OF_EARTH = 6371010.0; // in meters
		public static final double FEET_PER_METER = 3.2808;
		public static final double METERS_PER_MILE = 1609.344;

		public static final int DEFAULT_CHECKIN = 1000;
		public static final int NEARBY = (int) (METERS_PER_MILE * 25.0);
	}

	public static class Path {
		public static final String JaxbPackage = "com.smj10j.jaxb";
	}
	
	public static class Request {

		/* Generally required in all requests */
		public static final String METHOD = "method";
		public static final String API_KEY = "apiKey";
		public static final String RESPONSE_FORMAT = "responseFormat";
		public static final String JSCALLBACK = "jsCallback";
		public static final String ASYNC_TOKEN = "asyncToken";
		public static final String VALUE = "value";
		public static final String ADMIN_EMAIL = "adminEmail";
		public static final String ADMIN_PASSWORD = "adminPassword";
		public static final String SIGNATURE = "signature";
		public static final String TIMESTAMP = "timestamp";


		/* Permission */
		public static final String PUBLIC = "public";
		public static final String PRIVATE = "private";

		/* General */
		public static final String DATA = "data";
		public static final String TOKEN = "token";
		public static final String EMAIL = "email";
		public static final String PASSWORD = "password";
		public static final String ENABLED = "enabled";
		public static final String AMOUNT = "amount";
		public static final String URL = "url";
		public static final String FORMAT = "format";
		public static final String TYPE = "type";
		public static final String ASYNC = "async";

		/* IDs */
		public static final String USER_ID = "userId";
		public static final String CUSTOMER_ID = "customerId";
		

		/* Secret keys for admin users */
		public static Set<String> ADMIN_SECRET_KEYS = new HashSet<String>();
		
		
		/* Whitelisted Servers [IP => Friendly Hostname] */
		public static Map<String, String> WHITELISTED_SERVERS = new HashMap<String, String>();
		
		/* Set by the initialization servlet */
		public static Map<String, List<String>> methodFilters = new HashMap<String, List<String>>();
		public static final String METHOD_FILTER_NO_API_REQUIRED = "NoApiKeyRequired";
		public static final String METHOD_FILTER_REQUIRES_ADMIN_AUTHENTICATION = "RequiresAdminAuthentication";
		public static final String METHOD_FILTER_REQUIRES_SIGNATURE = "RequiresSignature";
		public static final String METHOD_FILTER_REQUIRES_ADMIN_SIGNATURE = "RequiresAdminSignature";
	}
	

	public static class Error {

		public static final class GENERAL {
			public static final int INVALID_JSON = 100;
		}				

		public static final class PERMISSIONS {
			public static final int INVALID_AUTHENTICATION = 200;
		}

		public static final class INVALID_ID {
			public static final int API_KEY = 400;
			public static final int USER_ID = 401;
			public static final int CUSTOMER_ID = 402;
		}

		public static final class GATEWAY {
			public static final int INTERNAL_SERVER_ERROR = 500;
			public static final int INVALID_METHOD = 501;
			public static final int INVALID_REQUEST_MISSING_APIKEY = 502;
			public static final int MISSING_REQUIRED_PARAMETER = 503;
			public static final int METHOD_NOT_YET_IMPLEMENTED = 504;
			public static final int INVALID_PARAMETER_TYPE = 505;
			public static final int ALL_METHODS_DISABLED_TEMPORARILY = 506;
			public static final int METHOD_DISABLED_TEMPORARILY = 507;
			public static final int INVALID_AUTHENTICATION = 508;
			public static final int INVALID_SIGNATURE = 509;
			public static final int INVALID_SIGNATURE_EXPIRED = 510;
			public static final int SIGNATURE_INVALID_SIGNATURE_IN_THE_FUTURE = 511;
			public static final int SERVER_NOT_WHITELISTED = 512;
			public static final int INVALID_REQUEST_FORMAT = 513;
		}

		public static final class EMAIL {
			public static final int INVALID_BYTE_STREAM = 1000;
		}
		
		public static final Map<Integer, String> map = new LinkedHashMap<Integer, String>();
		static {

			map.put(GENERAL.INVALID_JSON, "Invalid JSON for parameter %s. The JSON parser said \"%s\"");

			map.put(PERMISSIONS.INVALID_AUTHENTICATION, "The credentials you provided were incorrect.");

			map.put(INVALID_ID.API_KEY, "Invalid Customer API Key - %s");
			map.put(INVALID_ID.USER_ID, "Invalid User Id - %s");			
			
			map.put(GATEWAY.INVALID_METHOD, "Invalid method name - %s");
			map.put(GATEWAY.INVALID_REQUEST_MISSING_APIKEY, "An apiKey is required for this request.");
			map.put(GATEWAY.MISSING_REQUIRED_PARAMETER, "Missing required parameter - %s");
			map.put(GATEWAY.METHOD_NOT_YET_IMPLEMENTED, "Method not yet implemented - %s");
			map.put(GATEWAY.INVALID_PARAMETER_TYPE, "Invalid parameter type for parameter %s - we expected type %s and you provided value \"%s\"");
			map.put(GATEWAY.ALL_METHODS_DISABLED_TEMPORARILY, "All API access is temporarily disabled. We will resume service shortly! Please contact customer support for more information.");
			map.put(GATEWAY.METHOD_DISABLED_TEMPORARILY, "Access to this API method is temporarily disabled. We will resume service shortly! Please contact customer support for more information.");
			map.put(GATEWAY.INVALID_AUTHENTICATION, "The credentials you provided were incorrect.");
			map.put(GATEWAY.INVALID_SIGNATURE, "Invalid signature for the given apiKey, request parameters, and timestamp");
			map.put(GATEWAY.INVALID_SIGNATURE_EXPIRED, "Invalid signature - it has expired. Please set your clock forward %s seconds");
			map.put(GATEWAY.SIGNATURE_INVALID_SIGNATURE_IN_THE_FUTURE, "Invalid signature - it is in the future. Please set your clock back %s seconds");
			map.put(GATEWAY.SERVER_NOT_WHITELISTED, "This request is only allowed from trusted servers. Request came from IP %s");
			map.put(GATEWAY.INVALID_REQUEST_FORMAT, "Invalid request format - %s");
			map.put(GATEWAY.INTERNAL_SERVER_ERROR, "Sorry, but we're having some problems. Our engineers are working on it pronto.");

			map.put(EMAIL.INVALID_BYTE_STREAM, "You input an invalid byte stream to convert.");
		}	
		
	}

	public static Map<String, Method> allMethods = new ConcurrentHashMap<String, Method>();
	public static Map<String, Method> privateMethods = new ConcurrentHashMap<String, Method>();
	public static Map<String, Method> publicMethods = new ConcurrentHashMap<String, Method>();
	public static Map<String, String> methodDescriptions = new ConcurrentHashMap<String, String>();
	public static Map<String, List<MethodParameter>> methodParameters = new ConcurrentHashMap<String, List<MethodParameter>>();
	
	
	
}

