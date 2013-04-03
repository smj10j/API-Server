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
import org.json.JSONException;
import org.json.JSONObject;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.APIRequest;
import com.smj10j.model.User;
import com.smj10j.servlet.GatewayServlet;

public final class RateLimitFilter implements Filter {
	
	private FilterConfig filterConfig = null;
	private static Logger logger = Logger.getLogger(RateLimitFilter.class);
	
	private static final JSONObject errorResponse = new JSONObject();
	static {
		try {
			JSONObject response = new JSONObject();
			JSONObject error = new JSONObject();
			error.put("code", 500);
			error.put("message", "Rate Limit Hit - Please try again later");
			response.put("error", error);
			response.put("status", "error");
			errorResponse.put("response", response);
		} catch (JSONException e) {
			logger.error("FAILED TO CREATE DEFAULT JSON ERROR RESPONSE IN RATE LIMIT FILTER");
		}
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
		this.filterConfig = null;
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		
		if (filterConfig == null) {
			return;
		}
		
		if(GatewayServlet.isSandbox()) {
			chain.doFilter(servletRequest, servletResponse);
			return;
		}
		
		HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
		APIRequest request = new APIRequest(httpServletRequest);

		/*
		 * TODO: Rate limit by deviceId when no userId is present
		 * We have to be careful to get the right number here, but adding in a deviceId 
		 * rate limit will prevent server harm from negligent setups (like tablet circular references)
		 */
		try {
			request.setUserFromRequest();
			
			User user = request.getUser();
			String methodName = request.getParameter(Constants.Request.METHOD);
			/*Customer customer = request.getCustomer();
			
			if(customer != null && user != null) {
				if(ServletUtil.isRateLimitHit(customer, user, methodName)) {
					logger.warn("Aborting request");
					HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
					PrintWriter out = httpServletResponse.getWriter();
					out.write(errorResponse.toString());
					out.close();
					return;
				}
			}
			*/
			
		} catch (FatalException e) {
			logger.error(e);
			e.printStackTrace();
		} catch (InvalidParameterException e) {
			
			/*HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
			PrintWriter out = httpServletResponse.getWriter();
			out.write(e.toString());
			out.close();*/
			
			logger.error(e);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}finally {

		}

		chain.doFilter(servletRequest, servletResponse);
	}
	  
}
