package jrds.probe;

import java.io.IOException;
import java.lang.reflect.Array;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

import jrds.RdsHost;
import jrds.Tools;
import jrds.mockobjects.GetMoke;
import jrds.starter.Connection;
import jrds.starter.Resolver;
import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmxConnexionTest {
	static final private Logger logger = Logger.getLogger(JmxConnexionTest.class);

	@BeforeClass static public void configure() throws Exception {
		Tools.configure();
		Tools.setLevel(new String[] {JmxConnexionTest.class.getName(),jrds.probe.JMXConnection.class.getName() }, logger.getLevel());
//		RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
//		JMXConnectorServerFactory.newJMXConnectorServer(null, null, mxbean.);
	}

	@Test
	public void build1() throws MalformedObjectNameException, NullPointerException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException, IntrospectionException {
		
		
		RdsHost host = GetMoke.getHost("localhost");
		Connection<MBeanServerConnection> cnx = new JMXConnection(8998) {
			@Override
			public String getHostName() {
				return "localhost";
			}	
		};
		Resolver r = new Resolver(cnx.getHostName());
		r.register(host);
		cnx.register(host);
		boolean started = cnx.start();
		Assert.assertFalse(started);
		if(! started)
			return;
		MBeanServerConnection mbean =  cnx.getConnection();

		/*Set<ObjectInstance> s = mbean.queryMBeans(null, null);
		for(ObjectInstance o : s) {
			logger.debug("Class: " + o.getClassName());
		}*/

		/*for(String domains: mbean.getDomains()) {
			logger.debug("Domains: " + domains);
		}*/

		for(Object nameObject: mbean.queryNames(null, null)) {
			ObjectName name = (ObjectName) nameObject;
			logger.debug(name);
			MBeanInfo info = mbean.getMBeanInfo(name);
			MBeanAttributeInfo[] attrs = info.getAttributes();
			for(MBeanAttributeInfo attr : attrs) {
				if("javax.management.openmbean.TabularData".equals(attr.getType())) {
					TabularData td = (TabularData) mbean.getAttribute(name, attr.getName());
					logger.debug("    TabularData["  + td.size() +"] " + attr.getName());

				}
				else if("javax.management.openmbean.CompositeData".equals(attr.getType())) {
					CompositeData cd = (CompositeData) mbean.getAttribute(name, attr.getName());
					if(cd != null) {
						CompositeType ct = cd.getCompositeType();
						for(Object key: ct.keySet()) {
							Object value = cd.get((String)key);
							logger.debug("    "  + "    " + value.getClass().getName() + " " + key);

						}
					}
				}
				else if(attr.getType().startsWith("[")) {
					Object o = mbean.getAttribute(name, attr.getName());
					if(o == null)
						continue;
					logger.debug("    " + o.getClass().getComponentType().getName() + "[" + Array.getLength(o) + "]" + " " + attr.getName());
				}
				else {
					logger.debug("    " + attr.getType() + " " + attr.getName());
				}
			}
		}
	}

}
