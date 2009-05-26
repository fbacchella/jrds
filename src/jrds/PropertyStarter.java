/**
 * 
 */
package jrds;

import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 */
public class PropertyStarter extends Starter {
	static final private Logger logger = Logger.getLogger(PropertyStarter.class);

	static final public String KEY="PROPERTIES";
	private final Properties properties= new Properties();
	private PropertyStarter parent = null;
	public void setProp(String key, String value) {
		logger.trace("New property: " + key + "=" + value + " for " + getParent());
		properties.put(key, value);
	}
	
	/**
	 * Return the value of a property
	 * If key begins with the string "system.", it looks for the property in the system properties
	 * @param key
	 * @return the property found or null if it's not found
	 */
	public String getProp(String key) {
		if(key.startsWith("system.")) {
			return System.getProperty(key.replace("system.", ""));
		}
		String value = properties.getProperty(key);
		if(value == null && parent != null) {
			value = parent.getProp(key);
		}
		return value;
	}

	/* The key is static, there is only one for each level
	 * @see jrds.Starter#getKey()
	 */
	@Override
	public Object getKey() {
		return KEY;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#register(jrds.StarterNode)
	 */
	@Override
	public Starter register(StarterNode node) {
		parent = (PropertyStarter) node.getStarters().find(KEY);
		return super.register(node);
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#toString()
	 */
	@Override
	public String toString() {
		StringBuilder retValue = new StringBuilder();
		retValue.append("Properties(" + properties.size() + ") for " + getParent());
		if(parent != null) {
			retValue.append("following " + parent.toString());
		}
		return  retValue.toString();
	}
}
