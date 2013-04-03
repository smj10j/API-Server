package com.smj10j.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.jaxb.ResponseType;
import com.smj10j.util.JSONUtil;


public class APIResponse implements Serializable {

	private static final long serialVersionUID = 3546783458653931353L;
    
	private static Logger logger = Logger.getLogger(APIResponse.class);

    private ResponseType responseType;
	private boolean isBatchRequest;
	private HttpServletResponse httpServletResponse;
	private PrintWriter printWriter;
	private boolean canGenerateResponse;
		
	public APIResponse(HttpServletResponse httpServletResponse) {
		setHttpServletResponse(httpServletResponse);
		responseType = new ResponseType();
		this.canGenerateResponse = true;
	}
	
	public APIResponse(ResponseType responseType) {
		this.responseType = responseType;
		this.canGenerateResponse = true;
	}
	
	public String toString() {
		StringWriter responseWriter = new StringWriter();
		try {
			JSONUtil.marshal(null, get(), responseWriter);
		} catch (FatalException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		return responseWriter.toString();
	}
	
	public ResponseType get() {
		return responseType;
	}
	
	public void setHttpServletResponse(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	public HttpServletResponse getHttpServletResponse() {
		return httpServletResponse;
	}
	
	public PrintWriter getWriter() throws IOException {
		if(printWriter == null) {
			printWriter = getHttpServletResponse().getWriter();
		}
		return getHttpServletResponse().getWriter();
	}

	public void setBatchRequest(boolean isBatchRequest) {
		this.isBatchRequest = isBatchRequest;
	}

	public boolean isBatchRequest() {
		return isBatchRequest;
	}
	
	public boolean canGenerateResponse() {
		return canGenerateResponse;
	}

	public void disableOutput() {
		this.canGenerateResponse = false;
	}
}
