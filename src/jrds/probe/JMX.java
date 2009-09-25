package jrds.probe;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import jrds.ConnectedProbe;
import jrds.Probe;
import jrds.ProbeDesc;

import org.apache.log4j.Logger;

/**
 * 
 * @author Fabrice Bacchella 
 * @version $Revision: 407 $,  $Date: 2007-02-22 18:48:03 +0100 (jeu., 22 févr. 2007) $
 */
public class JMX extends Probe<String, Double> implements ConnectedProbe {

	static final private Logger logger = Logger.getLogger(JMX.class);

	private String connectionName = JMXConnection.class.getName();
	private Map<String, String> collectKeys = null;

	public boolean configure() {
		collectKeys = new HashMap<String, String>();
		for(Map.Entry<String, String> e:getPd().getCollectStrings().entrySet()) {
			String dsName = e.getValue();
			String solved = jrds.Util.parseTemplate(e.getKey(), this);
			collectKeys.put(solved, dsName);
		}
		return true;
	}


	@Override
	public Map<String, Double> getNewSampleValues() {
		JMXConnection cnx = (JMXConnection) getStarters().find(connectionName);
		if( !cnx.isStarted()) {
			return Collections.emptyMap();
		}
		MBeanServerConnection mbean = (MBeanServerConnection) cnx.getConnection();
		//Uptime is collected only once, by the connexion
		setUptime(cnx.getUptime());

		try {

			Set<String> collectKeys = getCollectMapping().keySet();
			Map<String, Double> retValues = new HashMap<String, Double>(collectKeys.size());

			logger.debug(collectKeys);
			for(String collect: collectKeys) {
				String[] jmxPathArray = collect.split("/");
				List<String> jmxPath = new ArrayList<String>();
				jmxPath.addAll(Arrays.asList(jmxPathArray));
				ObjectName mbeanName = new ObjectName(jmxPath.remove(0));
				String attributeName =  jmxPath.remove(0);
				try {
					Object attr = mbean.getAttribute(mbeanName, attributeName);
					Number v = resolvJmxObject(jmxPath, attr);
					logger.debug("JMX Path: " + collect +" = " + v);
					retValues.put(collect, v.doubleValue());
				} catch (AttributeNotFoundException e1) {
					logger.error("Invalide JMX attribue" +  attributeName + " for " + this);
				} catch (InstanceNotFoundException e1) {
					logger.error("JMX instance not found for " + this + ": ");
				} catch (MBeanException e1) {
					logger.error("JMX MBeanException for " + this + ": " + e1);
				} catch (ReflectionException e1) {
					logger.error("JMX reflection error for " + this + ": " + e1);
				} catch (IOException e1) {
					logger.error("JMX IO error for " + this + ": " + e1);
				}
			}
			return retValues;
		} catch (MalformedObjectNameException e) {
			logger.error("JMX name error for " + this + ": " + e);
		} catch (NullPointerException e) {
			logger.error("JMX error for " + this + ": " + e);
		}

		return Collections.emptyMap();
	}

	@Override
	public String getSourceType() {
		return "JMX";
	}

	/**
	 * Try to extract a numerical value from a jmx Path pointing to a jmx object
	 * If the attribute (element 0) of the path is a :
	 * - Set, array or TabularData, the size is used
	 * - Map, the second element is the key to the value
	 * - CompositeData, the second element is the key to the value
	 * @param jmxPath
	 * @param o
	 * @return
	 */
	Number resolvJmxObject(List<String> jmxPath, Object o) {
		Object value = null;
		if(o instanceof CompositeData) {
			String subKey = jmxPath.remove(0);
			CompositeData co = (CompositeData) o;
			value = co.get(subKey);
		}
		else if(o instanceof Number)
			return (Number) o;
		else if(o instanceof Map) {
			String subKey = jmxPath.remove(0);
			value = ((Map<?, ?>) o).get(subKey);
		}
		else if(o instanceof Collection) {
			return ((Collection<?>) o ).size();
		}
		else if(o instanceof TabularData) {
			return ((TabularData) o).size();
		}
		else if(o.getClass().isArray()) {
			return Array.getLength(o);
		}
		if(value instanceof Number) {
			return ((Number) value);
		}
		else if(value instanceof String) {
			return jrds.Util.parseStringNumber((String) value, Double.class, Double.NaN);
		}
		return Double.NaN;
	}
	
	/* (non-Javadoc)
	 * @see jrds.Probe#setPd(jrds.ProbeDesc)
	 */
	@Override
	public void setPd(ProbeDesc pd) {
		super.setPd(pd);
		collectKeys = getPd().getCollectStrings();
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getCollectkeys()
	 */
	@Override
	public Map<String, String> getCollectMapping() {
		return collectKeys;
	}

	/**
	 * @return the connection
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

}
