package jrds;

import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Level;

public abstract class ProbeConnected<KeyType, ValueType, ConnectionClass extends jrds.starter.Connection<?>> extends Probe<KeyType, ValueType> implements ConnectedProbe {
	private String connectionName;
	
	public ProbeConnected(String connectionName) {
		super();
		this.connectionName = connectionName;
		log(Level.DEBUG, "New Probe connect called %s", connectionName);
	}
	
	public Boolean configure() {
		ConnectionClass cnx = getConnection();
		if(cnx == null) {
			log(Level.ERROR, "No connection configured with name %s", getConnectionName());
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see jrds.ConnectedProbe#getConnectionName()
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/* (non-Javadoc)
	 * @see jrds.ConnectedProbe#setConnectionName(java.lang.String)
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	@SuppressWarnings("unchecked")
	public ConnectionClass getConnection() {
		return (ConnectionClass) getStarters().find(getConnectionName());
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	@Override
	public Map<KeyType, ValueType> getNewSampleValues() {
		ConnectionClass cnx = getConnection();
		if(cnx == null) {
			log(Level.WARN, "No connection found with name %s", getConnectionName());
		}
		log(Level.DEBUG, "find connection %s", connectionName);
		if( cnx == null || !cnx.isStarted()) {
			return Collections.emptyMap();
		}
		//Uptime is collected only once, by the connexion
		setUptime(cnx.getUptime());
		return getNewSampleValuesConnected(cnx);
	}

	public abstract Map<KeyType, ValueType> getNewSampleValuesConnected(ConnectionClass cnx);

}
