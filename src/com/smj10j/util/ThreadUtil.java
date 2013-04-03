package com.smj10j.util;

import com.smj10j.conf.FatalException;


public abstract class ThreadUtil {

	public static Thread create(Class<? extends Runnable> clazz) throws FatalException {
		try {
			return new Thread(clazz.newInstance());
		} catch (InstantiationException e) {
			throw new FatalException(e);
		} catch (IllegalAccessException e) {
			throw new FatalException(e);
		}
	}
	public static Thread create(Runnable thread) throws FatalException {
		return new Thread(thread);
	}	
    public static void start(Thread thread) throws FatalException {
        thread.start();    	
    }
    public static void stop(Thread thread) throws FatalException {
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new FatalException(e);
		}
    }
	
	
	
	
	
	
	
	
			
}
