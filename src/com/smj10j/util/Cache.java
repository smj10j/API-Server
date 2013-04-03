package com.smj10j.util;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.transcoders.SerializingTranscoder;

import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.servlet.GatewayServlet;

public abstract class Cache {

	public static final String NULL = "null";
	private static MemcachedClient memcached = null;	
	private static long lastConnectAttempt = 0l;
	private static SerializingTranscoder serializingTranscoder = new SerializingTranscoder();
	
	private static Logger logger = Logger.getLogger(Cache.class);
	
	private static Map<Long, Boolean> threadToUseCache = new ConcurrentHashMap<Long, Boolean>();
	
	public static class lifetime {
		public static final int HALF_MINUTE = 30;
		public static final int MINUTE = 60;
		public static final int FIVE_MINUTE = MINUTE*5;
		public static final int HALF_HOUR = MINUTE*30;
		public static final int HOUR = MINUTE*60;
		public static final int TWO_HOUR = HOUR*2;
		public static final int DAY = HOUR*24;
		public static final int DEFAULT = FIVE_MINUTE;
	}
	
	public static class namespace {
		public static final String USER_BY_ID = "USER_BY_ID";
		public static final String CUSTOMER_BY_API_KEY = "CUSTOMER_BY_API_KEY";
		public static final String CUSTOMER_API_KEY_TO_CUSTOMER_ID = "CUSTOMER_API_KEY_TO_CUSTOMER_ID";
		public static final String CUSTOMER_BY_ID = "CUSTOMER_BY_ID";
	}
	
	private static String getNamespaceKey(String...namespaces) {
		String server = GatewayServlet.getHostname();
		String key = server + "|" + Constants.API_VERSION + "|";
		for(String ns : namespaces) {
			key+= ns + "|";
		}
		//logger.debug("CacheKey: " + key);

		return key.toUpperCase();
	}
	
	private static String getSafeKey(String key) throws FatalException {
		String encodedKey = StringUtil.utf8Encode(key);
		//logger.debug("Encoded key="+key+ " to "+encodedKey);
		String safeKey = encodedKey;
		if(safeKey.length() > 250) {
			//make it a 64 byte key
			//logger.debug("Shortening key of length " + safeKey.length() + "...");
			safeKey = SecurityUtil.md5(safeKey.substring(0,249)) + SecurityUtil.md5(safeKey.substring(249));
		}
		return safeKey.toUpperCase();
	}
	
	public static void init() throws FatalException {
		//only allow attempts to reinitialize the connection every 30 seconds
		if(System.currentTimeMillis() - lastConnectAttempt < (1000 * 30))
			return;
		lastConnectAttempt = System.currentTimeMillis();
		shutdown();
		
		//start-up memcached server if not already up
		//String prefix = Constants.SERVLET_CONTEXT_PATH;
		//String file = "WEB-INF/scripts/memcached-start.sh";
		
		String memcachedServers = ArrayUtil.implode(Constants.memcacheServers," ");
		logger.info("Memcached: using the remote, shared cache nodes at : " + memcachedServers);
		
		try {
			memcached = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(memcachedServers));
			
			//compress anything over 10 kb
			serializingTranscoder.setCompressionThreshold(1024*10);
			
		} catch (IOException e) {
			throw new FatalException(e);
		}
	}
	
	private static void shutdown() {
		if(memcached != null)
			memcached.shutdown();
		memcached = null;
	}
	
	private static void handleFailure(boolean restartCache, String message) throws FatalException {
		if(restartCache) {
			init();
		}
		if(message != null) {
			logger.warn(message);
		}
	}

	private static void put(Object value, int lifetime, String key) throws FatalException {
		if(memcached == null) return;		
		if(value == null)
			return;
		
		Boolean useCache = threadToUseCache.get(Thread.currentThread().getId());
		if(useCache == null) {
			threadToUseCache.put(Thread.currentThread().getId(), true);
		}else if(useCache == false) {
			threadToUseCache.put(Thread.currentThread().getId(), true);
			return;
		}		
		
		String safeKey = getSafeKey(key);
		memcached.set(safeKey, lifetime, value, serializingTranscoder);
	}
	
	public static void put(Object value, String key, String...namespaces) throws FatalException {
		if(memcached == null) return;
		String namespaceKey = getNamespaceKey(namespaces);
		//logger.debug("Just put value: " + value + " into namespace: " + namespaceKey);
		put(value, lifetime.DEFAULT, namespaceKey + key);
	}	
	
	public static void put(Object value, int lifetime, String key, String...namespaces) throws FatalException {
		if(memcached == null) return;
		String namespaceKey = getNamespaceKey(namespaces);
		//logger.debug("Just put value: " + value + " into namespace: " + namespaceKey);
		put(value, lifetime, namespaceKey + key);
	}
	
	public static void ignoreCacheOnNext() {
		threadToUseCache.put(Thread.currentThread().getId(), false);
	}
	
	private static Object get(String key) throws FatalException {
		if(memcached == null) return null;
		Boolean useCache = threadToUseCache.get(Thread.currentThread().getId());
		if(useCache == null) {
			threadToUseCache.put(Thread.currentThread().getId(), true);
		}else if(useCache == false) {
			threadToUseCache.put(Thread.currentThread().getId(), true);
			return null;
		}

		String safeKey = getSafeKey(key);
		try {
			return memcached.get(safeKey, serializingTranscoder);
		}catch (BufferUnderflowException e) {
			handleFailure(true, "Memcached: connection error on get(\""+safeKey+"\")");	//safeKey may have been the problem
			return null;
		}catch (OperationTimeoutException e) {
			handleFailure(true, "Memcached: timeout on get(\""+key+"\")");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T get(String key,String...namespaces) throws FatalException {
		if(memcached == null) return null;
		String namespaceKey = getNamespaceKey(namespaces);
		return (T) get(namespaceKey + key);
	}	
	
	private static void remove(String key) throws FatalException {
		if(memcached == null) return;
		String safeKey = getSafeKey(key);
		memcached.delete(safeKey);
	}
	
	public static void remove(String key,String...namespaces) throws FatalException {
		if(memcached == null) return;
		String namespaceKey = getNamespaceKey(namespaces);
		//logger.debug("Removed with namespace: " + namespaceKey);
		remove(namespaceKey + key);
	}		
	
	public static void clear() throws FatalException {
		if(memcached == null) return;
		memcached.flush();
		logger.info("Memcached: clearing cache");
	}
}
