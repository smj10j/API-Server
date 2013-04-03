package com.smj10j.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


public final class HTTPHeaderFilter implements Filter {
	
	private FilterConfig filterConfig = null;
	private static Logger logger = Logger.getLogger(HTTPHeaderFilter.class);

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
		this.filterConfig = null;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (filterConfig == null)
			return;

		HttpServletResponse httpResponse = (HttpServletResponse)response;
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		
		// add in any headers that all responses should add
        httpResponse.addHeader("Content-Type", "text/plain");
        httpResponse.addHeader("Access-Control-Allow-Origin", "*");
        httpResponse.addHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST");
        httpResponse.addHeader("Access-Control-Allow-Headers", "XMLHttpRequest, X-Requested-With, Content-Type, Accept, Accept-Charset, Cache-Control, Connection, Cookie, Host, User-Agent");
        
		chain.doFilter(request, response);
	}
	  
}
