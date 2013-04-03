package com.smj10j.model;

import java.io.Serializable;
import java.util.Date;

import com.smj10j.annotation.MySQLTable;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.dao.MySQL;
import com.smj10j.jaxb.ServerType;

@MySQLTable(name=MySQL.TABLES.SERVER, 
		primaryKey="serverId",
		transients={}
)

public class Server extends DatabaseBackedObject implements Serializable {
	
	private static final long serialVersionUID = 5347247818275609834L;
	
	public static enum Type {
		SUBSCRIBE
	};
	
	private long serverId;
	private Type type;
	private String url;
	private int port;
	private String protocol;
	private String resource;
	private long subscriberCount;
	private Date created;
		
	public Server() {

	}
	
	public String toString() {
		return "Id: " + serverId;
	}
	
	public ServerType toServerType() throws InvalidParameterException, FatalException {
		ServerType serverType = new ServerType();
		serverType.setServerId(getServerId());
		serverType.setType(getType().toString());
		serverType.setUrl(getUrl());
		serverType.setPort(getPort());
		serverType.setProtocol(getProtocol());
		serverType.setResource(getResource());
		serverType.setSubscriberCount(getSubscriberCount());
		serverType.setCreated(getCreated().getTime());

		return serverType;
	}
	
	
	public static Server from(MySQL mysql) throws FatalException, InvalidParameterException {
		
		Server server = new Server();
		server.setServerId((Long)mysql.getColumn("server_id"));
		server.setType(Type.valueOf((String)mysql.getColumn("type")));
		server.setUrl((String)mysql.getColumn("url"));
		server.setPort(((Long)mysql.getColumn("port")).intValue());
		server.setProtocol((String)mysql.getColumn("protocol"));
		server.setResource((String)mysql.getColumn("resource"));
		server.setSubscriberCount(((Long)mysql.getColumn("subscriber_count")).intValue());
		server.setCreated((Date)mysql.getColumn("created"));

		return server;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getCreated() {
		return created;
	}

	public void setSubscriberCount(long subscriberCount) {
		this.subscriberCount = subscriberCount;
	}

	public long getSubscriberCount() {
		return subscriberCount;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResource() {
		return resource;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setServerId(long serverId) {
		this.serverId = serverId;
	}

	public long getServerId() {
		return serverId;
	}
}
