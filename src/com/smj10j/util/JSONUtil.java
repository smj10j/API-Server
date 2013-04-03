package com.smj10j.util;

import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.jaxb.ResponseType;
import com.smj10j.model.APIRequest;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

public abstract class JSONUtil {

    private static Logger logger = Logger.getLogger(JSONUtil.class);
    
	public static void marshal(APIRequest request, Object responseRoot, Writer writer) throws FatalException, IOException {
		
		//get the javascript callback, if present
        String jsCallback = null;
        if(request != null) {
			try {
				jsCallback = request.getParameter(Constants.Request.JSCALLBACK, false);
			} catch (InvalidParameterException e) {
				//it's okay to not have one
			}
        }
		
		// get an xstream using a json dirver
		XStream xstream = new XStream(new JsonHierarchicalStreamDriver());
		xstream.setMode(XStream.NO_REFERENCES);

		//alias top-level types
        xstream.alias("response", ResponseType.class);	

        //first write out the jscallback method name
        if(!StringUtil.isNullOrEmpty(jsCallback)) {
        	writer.write(jsCallback + "(");
        }
        
		// write the json to our writer
        //NOTE: all element names will be camel case and not cased as in JAXB
		xstream.marshal(responseRoot, new JsonWriter(writer));
		
        //close off the method if jscallback was given
        if(!StringUtil.isNullOrEmpty(jsCallback)) {
        	writer.write(")");
        }		
	}
	
	public static boolean isJSON( String response ) {
		try {
			@SuppressWarnings("unused") 
			// we are checking whether or not it is able to convert to JSON
			JSONObject jsonObject = new JSONObject( response );
			return true;
		} catch( JSONException e ) {
			logger.warn( "There was no JSON so there was no error" );
			return false;
		}
	}
	
	public static String getSafe(JSONObject jsonObject, String key) {
		String value = null;
		try {
			value = jsonObject.getString(key);
			if(value != null && value.equals("null")) {
				value = null;
			}
		}catch (JSONException e) {
			//null
		}
		return value;
	}
}
