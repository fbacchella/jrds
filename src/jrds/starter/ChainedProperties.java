/**
 * 
 */
package jrds.starter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChainedProperties extends Starter implements Map<String, String> {
	private final Map<String, String> properties = new HashMap<String, String>();
	public ChainedProperties parent = null;

	public ChainedProperties() {
	}

	public ChainedProperties(Map<String, String> prop) {
		properties.putAll(prop);
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
			retValue.append(" followed by " + parent.toString());
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
		return properties.containsKey(key) || (parent !=null ? parent.containsKey(key) : false);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return properties.containsValue(value) || (parent !=null ? parent.containsValue(value) : false);
	}

	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		Set<java.util.Map.Entry<String, String>> entries = new HashSet<java.util.Map.Entry<String, String>>(size());
		if(parent != null)
			entries.addAll(parent.entrySet());
		entries.addAll(properties.entrySet());
		return entries;
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
		return properties.isEmpty() && (parent != null ? parent.isEmpty() : true) ;
	}

	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	public Set<String> keySet() {
		Set<String> keys = new HashSet<String>(size());
		if(parent != null)
			keys.addAll(parent.keySet());
		keys.addAll(properties.keySet());
		return keys;
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public String put(String key, String value) {
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
		return properties.size() + (parent ==null ? 0:parent.size());
	}

	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<String> values() {
		List<String> values = new ArrayList<String>(size());
		values.addAll(properties.values());
		if(parent != null)
			values.addAll(parent.values());
		return values;
	}

	/* (non-Javadoc)
	 * @see jrds.starter.Starter#initialize(jrds.starter.StarterNode, jrds.starter.StartersSet)
	 */
	@Override
	public void initialize(StarterNode parent) {
		super.initialize(parent);
		this.parent = parent.find(getClass());
	}
}
