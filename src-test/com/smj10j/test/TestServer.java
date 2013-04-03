package com.smj10j.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.model.APIResponse;
import com.smj10j.util.JAXBUtil;
import com.smj10j.util.ListUtil;
import com.smj10j.util.SecurityUtil;
import com.smj10j.util.StringUtil;

public class TestServer {

	private static Logger logger = Logger.getLogger(TestServer.class);

	private String protocol = "http";	//TODO: Testing: use https after i figure out how to add a cert
	private String server = "api.myserver.com";//changed for testing.
	private String appPath = "mywar";
	private String version = "";
	private String servlet = "api";
	private String secretKey = "62458(84594gkjhk;43&P56679g,i9646534(*3/-junit";
	
	public TestServer() {
		
	}
	
	public TestServer(String protocol, String server, String version, String servlet, String secretKey) {
		if(protocol != null) this.protocol = protocol;
		if(server != null) this.server = server;
		if(version != null) this.version = version;
		if(servlet != null) this.servlet = servlet;
		if(secretKey != null) this.secretKey = secretKey;
	}
	
	public String getBaseUrl() {
		return protocol + "://" + server + "/" + appPath + version + "/" + servlet;
	}

	public APIResponse get(NameValuePair... parameters) throws FatalException {
		List<NameValuePair> parametersList = addSignatureAndTestParameters(parameters);
		return new APIResponse(JAXBUtil.unmarshal(get(getBaseUrl(), getQueryString(parametersList))));
	}
	
	public APIResponse post(NameValuePair... parameters) throws FatalException {
		List<NameValuePair> parametersList = addSignatureAndTestParameters(parameters);
		return new APIResponse(JAXBUtil.unmarshal(post(getBaseUrl(), parametersList.toArray(parameters))));
	}
	
	private List<NameValuePair> addSignatureAndTestParameters(NameValuePair[] parameters) throws FatalException {
		List<NameValuePair> parametersList = ListUtil.from(parameters);
		String apiKey = null;
		for(NameValuePair parameter : parametersList) {
			if(parameter.getName().equals("apiKey")) {
				apiKey = parameter.getValue();
				break;
			}
		}
		//parametersList.add(new NameValuePair("verifyOnly","true"));

		long timestamp = (Calendar.getInstance().getTimeInMillis()/1000);
		parametersList.add(new NameValuePair("timestamp",timestamp+""));
		String dataForSignature = apiKey+"|"+timestamp+"|"+getQueryString(parametersList);
		String signature = SecurityUtil.calculateSignature(dataForSignature, secretKey);
		parametersList.add(new NameValuePair("signature",signature));
		
		return parametersList;
	}
	
	private static String getQueryString(List<NameValuePair> parameters) throws FatalException {
		String requestString = "";
		for(NameValuePair parameter : parameters) {
			if(!StringUtil.isNullOrEmpty(requestString)) {
				requestString+= "&";
			}
			try {
				requestString+= parameter.getName() + "=" + URLEncoder.encode(parameter.getValue(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new FatalException(e);
			}
		}
		return requestString;
	}
	
	private static String get(String endpoint, String requestParameters) throws FatalException {
		return get(endpoint, requestParameters, null, null);
	}
	
	/**
	* Sends an HTTP GET request to a url
	*
	* @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
	* @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
	* @return - The response from the end point
	 * @throws FatalException 
	*/
	private static String get(String endpoint, String requestParameters, String username, String password) throws FatalException {
		// Send a GET request to the servlet
        HttpClient client = new HttpClient();
		
		GetMethod get = new GetMethod(endpoint);
		
		get.setQueryString(requestParameters);

		if(username != null) {
			client.getState().setCredentials(
				AuthScope.ANY, 
                new UsernamePasswordCredentials(username, password)
            );
			get.setDoAuthentication( true );
		}
		
        try {
			// execute
			client.executeMethod(get);
			
			// print the status and response
			InputStream responseStream = get.getResponseBodyAsStream();
			String responseStreamString = StringUtil.convertStreamToString(responseStream);
			//logger.warn(responseStreamString);
			return responseStreamString;
            
        } catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }			
	}
		
	private static String post(String endpoint, NameValuePair[] parameters) throws FatalException {
		return post(endpoint, parameters, null, null, false);
	}
	
	/**
	* Reads data from the data reader and posts it to a server via POST request.
	* data - The data you want to send
	* endpoint - The server's address
	* output - writes the server's response to output
	 * @return 
	 * @throws FatalException 
	* @throws Exception
	*/
	@SuppressWarnings("deprecation")
	private static String post(String endpoint, NameValuePair[] parameters, String username, String password, boolean async) throws FatalException {
        HttpClient client = new HttpClient();	
				
		PostMethod post = new PostMethod(endpoint);
		post.addParameters(parameters);

		if(username != null) {
			client.getState().setCredentials(
				AuthScope.ANY, 
                new UsernamePasswordCredentials(username, password)
            );
			post.setDoAuthentication( true );
		}
		if(async) {
			client.setConnectionTimeout(1);
			client.setTimeout(1);
		}

        try {
			// execute 
			client.executeMethod(post);
			
			// print the status and response
			InputStream responseStream = post.getResponseBodyAsStream();
			return StringUtil.convertStreamToString(responseStream);
            
        } catch (SocketTimeoutException e) {
        	if(async) {
        		//expected
        		return null;
        	}else {
        		throw new FatalException(e);
        	}
        } catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
            // release any connection resources used by the method
        	post.releaseConnection();
        }			

	}
	
	public void clearCache() throws FatalException {
		APIResponse response = get(
        	new NameValuePair("method","cache.clear")
    	);
		logger.debug("Cache cleared");
	}
}
