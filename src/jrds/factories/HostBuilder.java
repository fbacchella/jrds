package jrds.factories;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.ConnectedProbe;
import jrds.Macro;
import jrds.Probe;
import jrds.RdsHost;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.snmp.SnmpStarter;
import jrds.starter.ChainedProperties;
import jrds.starter.Connection;
import jrds.starter.StarterNode;

import org.apache.log4j.Logger;

public class HostBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(HostBuilder.class);

	private ClassLoader classLoader = null;

	private ProbeFactory pf;
	private Map<String, Macro> macrosMap;

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
		try {
			return makeRdsHost(n);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		} catch (IllegalAccessException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		} catch (InvocationTargetException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		} catch (ClassNotFoundException e) {
			throw new InvocationTargetException(e, HostBuilder.class.getName());
		}
	}

	public RdsHost makeRdsHost(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		JrdsNode hostNode = n.getChild(CompiledXPath.get("/host"));
		Map<String, String> hostattr = hostNode.attrMap();
		String hostName = hostattr.get("name");
		String dnsHostname = hostattr.get("dnsName");
		RdsHost host = null;
		if(hostName == null) {
			return null;
		}
		else if(dnsHostname != null) {
			host = new RdsHost(hostName, dnsHostname);
		}
		else
			host = new RdsHost(hostName);
		host.setHostDir(new File(pm.rrddir, host.getName()));

		String hidden = hostattr.get("hidden");
		host.setHidden(hidden != null && Boolean.parseBoolean(hidden));

		JrdsNode snmpNode = hostNode.getChild(CompiledXPath.get("snmp"));
		if(snmpNode != null) {
			SnmpStarter starter = snmpStarter(snmpNode, host);
			starter.register(host);
		}

		makeConnexion(hostNode, host);

		parseFragment(hostNode, host);

		return host;
	}

	private void parseFragment(JrdsNode fragment, RdsHost host) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		fragment.setMethod(host, CompiledXPath.get("tag"), "addTag", false);
		
		Map<String, Set<String>> collections = new HashMap<String, Set<String>>();
		for(JrdsNode collectionNode: fragment.iterate(CompiledXPath.get("collection"))) {
			String name = collectionNode.attrMap().get("name");
			Set<String> set = new HashSet<String>();
			collectionNode.setMethod(set, CompiledXPath.get("element"), "add", false);
			collections.put(name, set);
			logger.trace("Set added: " + set);
		}

		for(JrdsNode forNode: fragment.iterate(CompiledXPath.get("for"))) {
		String iterprop = forNode.attrMap().get("var");
		String name = forNode.attrMap().get("collection");
		Set<String> set = collections.get(name);
		if(set != null) {
			Map<String, String> properties = new HashMap<String, String>(1);
			for(String i: set) {
				properties.put(iterprop, i);
			}			
		}
		else {
			logger.error("Invalid host configuration, collection " + name + " not found");
		}
		//String set = forNode.attrMap().get("set");
//		String[] = set.split(",");
//		
	}
		for(JrdsNode probeNode: fragment.iterate(CompiledXPath.get("probe | rrd"))) {
			try {
				Probe<?,?> p = makeProbe(probeNode, host);
			} catch (Exception e) {
				logger.error("Probe creation failed for host " + host.getName() + ": " + e);
				e.printStackTrace();
			}
		}
		
		for(JrdsNode macroNode: fragment.iterate(CompiledXPath.get("macro"))) {
			String name = macroNode.attrMap().get("name");
			Macro m = macrosMap.get(name);
			logger.trace("Adding macro " + name + ": " + m);
			if(m != null) {
				Map<String, String> properties = makeProperties(macroNode);
				for(Probe<?,?> p:m.populate(host, properties)) {
					if(p != null && p.checkStore()) {
						host.getProbes().add(p);
					}
				}
			}
			else {
				logger.error("Unknown macro:" + name);
			}
		}
		Map<String, String> hostprop = makeProperties(fragment);
		if(hostprop != null) {
			ChainedProperties temp = new ChainedProperties(hostprop);
			temp.register(host);
		}
	}

	public SnmpStarter snmpStarter(JrdsNode d, RdsHost host) {
		SnmpStarter starter = new SnmpStarter();
		Map<String,String> attributes = d.attrMap();
		//Mandatory parameters
		starter.setCommunity(attributes.get("community"));
		starter.setVersion(attributes.get("version"));

		//Optional parameters
		String portStr = attributes.get("port");
		if(portStr != null && ! "".equals(portStr)) {
			int port = jrds.Util.parseStringNumber(portStr, Integer.class, 161).intValue();
			starter.setPort(port);
		}

		String hostName = attributes.get("host");
		if(hostName == null) {
			hostName = host.getDnsName();
		}
		starter.setHostname(hostName);
		return starter;
	}

	public Probe<?,?> makeProbe(JrdsNode probeNode, RdsHost host) {
		Probe<?,?> p = null;
		String type = probeNode.attrMap().get("type");
		//p = pf.makeProbe(type, host, args);
		p = pf.makeProbe(type);
		if(p == null)
			return null;
		p.setHost(host);

		//		for(JrdsNode thresholdNode: probeNode.iterate(CompiledXPath.get("threshold"))) {
		//			Map<String, String> thresholdAttr = thresholdNode.attrMap();
		//			String name = thresholdAttr.get("name").trim();
		//			String dsName = thresholdAttr.get("dsName").trim();
		//			double value = Double.parseDouble(thresholdAttr.get("value").trim());
		//			long duration = Long.parseLong(thresholdAttr.get("duration").trim());
		//			String operationStr = thresholdAttr.get("limit").trim();
		//			Comparator operation = Comparator.valueOf(operationStr.toUpperCase());
		//
		//			Threshold t= new Threshold(name, dsName, value, duration, operation);
		//			for(JrdsNode actionNode: thresholdNode.iterate(CompiledXPath.get("action"))) {
		//				String actionType = actionNode.getChild(CompiledXPath.get("@type")).getTextContent().trim().toUpperCase();
		//				Threshold.Action a = Threshold.Action.valueOf(actionType);
		//				t.addAction(a, makeArgs(actionNode));
		//			}
		//			p.addThreshold(t);
		//		}
		String label = probeNode.evaluate(CompiledXPath.get("@label"));
		if(label != null && ! "".equals(label)) {
			logger.trace("Adding label " + label + " to " + p);
			p.setLabel(label);
		}
		if(p instanceof ConnectedProbe) {
			String connexionName = probeNode.evaluate(CompiledXPath.get("@connection"));
			if(connexionName != null && ! "".equals(connexionName)) {
				logger.trace("Adding connection " + connexionName + " to " + p);
				((ConnectedProbe)p).setConnectionName(connexionName);
			}
		}
		List<Object> args = ArgFactory.makeArgs(probeNode, host);
		if( !pf.configure(p, args)) {
			logger.error(p + " configuration failed");
			return null;
		}
		if(p != null && p.checkStore()) {
			host.getProbes().add(p);
		}
		JrdsNode snmpProbeNode = probeNode.getChild(CompiledXPath.get("snmp"));
		if(snmpProbeNode != null) {
			SnmpStarter starter = snmpStarter(snmpProbeNode, host);
			starter.register(p);
		}
		Map<String, String> nodeprop = makeProperties(probeNode);
		if(nodeprop != null && nodeprop.size() > 0) {
			ChainedProperties temp = new ChainedProperties(nodeprop);
			temp.register(p);
		}
		makeConnexion(probeNode, p);
		return p;
	}

	public void makeConnexion(JrdsNode domNode, StarterNode sNode) {
		for(JrdsNode cnxNode: domNode.iterate(CompiledXPath.get("connection"))) {
			List<Object> args = ArgFactory.makeArgs(cnxNode);
			String type = cnxNode.attrMap().get("type");
			if(type == null) {
				logger.equals("No type declared");
			}
			String name = cnxNode.attrMap().get("name");
			Connection<?> o = null;
			try {
				Class<?> connectionClass = classLoader.loadClass(type);
				Class<?>[] constArgsType = new Class[args.size()];
				Object[] constArgsVal = new Object[args.size()];
				int index = 0;
				for (Object arg: args) {
					constArgsType[index] = arg.getClass();
					constArgsVal[index] = arg;
					index++;
				}
				Constructor<?> theConst = connectionClass.getConstructor(constArgsType);
				o = (Connection<?>)theConst.newInstance(constArgsVal);
				if(name !=null && ! "".equals(name))
					o.setName(name.trim());
				o.register(sNode);
				logger.debug("Connexion registred: " + o + " for " + sNode);
			}
			catch (NoClassDefFoundError ex) {
				logger.warn("Connection class not found: " + type+ ": " + ex);
			}
			catch (ClassCastException ex) {
				logger.warn("didn't get a Connection but a " + o.getClass().getName());
			}
			catch (Exception ex) {
				logger.warn("Error during connection creation of type " + type +
						": " + ex, ex);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public
	void setProperty(ObjectBuilder.properties name, Object o) {
		switch(name) {
		case CLASSLOADER:
			classLoader = (ClassLoader) o;
			break;
		case MACRO:
			macrosMap = (Map<String, Macro>) o;
			break;
		case PROBEFACTORY:
			pf = (ProbeFactory) o;
			break;
		default:
			super.setProperty(name, o);
		}
	}
}
