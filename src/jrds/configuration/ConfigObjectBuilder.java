package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpression;

import jrds.PropertiesManager;
import jrds.factories.NodeListIterator;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.factories.xml.JrdsNode.FilterNode;
import jrds.webapp.RolesACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.snmp4j.smi.OID;
import org.w3c.dom.Node;

abstract class ConfigObjectBuilder<BuildObject> {
	static final private Logger logger = Logger.getLogger(ConfigObjectBuilder.class);

	public enum Properties {
		MACRO, GRAPHDESC, PROBEDESC, PROBEFACTORY, GRAPHFACTORY, LOADER, CLASSLOADER, PM, DEFAULTROLE, GRAPHMAP;
	}

	PropertiesManager pm;
	public ConfigType ct;
	
	abstract BuildObject build(JrdsNode n) throws InvocationTargetException;
	
	public ConfigObjectBuilder(ConfigType ct) {
	    this.ct = ct;
	}

	public Map<String, String> makeProperties(JrdsNode n) {
		if(n == null)
			return Collections.emptyMap();
		NodeListIterator propsNodes = n.iterate(CompiledXPath.get("properties/entry"));
		if(propsNodes.getLength() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> props = new HashMap<String, String>();
		for(JrdsNode propNode: propsNodes) {
			String key = propNode.evaluate(CompiledXPath.get("@key"));
			String value = propNode.getTextContent();
			logger.trace("Adding propertie " + key + "=" + value);
			props.put(key, value);
		}
		logger.debug("Properties map: " + props);
		return props;
	}

	/**
	 * Add a roles ACL to the object being build, but only if security was set in the properties.
	 * If the xpath match no roles, the object will have no ACL set, so it will use it's own default ACL.
	 * 
	 * @param object The object to add a role to
	 * @param n  The DOM tree where the xpath will look into
	 * @param xpath where to found the roles
	 */
	protected void doACL(WithACL object, JrdsNode n, XPathExpression xpath) {
		if(pm.security){
			List<String> roles = n.doTreeList(xpath, new FilterNode<String>() {
				@Override
				public String filter(Node input) {
					return input.getTextContent();
				}
			}
			);
			if(roles.size() > 0) {				
				object.addACL(new RolesACL(new HashSet<String>(roles)));
				object.addACL(pm.adminACL);
			}
			else {
				object.addACL(pm.defaultACL);
			}
		}
	}

	/**
	 * Extract the data store list from a DOM node, it must contains a list of ds elements
	 * @param name the name of the graph desc being build
	 * @param node a DOM node wrapped in a JrdsNode
	 * @return a list of Map describing the data sources
	 */
	protected List<Map<String, Object>> doDsList(String name, JrdsNode node) {
		if(node == null)
			return Collections.emptyList();
		List<Map<String, Object>> dsList = new ArrayList<Map<String, Object>>();
		for(JrdsNode dsNode: node.iterate(CompiledXPath.get("ds"))) {
			Map<String, Object> dsMap = new HashMap<String, Object>(4);
			for(JrdsNode dsContent: dsNode.iterate(CompiledXPath.get("*"))) {
				String element = dsContent.getNodeName();
				String textValue = dsContent.getTextContent().trim();
				Object value = textValue;
				if( element.startsWith("collect")) {
					if("".equals(value))
						value = null;
				}
				else if("dsType".equals(element)) {
					if( !"NONE".equals(textValue.toUpperCase()))
						try {
							value = DsType.valueOf(textValue.toUpperCase());
						} catch (Exception e) {
							logger.error("Invalid ds type specified for " + name + ": " + textValue);
							dsMap = null;
							break;
						}
						else
							value = null;
				}
				else if(element.startsWith("oid")) {
					value = new OID(textValue);
					element = element.replace("oid", "collect");
				}
				dsMap.put(element, value);
			}
			if(dsMap != null)
				dsList.add(dsMap);
		}
		logger.trace(jrds.Util.delayedFormatString("data store list build: %s", dsList));
		return dsList;
	}

    /**
     * @param pm the pm to set
     */
    void setPm(PropertiesManager pm) {
        this.pm = pm;
    }
}
