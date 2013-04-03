package com.smj10j.dao;


import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.model.Customer;
import com.smj10j.util.ListUtil;

public abstract class CustomerDAO {

	private static Logger logger = Logger.getLogger(CustomerDAO.class);
	
	public static Customer getCustomer(long customerId) throws InvalidParameterException, FatalException {

		MySQL mysql = MySQL.getInstance(true);

		//create the user if they don't exist
		mysql.query("" +
				"SELECT * FROM " + MySQL.TABLES.CUSTOMERS + " " +
				"WHERE id=? LIMIT 1",
				customerId);
		if(mysql.nextRow()) {
			return Customer.from(mysql);
		}else {
			throw new InvalidParameterException(Constants.Error.INVALID_ID.CUSTOMER_ID, ListUtil.from(customerId+""));
		}
	}
	
	public static long getCustomerId(String apiKey) throws InvalidParameterException, FatalException {

		MySQL mysql = MySQL.getInstance(true);

		mysql.query("" +
				"SELECT id FROM " + MySQL.TABLES.CUSTOMERS + " " +
				"WHERE api_key=? LIMIT 1",
				apiKey);
		if(mysql.nextRow()) {
			return (Long)mysql.getColumn("id");
		}else {
			throw new InvalidParameterException(Constants.Error.INVALID_ID.API_KEY, ListUtil.from(apiKey));
		}
	}
}
