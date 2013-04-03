package com.smj10j.model;

import java.io.Serializable;

public class MethodParameter implements Serializable {

	private static final long serialVersionUID = -1301073746799731957L;
	
	private String name;
	private String type;
	private boolean required;
	private String defaultValue;
	private String description;
	
	public MethodParameter(String name, String type, boolean required, String defaultValue, String description) {
		this.name = name;
		this.type = type;
		this.required = required;
		this.defaultValue = defaultValue;
		this.setDescription(description);
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}
	public boolean isRequired() {
		return required;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
