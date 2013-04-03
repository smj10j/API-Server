package com.smj10j.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.servlet.GatewayServlet;
import com.smj10j.util.EmailUtil;
import com.smj10j.util.ListUtil;
import com.smj10j.util.OutputUtil;

public class MySQL {

	private static Logger logger = Logger.getLogger(MySQL.class);

	public static class TABLES {
		public static final String USER = "user";
	}

	private enum DataSourceType {
		MASTER, SLAVE
	}

	private static final Map<DataSourceType, String> dataSourceStrings = new HashMap<DataSourceType, String>();
	static {
		// set in jboss/server/myapp/deploy/mysql-ds.xml
		dataSourceStrings.put(DataSourceType.MASTER, "java:/MySqlDS");
		dataSourceStrings.put(DataSourceType.SLAVE, "java:/MySqlDSSlave");
	}

	// should be cross-thread
	private static Map<DataSourceType, DataSource> dataSources = new HashMap<DataSourceType, DataSource>();
	private static Map<DataSourceType, InitialContext> contexts = new HashMap<DataSourceType, InitialContext>();
	private static Map<String, Object> threadToMySQLInstance = new ConcurrentHashMap<String, Object>();

	// we don't want this to be static or concurrent... we want them just for
	// this thread
	private DataSourceType dataSourceType;
	private Connection connection;

	private Map<String, Long> lastInsertedIds = new HashMap<String, Long>();
	private Map<String, ResultSet> callerToResultSet = new HashMap<String, ResultSet>();
	private Map<String, PreparedStatement> callerToStatement = new HashMap<String, PreparedStatement>();

	private long totalTime;
	private boolean writeMade = false;
	private boolean debugNext;

	public static MySQL getInstance(boolean createIfNoneExists)
			throws FatalException {
		return getInstance(createIfNoneExists, DataSourceType.MASTER);
	}

	public static MySQL getSlaveInstance(boolean createIfNoneExists)
			throws FatalException {
		return getInstance(createIfNoneExists, DataSourceType.SLAVE);
	}

	private static MySQL getInstance(boolean createIfNoneExists,
			DataSourceType dataSourceType) throws FatalException {
		MySQL mysql = (MySQL) threadToMySQLInstance.get(dataSourceType.toString() + Thread.currentThread().getId());
		if (mysql == null && createIfNoneExists) {
			if (Constants.DEBUG_MODE)
				logger.info("Opening new " + dataSourceType.toString() + " transaction for thread" + OutputUtil.getElapsedString());
			mysql = new MySQL(dataSourceType);
		} else if (mysql != null) {
			if (Constants.DEBUG_MODE)
				logger.debug("Using existing " + dataSourceType.toString() + " transaction" + OutputUtil.getElapsedString());
			mysql.isValid();
		}
		return mysql;
	}

	private MySQL(DataSourceType dataSourceType) throws FatalException {
		long startTime = System.currentTimeMillis();
		try {

			totalTime = 0;
			this.dataSourceType = dataSourceType;

			// Register the JDBC driver for MySQL.
			Class.forName("com.mysql.jdbc.Driver");

			String datasourceString = dataSourceStrings
					.get(this.dataSourceType);
			DataSource datasource = dataSources.get(this.dataSourceType);
			InitialContext context = contexts.get(this.dataSourceType);

			if (context == null || datasource == null) {
				try {
					logger.info("Connecting to the MySQL Datasource at " + datasourceString + ": " + OutputUtil.getElapsedString());
					context = new InitialContext();
					datasource = (DataSource) context.lookup(datasourceString);

					dataSources.put(this.dataSourceType, datasource);
					contexts.put(this.dataSourceType, context);

				} catch (Exception e) {
					logger.error("Failed to connect to the MySQL datasource...");
					context = null;
					datasource = null;
					throw new FatalException(e);
				}
			}

			// for transaction management
			threadToMySQLInstance.put(dataSourceType.toString()	+ Thread.currentThread().getId(), this);

			this.fetchConnection();

		} catch (ClassNotFoundException e) {
			throw new FatalException(e);
		} finally {
			totalTime += (System.currentTimeMillis() - startTime);
		}

		debugNext = false;
	}

	private void fetchConnection() throws FatalException {
		try {
			DataSource datasource = dataSources.get(this.dataSourceType);

			connection = (Connection) datasource.getConnection();
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new FatalException(e);
		}
	}

	public boolean isValid() throws FatalException {
		try {
			return this.connection.isValid(5);
		} catch (SQLException e) {
			this.fetchConnection();
			try {
				return this.connection.isValid(5);
			} catch (SQLException e1) {
				throw new FatalException(e);
			}
		}
	}

	public static boolean wasWriteMade() throws FatalException {
		MySQL mysql = MySQL.getInstance(false);
		return mysql != null && mysql.writeMade;
	}

	public static void commit() throws FatalException {
		long startTime = System.currentTimeMillis();

		// commit the master
		MySQL mysqlMaster = MySQL.getInstance(false);
		try {
			if (mysqlMaster != null) {
				if (Constants.DEBUG_MODE)
					logger.info("Committing transaction" + OutputUtil.getElapsedString());
				if (mysqlMaster.connection != null && !mysqlMaster.connection.isClosed()) {
					mysqlMaster.connection.commit();
				} else {
					if (Constants.DEBUG_MODE)
						logger.info("No commit because connection is closed" + OutputUtil.getElapsedString());
				}
				mysqlMaster.close();
			}
		} catch (SQLException e) {
			throw new FatalException(e);
		} finally {
			if (mysqlMaster != null)
				mysqlMaster.totalTime += (System.currentTimeMillis() - startTime);
		}

		// and close the slave
		MySQL mysqlSlave = MySQL.getSlaveInstance(false);
		try {
			if (mysqlSlave != null) {
				mysqlSlave.close();
			}
		} finally {
			if (mysqlSlave != null)
				mysqlSlave.totalTime += (System.currentTimeMillis() - startTime);
		}
	}

	public static void rollback() throws FatalException {
		long startTime = System.currentTimeMillis();

		// rollback the master
		MySQL mysqlMaster = MySQL.getInstance(false);
		try {
			if (mysqlMaster != null) {
				if (Constants.DEBUG_MODE)
					logger.info("Rolling back transaction" + OutputUtil.getElapsedString());
				if (mysqlMaster.connection != null && !mysqlMaster.connection.isClosed()) {
					mysqlMaster.connection.rollback();
				} else {
					if (Constants.DEBUG_MODE)
						logger.info("No rollback because connection is closed" + OutputUtil.getElapsedString());
				}
				mysqlMaster.close();
			}
		} catch (SQLException e) {
			throw new FatalException(e);
		} finally {
			if (mysqlMaster != null)
				mysqlMaster.totalTime += (System.currentTimeMillis() - startTime);
		}

		// and close the slave
		MySQL mysqlSlave = MySQL.getSlaveInstance(false);
		try {
			if (mysqlSlave != null) {
				mysqlSlave.close();
			}
		} finally {
			if (mysqlSlave != null)
				mysqlSlave.totalTime += (System.currentTimeMillis() - startTime);
		}
	}

	private void close() throws FatalException {
		try {
			for (ResultSet callerResultSet : callerToResultSet.values()) {
				callerResultSet.close();
			}
			callerToResultSet.clear();

			for (PreparedStatement statement : callerToStatement.values()) {
				statement.close();
			}
			callerToStatement.clear();

			if (connection != null) {
				connection.close();
				connection = null;
			}

			threadToMySQLInstance.remove(dataSourceType.toString() + Thread.currentThread().getId());

			logger.info("Total time interacting with MySQL - Thread " + Thread.currentThread().getId() + ": " + totalTime + "ms");

		} catch (SQLException e) {
			throw new FatalException(e);
		}
	}

	private static String getUnescapedQueryForDebugging(String sql,
			Object[] parameters) {
		return OutputUtil.tokenReplace(sql, "\\?", ListUtil.from(parameters)) + OutputUtil.getElapsedString();
	}

	/*
	 * Generalized query method - if you're issuing a select statement you can
	 * access the returned results using nextRow() and getColumn()
	 */
	public void query(String sql, Object... parameters) throws FatalException {
		long startTime = System.currentTimeMillis();

		try {

			sql += " "; // this is for parameter splitting by '?'
			String baseSQLStr = sql.toUpperCase();

			if (baseSQLStr.contains("LOAD ") || baseSQLStr.contains("TRUNCATE ") || baseSQLStr.contains("DROP ") || baseSQLStr.contains("ALTER ") || baseSQLStr.contains("CREATE ")) {
				throw new FatalException("Unable to use commands like DROP and ALTER: " + sql);
			}

			if (dataSourceType.equals(DataSourceType.SLAVE)) {
				if (baseSQLStr.contains("INSERT ") || baseSQLStr.contains("UPDATE ") || baseSQLStr.contains("DELETE ")) {
					throw new FatalException("You cannot use INSERT/UPDATE/DELETE on the SLAVE database: " + sql);
				}
			}

			String caller = getCaller(false); // creates a new resultset in the
												// top level method
			if (Constants.DEBUG_MODE)
				logger.debug("Performing query for caller " + caller + OutputUtil.getElapsedString());
			ResultSet callerResultSet = callerToResultSet.get(caller);
			PreparedStatement statement = callerToStatement.get(caller);
			if (statement != null) {
				if (Constants.DEBUG_MODE)
					logger.debug("Closed statement for caller " + caller + OutputUtil.getElapsedString());
				statement.close();
			}
			// Get a Statement object
			statement = (PreparedStatement) connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			callerToStatement.put(caller, statement);
			if (Constants.DEBUG_MODE)
				logger.debug("Opened new statement for caller " + caller + OutputUtil.getElapsedString());

			int parameterIndex = 1;
			for (Object parameter : parameters) {
				if (parameter != null || (parameters.length == sql.split("\\?").length - 1)) {
					// we ignore null items in the list if there are optional
					// params
					statement.setObject(parameterIndex++, parameter);
				}
			}
			// debugNext = true;
			if (debugNext) {
				logger.debug("About to execute (unescaped) query: "	+ getUnescapedQueryForDebugging(sql, parameters));
				debugNext = false;
			}
			if (baseSQLStr.contains("INSERT ") || baseSQLStr.contains("UPDATE ") || baseSQLStr.contains("DELETE ")) {
				writeMade = true;
				statement.executeUpdate();
				if (baseSQLStr.contains("INSERT ")) {
					String tableName = baseSQLStr.substring(baseSQLStr.indexOf("INTO") + 4).trim();
					tableName = tableName.substring(0, tableName.indexOf(" "))	.trim();
					ResultSet generatedKeys = statement.getGeneratedKeys();
					if (generatedKeys.next()) {
						long insertId = (Long) generatedKeys.getObject(1);
						logger.debug("Inserted row into " + tableName + " with id: " + insertId	+ OutputUtil.getElapsedString());
						lastInsertedIds.put(tableName, insertId);
					}
				}
			} else {
				if (callerResultSet != null) {
					if (Constants.DEBUG_MODE)
						logger.debug("Closed resultSet for caller " + caller + OutputUtil.getElapsedString());
					callerResultSet.close();
				}
				callerResultSet = statement.executeQuery();
				callerToResultSet.put(caller, callerResultSet);
				if (Constants.DEBUG_MODE)
					logger.debug("Opened new resultSet for caller " + caller + OutputUtil.getElapsedString());
			}
		} catch (SQLException e) {
			throw new FatalException("SQL Exception on query: " + getUnescapedQueryForDebugging(sql, parameters), e);
		} finally {
			long elapsed = (System.currentTimeMillis() - startTime);
			totalTime += elapsed;
			if (elapsed > 1000) {
				// slow query!
				logger.warn("SLOW QUERY: " + getUnescapedQueryForDebugging(sql, parameters));
				// email a notice to admins
				try {
					String subject = "SLOW Query on " + GatewayServlet.getHostname();
					String body = "" + "SLOW Query: " + getUnescapedQueryForDebugging(sql, parameters);

					EmailUtil.email(null, "slowquery-notifier", ListUtil.from(EmailUtil.getInternalAdminEmail()), subject, body.getBytes(), "txt", null);

				} catch (FatalException e) {
					// oh bonkers
					logger.error("Error while trying to email admins about slow query!", e);
				} catch (InvalidParameterException e) {
					logger.error("Error while trying to email admins about slow query!", e);
				}
			}
		}
	}

	/*
	 * Returns true if there is another row and advance the internal pointer Use
	 * getColumn() to select items for the row
	 */
	public boolean nextRow() throws FatalException {
		long startTime = System.currentTimeMillis();
		String caller = getCaller(true); // expects to find a result set on the
											// stack
		try {
			if (Constants.DEBUG_MODE)
				logger.debug("Trying to access resultSet for caller " + caller + OutputUtil.getElapsedString());
			ResultSet callerResultSet = callerToResultSet.get(caller);
			if (callerResultSet != null) {
				if (!callerResultSet.next()) {

					closeResultSet();

					return false;
				}
				return true;
			} else {
				throw new FatalException("Tried to GET THE NEXT ROW from a resultSet that was null with caller=" + caller + "!");
			}
		} catch (SQLException e) {
			// logger.error("Tried to GET THE NEXT ROW from a non-null resultSet with caller="
			// + caller + "!");
			throw new FatalException(e);
		} finally {
			totalTime += (System.currentTimeMillis() - startTime);
		}
	}

	private void closeResultSet() throws FatalException {
		try {
			String caller = getCaller(true); // expects to find a result set on
												// the stack

			ResultSet callerResultSet = callerToResultSet.get(caller);
			if (callerResultSet != null) {
				callerResultSet.close();
				callerToResultSet.remove(caller);
			}
			PreparedStatement statement = callerToStatement.get(caller);
			if (statement != null) {
				statement.close();
				callerToStatement.remove(caller);
			}

			if (Constants.DEBUG_MODE)
				logger.debug("Closed statement and resultSet for caller " + caller + OutputUtil.getElapsedString());

		} catch (SQLException e) {
			throw new FatalException(e);
		}
	}

	public long lastInsertId(String tableName) {
		return lastInsertedIds.get(tableName.toUpperCase());
	}

	public Object getColumn(String columnLabel) throws FatalException {
		long startTime = System.currentTimeMillis();
		String caller = getCaller(true); // expects to find a result set on the
											// stack
		ResultSet callerResultSet = callerToResultSet.get(caller);
		try {
			if (callerResultSet == null) {
				throw new FatalException("Tried to GET A COLUMN from a resultSet that was null with caller=" + caller + "!");
			}

			Object resultObject = null;
			try {
				resultObject = callerResultSet.getObject(columnLabel);
			} catch (SQLException e) {
				e.fillInStackTrace();
				if (e.getMessage().contains("can not be represented as java.sql.Date")) {
					resultObject = null;
				} else if (e.getMessage().contains("to TIMESTAMP")) {
					resultObject = null;
				} else {
					throw e;
				}
			}

			return resultObject;

		} catch (SQLException e) {
			// logger.error("Tried to GET A COLUMN from a non-null resultSet with caller="
			// + caller + "!");
			throw new FatalException(e);
		} finally {
			totalTime += (System.currentTimeMillis() - startTime);
		}
	}

	public static <T> byte[] serialize(T object) throws FatalException {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(object);
			byte[] output = bos.toByteArray();
			oos.close();
			return output;
		} catch (IOException e) {
			throw new FatalException(e);
		}

	}

	public void debugNext() {
		this.debugNext = true;
	}

	public static Object getValue(MySQL mysql, String columnName)
			throws FatalException {
		Object value;
		Object rawValue = mysql.getColumn(columnName);
		try {
			value = (String) rawValue;
		} catch (ClassCastException e) {
			try {
				value = new String((byte[]) rawValue);
			} catch (ClassCastException e2) {
				try {
					value = (Integer) rawValue;
				} catch (ClassCastException e3) {
					try {
						value = (Long) rawValue;
					} catch (ClassCastException e4) {
						try {
							value = (Date) rawValue;
						} catch (ClassCastException e5) {
							value = getCountValue(mysql, columnName);
						}
					}
				}
			}
		}
		return value;
	}

	public static double getCountValue(MySQL mysql, String columnName)
			throws FatalException {
		double count;
		Object rawValue = mysql.getColumn(columnName);
		try {
			count = (Double) rawValue;
		} catch (ClassCastException e) {
			try {
				count = ((BigDecimal) rawValue).doubleValue();
			} catch (ClassCastException e2) {
				try {
					count = ((BigInteger) rawValue).doubleValue();
				} catch (ClassCastException e3) {
					logger.warn("Failed to guess type of columnName=" + columnName);
					throw e3;
				}
			}
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(byte[] input) throws FatalException {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(input);
			ObjectInputStream ois = new ObjectInputStream(bis);
			Object object = ois.readObject();
			return (T) object;
		} catch (IOException e) {
			throw new FatalException(e);
		} catch (ClassNotFoundException e) {
			throw new FatalException(e);
		}
	}

	private String getCaller(boolean withResultSet) {
		Throwable t = new Throwable();
		StackTraceElement[] elements = t.getStackTrace();

		// find the most recently opened result set
		for (int depth = 2; depth < elements.length; depth++) {
			String callerMethodName = elements[depth].getMethodName();
			String callerClassName = elements[depth].getClassName();
			String caller = callerClassName + "." + callerMethodName;
			if (!withResultSet || callerToResultSet.containsKey(caller)) {
				return caller;
			}
		}
		String caller = elements[2].getClassName() + "." + elements[2].getMethodName();
		return caller;
	}

}
