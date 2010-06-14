package jrds.probe;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import jrds.starter.Connection;

import org.apache.log4j.Level;

public class JMXConnection extends Connection<MBeanServerConnection> {

	final static String startTimeObjectName = "java.lang:type=Runtime";
	final static String startTimeAttribue = "Uptime";

	private int port;
	private String user = null;
	private String password = null;
	private JMXConnector connector;
	private MBeanServerConnection connection;

	public JMXConnection(Integer port) {
		super();
		this.port = port;
	}

	public JMXConnection(Integer port, String user, String password) {
		super();
		this.port = port;
		this.user = user;
		this.password = password;
	}

	@Override
	public MBeanServerConnection getConnection() {
		return connection;
	}

	@Override
	public long setUptime() {
		ObjectName objectname;
		try {
			objectname = new ObjectName(startTimeObjectName);
			Object o = connection.getAttribute(objectname, startTimeAttribue);
			long uptime = ((Number)o).longValue() / 1000;
			log(Level.DEBUG, "Uptime for %s = %ld", this, uptime);
			return uptime;
		} catch (Exception e) {
			log(Level.ERROR, e, "Uptime error for %s: %s", this, e);
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#start()
	 */
	@Override
	public boolean startConnection() {
		String uri = "service:jmx:rmi:///jndi/rmi://" + getHostName() + ":" + port + "/jmxrmi";
		try {
			JMXServiceURL jmxserviceurl = new JMXServiceURL(uri);
			Map<String, Object> attributes = null;
			if(user != null && password != null ) {
				String[] credentials = new String[]{user, password};
				attributes = new HashMap<String, Object>();
				attributes.put("jmx.remote.credentials", credentials);
			}
			connector = JMXConnectorFactory.connect(jmxserviceurl, attributes);
			connection = connector.getMBeanServerConnection();
			return true;
		} catch (MalformedURLException e) {
			log(Level.ERROR, e, "Invalid jmx URL %s: %s", uri, e);
		} catch (IOException e) {
			log(Level.ERROR, e, "Communication error with %s: %s", uri, e);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#stop()
	 */
	@Override
	public void stopConnection() {
		try {
			connector.close();
		} catch (IOException e) {
			log(Level.ERROR, e, "JMXConnector to %s close failed because of: %s", this, e );
		}
		connection = null;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#toString()
	 */
	@Override
	public String toString() {
		return "service:jmx:rmi:///jndi/rmi://" + getHostName() + ":" + port + "/jmxrmi";
	}

}
