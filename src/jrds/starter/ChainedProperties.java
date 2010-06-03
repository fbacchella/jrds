/**
 * 
 */
package jrds.starter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import org.apache.log4j.Logger;

public class ChainedProperties extends Starter implements Map<String, String> {
	static final private Logger logger = Logger.getLogger(ChainedProperties.class);

	private Map<String, String> properties = new HashMap<String, String>();
	private Map<String, String> parent = null;

	public ChainedProperties() {
		properties = new HashMap<String, String>();
	}

	public ChainedProperties(Map<String, String> prop) {
		properties = new HashMap<String, String>(prop.size());
		for(Entry<?, ?> e: prop.entrySet()) {
			properties.put(e.getKey().toString(), e.getValue().toString());
		}
	}

	public void chain(ChainedProperties parent) {
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#toString()
	 */
	@Override
	public String toString() {
		StringBuilder retValue = new StringBuilder();
		retValue.append("Properties(" + properties.size() + ") for " + getLevel());
		if(parent != null) {
			retValue.append("following " + parent.toString());
		}
		return  retValue.toString();
	}


	/**
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		properties.clear();
	}


	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}


	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}


	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return properties.entrySet();
	}


	/**
	 * @param o
	 * @return
	 * @see java.util.Map#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return properties.equals(o);
	}


	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public String get(Object key) {
		String value = properties.get(key);
		if(value == null && parent != null) {
			value = parent.get(key);
		}
		return value;
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return properties.isEmpty();
	}


	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	public Set<String> keySet() {
		return properties.keySet();
	}


	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public String put(String key, String value) {
		logger.trace("Adding properties " + key + ": " + value);
		return properties.put(key, value);
	}


	/**
	 * @param t
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends String, ? extends String> t) {
		properties.putAll(t);
	}


	/**
	 * @param key
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public String remove(Object key) {
		return properties.remove(key);
	}


	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	public int size() {
		return properties.size();
	}


	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<String> values() {
		return properties.values();
	}

	/* (non-Javadoc)
	 * @see jrds.starter.Starter#initialize(jrds.starter.StarterNode, jrds.starter.StartersSet)
	 */
	@Override
	public void initialize(StarterNode parent) {
		this.parent = parent.find(getClass());
		super.initialize(parent);
	}
}
