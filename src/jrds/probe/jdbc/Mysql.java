package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

public abstract class Mysql extends JdbcProbe {
	static final private org.apache.log4j.Logger logger = Logger.getLogger(Mysql.class);

	protected final static int PORT = 3306;
	static {
		registerDriver(com.mysql.jdbc.Driver.class);
	}

	public void configure(int port, String user, String passwd) {
		super.configure(port, user, passwd);
	}

	public void configure(String user, String passwd) {
		super.configure(PORT, user, passwd);
	}

	public void configure(int port, String user, String passwd, String dbName) {
		super.configure(port, user, passwd, dbName);
	}

	public void configure(String user, String passwd, String dbName) {
		super.configure(PORT, user, passwd, dbName);
	}

	JdbcStarter setStarter() {
		return new JdbcStarter() {
			@Override
			public boolean start() {
				logger.trace("Getting uptime for " + this);
				boolean started = super.start();
				long uptime = 0;
				if(started) {
					Statement stmt;
					try {
						stmt = getStatment();
						if(stmt.execute("SHOW STATUS LIKE 'Uptime';")) {
							ResultSet rs = stmt.getResultSet();
							while(rs.next()) {
								String key =  rs.getObject(1).toString();
								String oValue = rs.getObject(2).toString();
								if("Uptime".equals(key)) {
									uptime = Long.parseLong(oValue);
									break;
								}
							}
						}
					} catch (SQLException e) {
						logger.error("SQL exception while getting uptime for " + this);
					} catch (NumberFormatException ex) {
						logger.error("Uptime not parsable for " + this);
					}
				}
				setUptime(uptime);
				logger.trace(this + "is started: " + started);
				return started;
			}
			public String getUrlAsString() {
				return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDbName();
			}
			@Override
			public Properties getProperties() {
				Properties p = super.getProperties();
				p.put("connectTimeout", 10000);
				p.put("socketTimeout", 10000);
				return p;
			}

		};
	}
}
