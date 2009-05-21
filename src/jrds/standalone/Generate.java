package jrds.standalone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jrds.RdsHost;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.OID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Generate {
	static {
		jrds.JrdsLoggerConfiguration.initLog4J();
	}
	static final private Logger logger = Logger.getLogger(Generate.class);
	static final private OutputFormat of = new OutputFormat("XML","UTF-8",true);

	static final private Comparator<Map.Entry<OID, Object>>c = new Comparator<Map.Entry<OID, Object>>(){
		public int compare(Entry<OID, Object> o1, Entry<OID, Object> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}

	};

	public static void main(String[] args) throws Exception {
		RdsHost host = null;
		SnmpStarter sstarter = new SnmpStarter();

		for(int i=0; i < args.length; i++) {
			String arg = args[i];
			if("-h".equals(arg)) {
				host = new RdsHost(args[++i]);
				sstarter.setHostname(host.getName());
			}
			else if("-v".equals(arg)) {
				sstarter.setVersion(args[++i]);

			}
			else if("-c".equals(arg)) {
				sstarter.setCommunity(args[++i]);
			}
		}
		SnmpStarter.full.register(host);
		sstarter = (SnmpStarter) sstarter.register(host);
		host.getStarters().startCollect();

		DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document hostDom = dbuilder.newDocument();

		Element hostEleme = hostDom.createElement("host");
		hostDom.appendChild(hostEleme);

		Element snmpElem = hostDom.createElement("snmp");
		snmpElem.setAttribute("community", ((CommunityTarget)sstarter.getTarget()).getCommunity().toString());
		snmpElem.setAttribute("version", Integer.toString(sstarter.getTarget().getVersion()));
		hostDom.adoptNode(snmpElem);

		enumerateInterfaces(hostEleme, sstarter);
		host.getStarters().stopCollect();

		of.setIndent(1);
		of.setIndenting(true);

		XMLSerializer serializer = new XMLSerializer(System.out,of);
		serializer.asDOMSerializer();
		serializer.serialize(hostDom.getDocumentElement() );

	}

	static private void enumerateInterfaces(Element hostEleme, SnmpStarter s) {
		List<Map.Entry<OID, Object>> IfsList = Collections.emptyList();
		try {
			Collection<OID> oidsSet = Collections.singleton(new OID(".1.3.6.1.2.1.2.2.1.2"));
			Map<OID, Object>  allIfs = SnmpRequester.TABULAR.doSnmpGet(s, oidsSet);
			IfsList = new ArrayList<Map.Entry<OID, Object>>(allIfs.size());
			IfsList.addAll(allIfs.entrySet());
			Collections.sort(IfsList, c);
		} catch (IOException e) {
			logger.error("Resolving interfaces: " + e.getMessage());
		}

		for(Map.Entry<OID, Object>e: IfsList) {
			String ifName = (String) e.getValue();
			int index = e.getKey().last();

			Element rrdElem = hostEleme.getOwnerDocument().createElement("rrd");
			rrdElem.setAttribute("type", "IfXSnmp");
			Element arg1 = hostEleme.getOwnerDocument().createElement("arg");
			arg1.setAttribute("type", "String");
			arg1.setAttribute("value", ifName);

			Element arg2 = hostEleme.getOwnerDocument().createElement("arg");
			arg2.setAttribute("type", "OID");
			arg2.setAttribute("value", Integer.toString(index));

			String label = getLabel(s, Collections.singletonList(new OID(".1.3.6.1.4.1.9.2.2.1.1.28." + index)));
			if(label != null) {
				rrdElem.setAttribute("label", label);
			}
			rrdElem.appendChild(arg1);
			rrdElem.appendChild(arg2);
			hostEleme.appendChild(rrdElem);
		}
	}

	static private String getLabel(SnmpStarter s, List<OID> labelsOID) {
		try {
			Map<OID, Object> ifLabel = SnmpRequester.RAW.doSnmpGet(s, labelsOID);
			for(Map.Entry<OID, Object> labelEntry: ifLabel.entrySet()) {
				String label = labelEntry.getValue().toString();
				if(label.length() >= 1)
					return label;
			}
		} catch (IOException e) {
			logger.error("Resolving labels: " + e.getMessage());
		}

		return null;
	}
}
