package com.smj10j.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.APIRequest;
import com.smj10j.util.RemoteRequestUtil;


public final class LoggingFilter implements Filter {
	
	private FilterConfig filterConfig = null;
	private static Logger logger = Logger.getLogger(LoggingFilter.class);

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
		this.filterConfig = null;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (filterConfig == null)
			return;

		HttpServletRequest httpRequest = (HttpServletRequest)request;
		
		String queryString = null;
		try {
			APIRequest apiRequest = new APIRequest(httpRequest);
			queryString = RemoteRequestUtil.getQueryString(apiRequest);
		}  catch (InvalidParameterException e) {
			logger.error("Request: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI() + " -- FAILED TO PARSE QUERY STRING -- ");
		}
		
		logger.info("Request: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI() + "?" + queryString);
		

		chain.doFilter(request, response);
	}
	  
}
