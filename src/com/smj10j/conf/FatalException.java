package com.smj10j.conf;

public class FatalException extends Exception {

	private static final long serialVersionUID = 4239388505566319863L;

	public FatalException(Exception e) {
		super(e);
	}

	public FatalException(String err) {
		super(err); 
	}

	public FatalException(String err, Exception e) {
		super(err,e); 
	}
}
