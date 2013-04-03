package com.smj10j.util;

import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.InvalidParameterException;

public class WhitelistUtil {
	
	private static Logger logger = Logger.getLogger(WhitelistUtil.class);


	/*
	 * Trusts servers in the local network and *.server.com
	 */
	public static void validate(String ip) throws InvalidParameterException {
		if(!ip.equals("127.0.0.1") && !ip.startsWith("10.")) {
			if(Constants.Request.WHITELISTED_SERVERS.get(ip) == null) {
				logger.warn("Attempt made to access the AsynchronousLoggingServlet from an untrusted server! Request from " + ip);
				throw new InvalidParameterException(Constants.Error.GATEWAY.SERVER_NOT_WHITELISTED, ListUtil.from(ip));
			}
		}
	}
}
