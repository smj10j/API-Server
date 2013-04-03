package com.smj10j.conf;

import java.util.List;

import com.smj10j.util.OutputUtil;

public class InvalidParameterException extends Exception {

	private static final long serialVersionUID = 4239388505566319863L;

	public int code;
	
	public InvalidParameterException(Exception e) {
		super(e);
	}

	public InvalidParameterException(String err) {
		super(err); 
	}

	public InvalidParameterException(String err, Exception e) {
		super(err,e); 
	}
	
	public InvalidParameterException(int code, List<String> tokens, Exception e) {
		super(OutputUtil.tokenReplace(Constants.Error.map.get(code),"%s", tokens), e);
		this.code = code;
	}	
	
	public InvalidParameterException(int code, List<String> tokens) {
		super(OutputUtil.tokenReplace(Constants.Error.map.get(code),"%s", tokens));
		this.code = code;
	}
	
	public InvalidParameterException(int code, Exception e) {
		super(Constants.Error.map.get(code), e);
		this.code = code;
	}
	
	public InvalidParameterException(int code) {
		super(Constants.Error.map.get(code));
		this.code = code;
	}
}
