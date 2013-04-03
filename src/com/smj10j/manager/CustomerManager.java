package com.smj10j.manager;

import org.apache.log4j.Logger;

import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.CustomerDAO;
import com.smj10j.model.Customer;
import com.smj10j.util.Cache;

public abstract class CustomerManager {

	private static Logger logger = Logger.getLogger(CustomerManager.class);

	public static Customer getCustomer(long customerId) throws InvalidParameterException, FatalException {
		//get the basic customer data
		Customer customer = Cache.get(customerId+"", Cache.namespace.CUSTOMER_BY_ID);
		if(customer == null) {
			customer = CustomerDAO.getCustomer(customerId);
			Cache.put(customer, customerId+"", Cache.namespace.CUSTOMER_BY_ID);
		}

		//logger.debug("Got customer via customerId. customerId: " + customer.getCustomerId() + OutputUtil.getElapsedString());
		return customer;
	}
	
	public static Customer getCustomer(String apiKey) throws InvalidParameterException, FatalException {
		//first see if we have an api_key=>customer cache value
		Customer customer = Cache.get(apiKey, Cache.namespace.CUSTOMER_BY_API_KEY);
		if(customer == null) {
			//find the customer id
			Long customerId = Cache.get(apiKey, Cache.namespace.CUSTOMER_API_KEY_TO_CUSTOMER_ID);
			if(customerId == null) {
				customerId = CustomerDAO.getCustomerId(apiKey);
				Cache.put(customerId, apiKey, Cache.namespace.CUSTOMER_API_KEY_TO_CUSTOMER_ID);
			}
			customer = getCustomer(customerId);
			Cache.put(customer, apiKey, Cache.namespace.CUSTOMER_BY_API_KEY);
		}
		//logger.debug("Got customer via apiKey. customerId: " + customer.getCustomerId() + OutputUtil.getElapsedString());
		return customer;
	}
	
	public static void save(Customer customer) throws FatalException, InvalidParameterException {
		customer.save();
		Cache.remove(customer.getApiKey(), Cache.namespace.CUSTOMER_BY_API_KEY);
		Cache.remove(customer.getId()+"", Cache.namespace.CUSTOMER_BY_ID);
	}
}
