package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import jrds.Macro;
import jrds.Probe;
import jrds.ProbeFactory;
import jrds.RdsHost;
import jrds.factories.xml.JrdsNode;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Logger;

public class HostBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(HostBuilder.class);

	private ProbeFactory pf;
	private Map<String, Macro> macrosMap;

	@Override
	Object build(JrdsNode n) {
		try {
			Object o = makeRdsHost(n);
			return o;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public RdsHost makeRdsHost(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		RdsHost host = new RdsHost();

		n.setMethod(host, CompiledXPath.get("/host/@dnsName"), "setDnsName");
		n.setMethod(host, CompiledXPath.get("/host/tag"), "addTag", false);

		n.setMethod(host, CompiledXPath.get("/host/@name"), "setName");

		JrdsNode snmpNode = n.getChild(CompiledXPath.get("/host/snmp"));
		if(snmpNode != null) {
			SnmpStarter starter = snmpStarter(snmpNode, host);
			starter.register(host);
		}

		for(JrdsNode probeNode: n.iterate(CompiledXPath.get("/host/probe | /host/rrd"))) {
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

		}
		for(JrdsNode probeNode: n.iterate(CompiledXPath.get("/host/macro/@name"))) {
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
		return p;
	}

	@SuppressWarnings("unchecked")
	@Override
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
