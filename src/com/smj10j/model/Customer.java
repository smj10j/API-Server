package com.smj10j.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.CustomerType;

@MySQLTable(name=MySQL.TABLES.CUSTOMER, 
		primaryKey="customerId",
		transients={
			"someTransientExample"
		}
)

public class Customer extends DatabaseBackedObject implements Serializable {

	private static Logger logger = Logger.getLogger(Customer.class);

	private static final long serialVersionUID = 8413764518270892930L;
	
	private String apiKey;
	private String name;
	private String secretKey;
	private long customerId;
	private boolean enabled;
	private long created;

	private List<String> someTransientExample = null;// transient

	public Customer() {

	}	
	
	public CustomerType toCustomerType() throws InvalidParameterException, FatalException {
		CustomerType customerType = new CustomerType();
		customerType.setCustomerId(getCustomerId());
		customerType.setApiKey(getApiKey());
		customerType.setEnabled(isEnabled());
		customerType.setName(getName());
		
		return customerType;
	}
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	public void setCreated(long created) {
		this.created = created;
	}

	public long getCreated() {
		return created;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}

	public long getCustomerId() {
		return customerId;
	}
	
	public static Customer from(MySQL mysql) throws FatalException, InvalidParameterException {
		
		
		Customer customer = new Customer();
		customer.setCustomerId((Long)mysql.getColumn("customer_id"));
		customer.setApiKey((String)mysql.getColumn("api_key"));
		customer.setName((String)mysql.getColumn("name"));
		customer.setSecretKey((String)mysql.getColumn("secret_key"));
		customer.setEnabled((Boolean)mysql.getColumn("enabled"));
		
		customer.takeFieldValuesSnapshot();
		
		return customer;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public static class NameComparator implements Comparator<Customer> {
	    public int compare(Customer left, Customer right) {
	    	if(left == null) {
	    		if(right == null) {
	    			return 0;
	    		}else {
	    			return -1;
	    		}
	    	}else if(right == null) {
	    		return 1;
	    	}else {
	    		return left.getName().compareTo(right.getName());
	    	}
	    }
	}
		
	public boolean is(String apiKey) {
		return getApiKey().equalsIgnoreCase(apiKey);
	}

	public List<String> getSomeTransientExample() {
		return someTransientExample;
	}

	public void setSomeTransientExample(List<String> someTransientExample) {
		this.someTransientExample = someTransientExample;
	}
}