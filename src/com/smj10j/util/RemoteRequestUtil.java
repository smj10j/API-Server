package com.smj10j.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.APIRequest;

public class RemoteRequestUtil {

	private static Logger logger = Logger.getLogger(RemoteRequestUtil.class);

	public static String get(String endpoint, NameValuePair[] parameters,
			boolean async) throws FatalException {
		String requestString = "";
		String name;
		String value;
		if (parameters != null) {
			for (NameValuePair parameter : parameters) {
				if (!StringUtil.isNullOrEmpty(requestString)) {
					requestString += "&";
				}
				try {
					name = parameter.getName();
					value = parameter.getValue();
					requestString += name
							+ "="
							+ (StringUtil.isNullOrEmpty(value) ? ""
									: URLEncoder.encode(parameter.getValue(),
											"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new FatalException(e);
				}
				// requestString+= parameter.getName() + "=" +
				// parameter.getValue();
			}
		}


		logger.debug("URI: " + endpoint + "?" + requestString);
		return get(endpoint, requestString, async);
	}

	public static String get(String endpoint, String requestParameters,
			boolean async) throws FatalException {
		return get(endpoint, requestParameters, null, null, async);
	}

	/**
	 * Sends an HTTP GET request to a url
	 * 
	 * @param endpoint
	 *            - The URL of the server. (Example:
	 *            " http://www.yahoo.com/search")
	 * @param requestParameters
	 *            - all the request parameters (Example:
	 *            "param1=val1&param2=val2"). Note: This method will add the
	 *            question mark (?) to the request - DO NOT add it yourself
	 * @param async
	 *            if true, the request will return instantly
	 * @return - The response from the end point
	 * @throws FatalException
	 */
	@SuppressWarnings("deprecation")
	public static String get(String endpoint, String requestParameters,
			String username, String password, boolean async)
			throws FatalException {
		// Send a GET request to the servlet
		HttpClient client = new HttpClient();

		GetMethod get = new GetMethod(endpoint);
		get.setQueryString(requestParameters);

		if (username != null) {
			client.getState().setCredentials(
					AuthScope.ANY,
					new UsernamePasswordCredentials(username,
							password == null ? "" : password));
			get.setDoAuthentication(true);
		}

		if (async) {
			client.setConnectionTimeout(1);
			client.setTimeout(1);
		} else {
			client.setConnectionTimeout(5000);
			client.setTimeout(10000);
		}

		try {
			// execute
			client.executeMethod(get);

			// print the status and response
			InputStream responseStream = get.getResponseBodyAsStream();
			return StringUtil.convertStreamToString(responseStream);

		} catch (SocketTimeoutException e) {
			if (async) {
				// expected
				return null;
			} else {
				throw new FatalException(e);
			}
		} catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
			// release any connection resources used by the method
			get.releaseConnection();
		}
	}

	public static String post(String endpoint, String xmlString,
			String username, String password, String headerName,
			String headerValue) throws FatalException {
		// Send a GET request to the servlet
		HttpClient client = new HttpClient();

		PostMethod post = new PostMethod(endpoint);

		try {
			post.setRequestEntity(new StringRequestEntity(xmlString,
					"text/plain", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new FatalException(e);
		}

		if (username != null) {
			client.getState().setCredentials(
					AuthScope.ANY,
					new UsernamePasswordCredentials(username,
							password == null ? "" : password));
			post.setDoAuthentication(true);
		}

		if (headerName != null) {
			post.setRequestHeader(new Header(headerName, headerValue));
		}

		try {
			// execute
			client.executeMethod(post);

			// print the status and response
			InputStream responseStream = post.getResponseBodyAsStream();
			return StringUtil.convertStreamToString(responseStream);

		} catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
			// release any connection resources used by the method
			post.releaseConnection();
		}
	}

	public static byte[] getBytes(String endpoint, String requestParameters,
			String username, String password) throws FatalException {
		// Send a GET request to the servlet
		HttpClient client = new HttpClient();

		GetMethod get = new GetMethod(endpoint);
		get.setQueryString(requestParameters);

		if (username != null) {
			client.getState().setCredentials(
					AuthScope.ANY,
					new UsernamePasswordCredentials(username,
							password == null ? "" : password));
			get.setDoAuthentication(true);
		}

		try {
			// execute
			client.executeMethod(get);

			// print the status and response
			return get.getResponseBody();

		} catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
			// release any connection resources used by the method
			get.releaseConnection();
		}
	}

	public static String post(String endpoint, NameValuePair... parameters)
			throws FatalException {
		return post(endpoint, parameters, null, null, false);
	}

	/**
	 * Reads data from the data reader and posts it to a server via POST
	 * request. data - The data you want to send endpoint - The server's address
	 * output - writes the server's response to output
	 * 
	 * @return
	 * @throws FatalException
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static String post(String endpoint, NameValuePair[] parameters,
			String username, String password, boolean async)
			throws FatalException {

		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(endpoint);
		post.addParameters(parameters);

		if (username != null) {
			client.getState().setCredentials(
					AuthScope.ANY,
					new UsernamePasswordCredentials(username,
							password == null ? "" : password));
			post.setDoAuthentication(true);
		}
		if (async) {
			client.setConnectionTimeout(1);
			client.setTimeout(1);
		} else {
			client.setConnectionTimeout(5000);
			client.setTimeout(10000);
		}

		logger.info("Making request to: " + endpoint);

		try {
			// execute
			client.executeMethod(post);

			// print the status and response
			InputStream responseStream = post.getResponseBodyAsStream();
			return StringUtil.convertStreamToString(responseStream);

		} catch (SocketTimeoutException e) {
			if (async) {
				// expected
				return null;
			} else {
				throw new FatalException(e);
			}
		} catch (ConnectTimeoutException e) {
			if (async) {
				// expected
				return null;
			} else {
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

	/**
	 * Reads data from the data reader and posts it to a server via DELETE
	 * request. endpoint - The server's address output - writes the server's
	 * response to output
	 * 
	 * @return
	 * @throws FatalException
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static String delete(String endpoint, String username,
			String password, boolean async) throws FatalException {

		HttpClient client = new HttpClient();
		DeleteMethod delete = new DeleteMethod(endpoint);

		if (username != null) {
			client.getState().setCredentials(
					AuthScope.ANY,
					new UsernamePasswordCredentials(username,
							password == null ? "" : password));
			delete.setDoAuthentication(true);
		}
		if (async) {
			client.setConnectionTimeout(1);
			client.setTimeout(1);
		} else {
			client.setConnectionTimeout(5000);
			client.setTimeout(10000);
		}

		logger.info("Making request to: " + endpoint);

		try {
			// execute
			client.executeMethod(delete);

			// print the status and response
			InputStream responseStream = delete.getResponseBodyAsStream();
			return StringUtil.convertStreamToString(responseStream);

		} catch (SocketTimeoutException e) {
			if (async) {
				// expected
				return null;
			} else {
				throw new FatalException(e);
			}
		} catch (ConnectTimeoutException e) {
			if (async) {
				// expected
				return null;
			} else {
				throw new FatalException(e);
			}
		} catch (HttpException e) {
			throw new FatalException(e);
		} catch (IOException e) {
			throw new FatalException(e);
		} finally {
			// release any connection resources used by the method
			delete.releaseConnection();
		}

	}

	public static String delete(String endpoint) throws FatalException {
		return delete(endpoint, null, null, false);
	}

	public static String getQueryString(APIRequest apiRequest)
			throws InvalidParameterException {

		Map<String, String> parameterMap = new LinkedHashMap<String, String>();

		// add in the overrides
		Map<String, String[]> overridenParameterMap = apiRequest
				.getParameterMap();
		for (String key : overridenParameterMap.keySet()) {
			parameterMap.put(key, apiRequest.getParameter(key));
		}

		// and make a string of it
		StringBuilder queryStringBuilder = new StringBuilder();
		for (String name : parameterMap.keySet()) {
			String value = parameterMap.get(name);
			if (queryStringBuilder.length() > 0) {
				queryStringBuilder.append("&");
			}
			try {
				queryStringBuilder.append(name + "="
						+ URLEncoder.encode(value, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(
						"Failed to generate querystring from parameters - Bad encoding for name/value pair at: "
								+ name + "=" + value, e);
			}
		}

		return queryStringBuilder.toString();
	}
}
