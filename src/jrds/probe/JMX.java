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

import org.apache.log4j.Logger;

/**
 * 
 * @author Fabrice Bacchella 
 * @version $Revision: 407 $,  $Date: 2007-02-22 18:48:03 +0100 (jeu., 22 févr. 2007) $
 */
public class JMX extends Probe implements ConnectedProbe {

	static final private Logger logger = Logger.getLogger(JMX.class);

	private String connectionName = JMXConnection.class.getName();
	
	public boolean configure() {
		return true;
	}

	@Override
	public Map<?, ?> getNewSampleValues() {
		JMXConnection cnx = (JMXConnection) getStarters().find(connectionName);
		if( !cnx.isStarted()) {
			return Collections.EMPTY_MAP;
		}
		MBeanServerConnection mbean = (MBeanServerConnection) cnx.getConnection();
		//Uptime is collected only once, by the connexion
		setUptime(cnx.getUptime());

		try {

			Map<String, String> collectKeys = getPd().getCollectStrings();
			Map<String, Double> retValues = new HashMap<String, Double>(collectKeys.size());

			logger.debug(collectKeys);
			for(Map.Entry<String, String> e: collectKeys.entrySet()) {
				try {
					String[] jmxPathArray = e.getKey().split("/");
					List<String> jmxPath = new ArrayList<String>();
					jmxPath.addAll(Arrays.asList(jmxPathArray));
					ObjectName mbeanName = new ObjectName(jmxPath.remove(0));
					String attributeName =  jmxPath.remove(0);
					Object attr = mbean.getAttribute(mbeanName, attributeName);
					Number v = resolvJmxObject(jmxPath, attr);
					logger.debug("JMX Path: " + e.getKey() +" = " + v);
					retValues.put(e.getValue(), v.doubleValue());
				} catch (AttributeNotFoundException e1) {
					logger.error("JMX error for " + this + ": ");
				} catch (InstanceNotFoundException e1) {
					logger.error("JMX error for " + this + ": ");
				} catch (MBeanException e1) {
					logger.error("JMX error for " + this + ": ");
				} catch (ReflectionException e1) {
					logger.error("JMX error for " + this + ": ");
				} catch (IOException e1) {
					logger.error("JMX error for " + this + ": ");
				}
			}
			return retValues;
		} catch (MalformedObjectNameException e) {
			logger.error("JMX error for " + this + ": ");
		} catch (NullPointerException e) {
			logger.error("JMX error for " + this + ": ");
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
		String subKey = jmxPath.remove(0);
		if(o instanceof CompositeData) {
			CompositeData co = (CompositeData) o;
			value = co.get(subKey);
		}
		else if(o instanceof Number)
			return (Number) o;
		else if(o instanceof Map) {
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
