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

import jrds.factories.ProbeBean;
import jrds.starter.Connection;

import org.apache.log4j.Level;

@ProbeBean({"port, user, password"})
public class JMXConnection extends Connection<MBeanServerConnection> {

	final static String startTimeObjectName = "java.lang:type=Runtime";
	final static String startTimeAttribue = "Uptime";

	private int port;
	private String user = null;
	private String password = null;
	private JMXConnector connector;
	private MBeanServerConnection connection;

    public JMXConnection() {
        super();
    }

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

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

}
