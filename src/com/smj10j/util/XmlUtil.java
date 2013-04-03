package com.smj10j.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.smj10j.conf.Constants;

public abstract class XmlUtil {
    private static Logger logger = Logger.getLogger(XmlUtil.class);


	public static String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}

	public static int getIntValue(Element ele, String tagName) {
		return Integer.parseInt(getTextValue(ele,tagName));
	}
	
	public static long getLongValue(Element ele, String tagName) {
        return Long.parseLong(getTextValue(ele,tagName));
    }
	
	public static float getFloatValue(Element ele, String tagName) {
        return Float.parseFloat(getTextValue(ele,tagName));
    }
	
	public static double getDoubleValue(Element ele, String tagName) {
		return Double.parseDouble(getTextValue( ele, tagName));
	}
	
	public static Date getDateValue(Element ele, String tagName) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat( Constants.Date.FORMAT );
        return dateFormat.parse( getTextValue( ele, tagName) ) ;
	}
	                                                           
	
	
}
