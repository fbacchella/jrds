package jrds.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import jrds.ChainedProperties;
import jrds.ConnectedProbe;
import jrds.Connection;
import jrds.Macro;
import jrds.Probe;
import jrds.RdsHost;
import jrds.StarterNode;
import jrds.Threshold;
import jrds.Threshold.Comparator;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Logger;

public class HostBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(HostBuilder.class);

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
		RdsHost host = new RdsHost();
		JrdsNode hostNode = n.getChild(CompiledXPath.get("/host"));

		hostNode.setMethod(host, CompiledXPath.get("@dnsName"), "setDnsName");
		hostNode.setMethod(host, CompiledXPath.get("tag"), "addTag", false);

		hostNode.setMethod(host, CompiledXPath.get("@name"), "setName");
		host.setHostDir(pm.rrddir + org.rrd4j.core.Util.getFileSeparator() + host.getName());

		JrdsNode snmpNode = hostNode.getChild(CompiledXPath.get("snmp"));
		if(snmpNode != null) {
			SnmpStarter starter = snmpStarter(snmpNode, host);
			starter.register(host);
		}

		makeConnexion(hostNode, host);

		Map<String, String> hostprop = makeProperties(hostNode);
		if(hostprop != null) {
			ChainedProperties temp = new ChainedProperties(hostprop);
			temp.register(host);
		}

		for(JrdsNode probeNode: hostNode.iterate(CompiledXPath.get("probe | rrd"))) {
			try {
				Probe p = makeProbe(probeNode);
				if(p != null) {
					logger.trace(p);
					host.addProbe(p);
				}
				JrdsNode snmpProbeNode = probeNode.getChild(CompiledXPath.get("snmp"));
				if(snmpProbeNode != null) {
					SnmpStarter starter = snmpStarter(snmpProbeNode, host);
					starter.register(p);
				}
				Map<String, String> nodeprop = makeProperties(probeNode);
				if(nodeprop != null) {
					ChainedProperties temp = new ChainedProperties(nodeprop);
					temp.register(p);
				}
				makeConnexion(probeNode, p);

			} catch (Exception e) {
				logger.error("Probe creation failed for host " + host.getName() + ": " + e);
				e.printStackTrace();
			}
		}
		for(JrdsNode probeNode: hostNode.iterate(CompiledXPath.get("macro/@name"))) {
			String name = probeNode.getTextContent();
			logger.trace("Adding macro " + name + ": " + macrosMap.get(name));
			Macro m = macrosMap.get(name);
			if(m != null) {
				m.populate(host);
			}
		}
		return host;
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
			int port = Integer.parseInt(portStr);
			starter.setPort(port);
		}

		String hostName = attributes.get("host");
		if(hostName == null) {
			hostName = host.getName();
		}
		starter.setHostname(hostName);
		return starter;
	}

	public Probe makeProbe(JrdsNode probeNode) {
		Probe p = null;
		List<Object> args = makeArgs(probeNode);
		String type = probeNode.attrMap().get("type");
		p = pf.makeProbe(type, args);
		if(p == null)
			return null;
		for(JrdsNode thresholdNode: probeNode.iterate(CompiledXPath.get("threshold"))) {
			Map<String, String> thresholdAttr = thresholdNode.attrMap();
			String name = thresholdAttr.get("name").trim();
			String dsName = thresholdAttr.get("dsName").trim();
			double value = Double.parseDouble(thresholdAttr.get("value").trim());
			long duration = Long.parseLong(thresholdAttr.get("duration").trim());
			String operationStr = thresholdAttr.get("limit").trim();
			Comparator operation = Comparator.valueOf(operationStr.toUpperCase());

			Threshold t= new Threshold(name, dsName, value, duration, operation);
			for(JrdsNode actionNode: thresholdNode.iterate(CompiledXPath.get("action"))) {
				String actionType = actionNode.getChild(CompiledXPath.get("@type")).getTextContent().trim().toUpperCase();
				Threshold.Action a = Threshold.Action.valueOf(actionType);
				t.addAction(a, makeArgs(actionNode));
			}
			p.addThreshold(t);
		}
		String label = probeNode.evaluate(CompiledXPath.get("@label"));
		if(label != null && ! "".equals(label)) {
			logger.trace("Adding label " + label + " to " + p);
			p.setLabel(label);
		}
		if(p instanceof ConnectedProbe) {
			String connexionName = probeNode.evaluate(CompiledXPath.get("@connection"));
			if(connexionName != null && ! "".equals(connexionName)) {
				logger.trace("Adding connection " + connexionName + " to " + p);
				((ConnectedProbe)p).setConnection(connexionName);
			}
		}

		return p;
	}

	public void makeConnexion(JrdsNode domNode, StarterNode sNode) {
		for(JrdsNode cnxNode: domNode.iterate(CompiledXPath.get("connection"))) {
			List<Object> args = makeArgs(cnxNode);
			String type = cnxNode.attrMap().get("type");
			if(type == null) {
				logger.equals("No type declared");
			}
			String name = cnxNode.attrMap().get("name");
			Connection o = null;
			try {
				Class<?> connectionClass = Class.forName(type);
				Class<?>[] constArgsType = new Class[args.size()];
				Object[] constArgsVal = new Object[args.size()];
				int index = 0;
				for (Object arg: args) {
					constArgsType[index] = arg.getClass();
					constArgsVal[index] = arg;
					index++;
				}
				Constructor<?> theConst = connectionClass.getConstructor(constArgsType);
				o = (Connection)theConst.newInstance(constArgsVal);
				if(name !=null && ! "".equals(name))
					o.setName(name.trim());
				o.register(sNode);
				logger.debug("Connexion registred: " + o + " for " + sNode);
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
