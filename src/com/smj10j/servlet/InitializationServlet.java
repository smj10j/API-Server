package com.smj10j.servlet;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.model.APIRequest;
import com.smj10j.model.APIResponse;
import com.smj10j.model.MethodParameter;
import com.smj10j.util.Cache;
import com.smj10j.util.StringUtil;

public class InitializationServlet extends HttpServlet{

	private static final long serialVersionUID = 5190463215246060414L;
	private static Logger logger = Logger.getLogger(InitializationServlet.class);
	private static Logger stdErrLoggingProxy = Logger.getLogger("STDOUT Proxy");
	
	public void init() {
		
		if(StringUtil.isNullOrEmpty(Constants.SERVLET_CONTEXT_PATH)) {
			Constants.SERVLET_CONTEXT_PATH = getServletContext().getRealPath("/");
		}
		
		//setup log4j
		initializeLog4J();
		
		//examine services.xml to determine the available api methods
		initializeServices();
		
		//examine services.xml to determine the filters for methods 
		setupMethodFilters();
		
		//initialize memcache
		initializeCache();

		//attached stderr and stdout so we can log them to file
		StdOutErrLog.tieSystemOutAndErrToLog();
		
		logger.info("-- Initialization Complete --");			
	}
	
	private void initializeLog4J() {
		String prefix = Constants.SERVLET_CONTEXT_PATH;
		String file = "WEB-INF/"+"log4j.xml";
    	logger.info("Loading log4j configuration from " + prefix+file);
    	PropertyConfigurator.configure(prefix+file);
	}
	
	private void initializeCache() {

		try {
			Cache.init();
			Cache.clear();
		} catch (FatalException e) {
			logger.fatal("Could not initialize cache - " + e.getMessage());
			logger.info("Aborting startup");
    		System.exit(1);				
		}
		logger.info("Cache Initialization Complete.");											
		
	}
	
	
	private void initializeServices() {
		String prefix = Constants.SERVLET_CONTEXT_PATH;
		String file = "WEB-INF/"+"services.xml";
    	logger.info("Loading method list from " + prefix+file);

    	Constants.allMethods = new ConcurrentHashMap<String, Method>();
    	Constants.privateMethods = new ConcurrentHashMap<String, Method>();
    	Constants.publicMethods = new ConcurrentHashMap<String, Method>();
    	Constants.methodDescriptions = new ConcurrentHashMap<String, String>();
    	Constants.methodParameters = new ConcurrentHashMap<String, List<MethodParameter>>();
    	
		
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(prefix+file);
			Element documentElement = dom.getDocumentElement();

			Element publicMethodsElement = (Element) documentElement.getElementsByTagName("PublicMethods").item(0);
			Element privateMethodsElement = (Element) documentElement.getElementsByTagName("PrivateMethods").item(0);
			
			logger.info("-- Initializing Public Methods --");								
	    	Constants.publicMethods = initalizeMethodsFromXMLList(publicMethodsElement);
	    	
			logger.info("-- Initializing Private Methods --");								
	    	Constants.privateMethods = initalizeMethodsFromXMLList(privateMethodsElement);
	    	
	    	Constants.allMethods.putAll(Constants.publicMethods);
	    	Constants.allMethods.putAll(Constants.privateMethods);
	    	
			logger.info("-- Method Initialization Complete --");								
    	
		} catch(ParserConfigurationException e) {
			logger.fatal("Parser Config Problem reading services.xml", e);
			logger.info("Aborting startup");
    		System.exit(1);				
		} catch (SAXException e) {
			logger.fatal("SAX Problem reading services.xml", e);
			logger.info("Aborting startup");
    		System.exit(1);				
		} catch (IOException e) {
			logger.fatal("IOException reading services.xml", e);
			logger.info("Aborting startup");
    		System.exit(1);					
		}
		
    }
	
	private Map<String,Method> initalizeMethodsFromXMLList(Element methodsElement) {

    	Map<String,Method> methods = new ConcurrentHashMap<String, Method>();

    	String methodName = null;
    	String methodDescription = null;
    	String serviceName = null;
		String method = null;
		
		String parameterName = null;
		String parameterType = null;
		boolean parameterRequired;
		String parameterDefaultValue = null;
		String parameterDescription = null;

		try {
			NodeList methodNodeList = methodsElement.getElementsByTagName("Method");
			if(methodNodeList != null && methodNodeList.getLength() > 0) {
				for(int i = 0; i < methodNodeList.getLength(); i++) {
	
					//get the method information
					Element methodElement = (Element)methodNodeList.item(i);
			    	methodName = methodElement.getAttribute("name");
			    	methodDescription = methodElement.getAttribute("description");
			    	serviceName = methodElement.getAttribute("class");
					method = methodElement.getAttribute("method");
					
					//verify and store it
					methods.put(methodName,  Class.forName(serviceName).getMethod(method, APIRequest.class, APIResponse.class));
					Constants.methodDescriptions.put(methodName, methodDescription);
					logger.info("Recognized method " + methodName + " and mapped it to " + serviceName + "." + method + "(APIRequest, APIResponse)");
					
					//lookup all parameters for this method
					Element parametersElement = (Element) methodElement.getElementsByTagName("Parameters").item(0);
					NodeList parameters = parametersElement.getElementsByTagName("Parameter");
					List<MethodParameter> parametersList = new ArrayList<MethodParameter>();
					if(parameters != null && parameters.getLength() > 0) {
						for(int j = 0 ; j < parameters.getLength();j++) {
	
							//get a parameter's information
							Element parameterElement = (Element)parameters.item(j);
							parameterName = parameterElement.getAttribute("name");
							parameterType = parameterElement.getAttribute("type");
							parameterRequired = Boolean.parseBoolean(parameterElement.getAttribute("required"));
							parameterDefaultValue = parameterElement.getAttribute("default");
							if(parameterType.equals("String") && StringUtil.isNullOrEmpty(parameterDefaultValue)) {
								//put back in nulls
								parameterDefaultValue = null;
							}
							parameterDescription = parameterElement.getAttribute("description");
							
							//store it
							MethodParameter methodParameter = new MethodParameter(parameterName, parameterType, parameterRequired, parameterDefaultValue, parameterDescription);
							parametersList.add(methodParameter);
							
							logger.info("  Recognized parameter " + parameterName + " of type " + parameterType + " default="+parameterDefaultValue + " required="+parameterRequired);								
						}
					}
					//save the list of parameters for this method
					Constants.methodParameters.put(methodName, parametersList);
				}
			}
			
		} catch (SecurityException e) {
			logger.fatal("Unable to invoke method for " + methodName, e);
			logger.info("Aborting startup");
    		System.exit(1);				
		} catch (NoSuchMethodException e) {
			logger.fatal("No method for " + methodName, e);
			logger.info("Aborting startup");
    		System.exit(1);				
		} catch (ClassNotFoundException e) {
			logger.fatal("Could not find class " + serviceName + " when trying to invoke method for " + methodName, e);
			logger.info("Aborting startup");
    		System.exit(1);				
		} 

		return methods;

	}
	
	private void setupMethodFilters() {

		String prefix = Constants.SERVLET_CONTEXT_PATH;
		String file = "WEB-INF/"+"services.xml";
    	logger.info("Loading method filter list from " + prefix+file);

		Constants.Request.methodFilters = new HashMap<String, List<String>>();

    	String filterName = null;
    	String methodRegex = null;
    	
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(prefix+file);
			Element documentElement = dom.getDocumentElement();
			Element methodsElement = (Element) documentElement.getElementsByTagName("MethodFilters").item(0);
			
			NodeList methodFiltersList = methodsElement.getElementsByTagName("MethodFilter");
			if(methodFiltersList != null && methodFiltersList.getLength() > 0) {
				for(int i = 0; i < methodFiltersList.getLength(); i++) {
		
					//get the method information
					Element methodFilterElement = (Element)methodFiltersList.item(i);
			    	filterName = methodFilterElement.getAttribute("name");
			
					//lookup all parameters for this method
					NodeList methodList = methodFilterElement.getElementsByTagName("Method");
					if(methodList != null && methodList.getLength() > 0) {
						for(int j = 0 ; j < methodList.getLength();j++) {

							//get a parameter's information
							Element methodElement = (Element)methodList.item(j);
					    	methodRegex = methodElement.getAttribute("matches");

					    	if(!StringUtil.isNullOrEmpty(methodRegex)) {
						    	List<String> methodFilter = Constants.Request.methodFilters.get(filterName);
						    	if(methodFilter == null) {
						    		methodFilter = new ArrayList<String>();
						    		Constants.Request.methodFilters.put(filterName, methodFilter);
						    	}
						    	methodFilter.add(methodRegex);
	
						    	
								logger.info("Recognized filter for " + filterName + " - " + methodRegex);
					    	}else {
								logger.fatal("Failed to find \"matches\" filter for " + filterName);
								logger.info("Aborting startup");
					    		System.exit(1);
					    	}
						}
					}
				}
			}
    	
		} catch(ParserConfigurationException e) {
			logger.fatal("Parser Config Problem reading services.xml at " + filterName, e);
			logger.info("Aborting startup");
    		System.exit(1);			
		} catch(SAXException e) {
			logger.fatal("SAX Problem reading services.xml at " + filterName, e);
			logger.info("Aborting startup");
    		System.exit(1);			
		} catch(IOException e) {
			logger.fatal("IO Problem reading services.xml", e);
			logger.info("Aborting startup");
    		System.exit(1);			
		} 								
		
		logger.info("-- Method Filter Initialization Complete --");				
		
	}
	
	private static class StdOutErrLog {

	    public static void tieSystemOutAndErrToLog() {
	    	if(System.out.getClass().equals(PrintStream.class)) {
	    		return;
	    	}
	        System.setOut(createLoggingProxy(System.out));
	        System.setErr(createLoggingProxy(System.err));
	    }

	    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
	        return new PrintStream(realPrintStream) {
	            public void print(final String string) {
	                //uncommenting the following line will create a situation where
	            	//successive deployments will write out multiple lines
	            	//realPrintStream.print(string);
	                stdErrLoggingProxy.info(string);
	            }
	        };
	    }
	}

}
