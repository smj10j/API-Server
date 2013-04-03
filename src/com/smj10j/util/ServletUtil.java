package com.smj10j.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class ServletUtil {

	private static Logger logger = Logger.getLogger(ServletUtil.class);
	
	private static final int RATE_LIMIT_PERIOD = Cache.lifetime.FIVE_MINUTE;
	private static final int RATE_LIMIT_COOLDOWN_PERIOD = Cache.lifetime.HOUR;
	private static final int DEFAULT_USER_RATE_LIMIT = 300;	//PER 5 MINUTES --- defaults to 1 request/second
	private static final Map<String, Integer> userRateLimitByActionName = new HashMap<String, Integer>();
	private static final Map<String, Integer> userEmailRateLimitByActionName = new HashMap<String, Integer>();
	static {
		userRateLimitByActionName.put("user.save", 20);					//20 is 1 request every 15 seconds
		userRateLimitByActionName.put("ServletAuth", 20);
		
		userEmailRateLimitByActionName.put("user.save", 5);
		userEmailRateLimitByActionName.put("ServletAuth", 5);
	}

	/*
	public static boolean isRateLimitHit(Customer customer, User user, String actionName) {

		try {
			
			//Rate Limit Filter: limits an apiKey's ability to spam certain requests
			
			String requestKey = (customer == null ? "" : customer.getCustomerId())+"|"+(user == null ? "" : user.getUserId())+"|"+actionName;
			
			Boolean rateLimitHitEarlier = Cache.get(requestKey, Cache.namespace.RATE_LIMIT_HIT_FOR_CUSTOMER_USER_METHOD);
			if(rateLimitHitEarlier != null && rateLimitHitEarlier) {
				//we're in a cooldown phase, abort!
				logger.warn("RATE LIMIT THRESHOLD HIT AGAIN DURING COOLDOWN PHASE!  requestKey="+requestKey);
				return true;					
			}
			
			Integer requestCount = Cache.get(requestKey, Cache.namespace.RATE_LIMIT_CUSTOMER_USER_METHOD);
			if(requestCount == null) {
				requestCount = 0;
			}
			
			int emailRateLimit = DEFAULT_USER_RATE_LIMIT;
			if(userEmailRateLimitByActionName.containsKey(actionName)) {
				emailRateLimit = userEmailRateLimitByActionName.get(actionName);
			}
			
			int rateLimit = DEFAULT_USER_RATE_LIMIT;
			if(userRateLimitByActionName.containsKey(actionName)) {
				rateLimit = userRateLimitByActionName.get(actionName);
			}
			
			//email a notice to admins - this should only happen once per cooldown
			if(requestCount == emailRateLimit) {	
				try {
	
					String subject = "Rate Limit Hit on "+GatewayServlet.getHostname()+"!";
					String body = "Customer: " + (customer == null ? "NONE" : customer.getApiKey()) + "\n" +
								  "User: " + (user == null ? "NONE" : user.getUserId()) + "\n" + 
								  "Action: " + actionName + "\n" + 
								  "Time: " + (new Date()).toString();
					
					EmailUtil.email(null, "warn-notifier", ListUtil.from(EmailUtil.getInternalAdminEmail()), subject, body.getBytes(), "txt", null);

				} catch (FatalException e) {
					//oh bonkers
	    			logger.error("Error while trying to email admins about rate limit being hit!", e);	    			
				}
			}				
			
			
			if(requestCount >= rateLimit) {	
				Cache.put(true, RATE_LIMIT_COOLDOWN_PERIOD, requestKey, Cache.namespace.RATE_LIMIT_HIT_FOR_CUSTOMER_USER_METHOD);
				logger.warn("RATE LIMIT THRESHOLD HIT!  requestKey="+requestKey+", requestCount=" + requestCount + "/" + rateLimit);
				return true;
			}
			
			requestCount++;
			Cache.put(requestCount, RATE_LIMIT_PERIOD, requestKey, Cache.namespace.RATE_LIMIT_CUSTOMER_USER_METHOD);
			logger.debug("requestKey="+requestKey+", requestCount=" + requestCount + "/" + rateLimit);

		} catch (FatalException e) {
			logger.error(e);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}finally {

		}
		return false;
	}
*/
}
