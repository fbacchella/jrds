package jrds.probe.jdbc;

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

	JdbcStarter setStarter() {
		return new JdbcStarter() {
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
