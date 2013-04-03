package com.smj10j.test;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;

import com.smj10j.conf.FatalException;


public abstract class SimpleTestBase {
	
	protected TestServer testServer;
	
    @Before
    public void setUp() {
    	PropertyConfigurator.configure("conf/log4j.xml");
    	
    	testServer = new TestServer(
    		//use defaults
    	);
    }

    @After
    public void tearDown() throws FatalException {

    }

}
