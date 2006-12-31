package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public abstract class Mysql extends JdbcProbe {

	protected final static int PORT = 3306;
	static {
		registerDriver(com.mysql.jdbc.Driver.class);
	}

	public Mysql(int port, String user, String passwd) {
		super(port, user, passwd);
	}

	public Mysql(String user, String passwd) {
		super(PORT, user, passwd);
	}

	public Mysql(int port, String user, String passwd, String dbName) {
		super(port, user, passwd, dbName);
	}

	public Mysql(String user, String passwd, String dbName) {
		super(PORT, user, passwd, dbName);
	}

	JdbcStarter setStarter() {
		return new JdbcStarter() {
			@Override
			public boolean start() {
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
					} catch (NumberFormatException ex) {
					}
				}
				setUptime(uptime);
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
