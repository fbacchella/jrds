package jrds.webapp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.PropertiesManager;
import jrds.factories.Loader;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Servlet implementation class AutoDetect
 */
public class Discover extends HttpServlet {
	static final private OutputFormat of = new OutputFormat("XML","UTF-8",true);
	static final private Logger logger = Logger.getLogger(Discover.class);

	private static final String CONTENT_TYPE = "application/xml";
	private static final long serialVersionUID = 1L;
	
	private static final OID sysObjectID = new OID("1.3.6.1.2.1.1.2.0");
	private static final OID linuxOID = new OID("1.3.6.1.4.1.8072.3.2.10");
	private static final OID solarisOID = new OID("1.3.6.1.4.1.8072.3.2.3");
	private static final OID windowsNT_workstation_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.1");
	private static final OID windowsNT_server_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.2");
	private static final OID windowsNT_dc_OID = new OID("1.3.6.1.4.1.311.1.1.3.1.3");

	static class LocalSnmpStarter extends SnmpStarter {
		Snmp snmp;
		static final PDUFactory factory = new DefaultPDUFactory(PDU.GETBULK);
		Target target;
		@Override
		public void doStart() {
			try {
				snmp = new Snmp(new DefaultUdpTransportMapping());
				snmp.listen();
			} catch (IOException e) {
			}
		}
		@Override
		public void doStop() {
			try {
				snmp.close();
			} catch (IOException e) {
			}
		}
		@Override
		public Snmp getSnmp() {
			return snmp;
		}
		@Override
		public Target getTarget() {
			return target;
		}
		@Override
		public PDUFactory getPdufactory() {
			return factory;
		}
		@Override
		public boolean isStarted() {
			return true;
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		String hostname = request.getParameter("host");
		boolean withOid = false;
		String withOidStr = request.getParameter("withoid");
		if(withOidStr != null)
			withOid = true;

		PropertiesManager pm = (PropertiesManager) getServletContext().getAttribute(PropertiesManager.class.getCanonicalName());

		Loader l;
		try {
			l = new Loader();
			URL graphUrl = getClass().getResource("/desc");
			if(graphUrl != null)
				l.importUrl(graphUrl);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Loader initialisation error",e);
		}

		logger.debug("Scanning " + pm.libspath + " for probes libraries");
		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}

		l.importDir(new File(pm.configdir));

		try {
			Document hostDom = generate(hostname, l.getRepository(Loader.ConfigType.PROBEDESC).values(), withOid, request.getParameterValues("tag"));

			of.setIndent(1);
			of.setIndenting(true);

			response.setContentType(CONTENT_TYPE);
			response.addHeader("Cache-Control", "no-cache");

			XMLSerializer serializer = new XMLSerializer(response.getOutputStream(),of);
			serializer.asDOMSerializer();
			serializer.serialize(hostDom.getDocumentElement() );
		} catch (IOException e) {
			logger.error(e);
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}
	}

	public Document generate(String hostname, Collection<JrdsNode> probdescs, boolean withOid, String[] tags) throws IOException, ParserConfigurationException {
		DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document hostDom = dbuilder.newDocument();

		LocalSnmpStarter active = new LocalSnmpStarter();
		IpAddress addr = new UdpAddress(InetAddress.getByName(hostname), 161);
		Target hosttarget = new CommunityTarget(addr, new OctetString("public"));
		hosttarget.setVersion(SnmpConstants.version2c);
		active.target = hosttarget;
		active.doStart();

		Element hostEleme = hostDom.createElement("host");
		hostEleme.setAttribute("name", hostname);
		hostDom.appendChild(hostEleme);

		if(tags != null)
			for(String tag: tags) {
				Element tagElem = hostDom.createElement("tag");
				tagElem.setTextContent(tag);
				hostEleme.appendChild(tagElem);
			}

		Element snmpElem = hostDom.createElement("snmp");
		if(hosttarget instanceof CommunityTarget) {
			CommunityTarget ct = (CommunityTarget) hosttarget;
			snmpElem.setAttribute("community", ct.getCommunity().toString());
		}
		snmpElem.setAttribute("version", Integer.toString( 1 + hosttarget.getVersion()));
		hostEleme.appendChild(snmpElem);

		for(JrdsNode e: probdescs) {
			String name = e.evaluate(CompiledXPath.get("/probedesc/name"));
			String index = e.evaluate(CompiledXPath.get("/probedesc/index"));
			try {
				logger.trace("Found probe" + name + " with index " + index);
				if(! "".equals(index) ) {
					String labelOid = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='labelOid']"));
					logger.debug("index OID for " + name + ": " + index);
					logger.debug("label OID for " + name + ": " + labelOid);
					try {
						enumerateIndexed(hostEleme, active, name, index, labelOid, withOid);
					} catch (Exception e1) {
						logger.error("Error discoverer " + name + "for index " + index + ": " +e1);
					}
				}
				String doesExistOid = e.evaluate(CompiledXPath.get("/probedesc/specific[@name='existOid']"));
				if(! "".equals(doesExistOid)) {
					doesExist(hostEleme, active, name, doesExistOid);
				}
			} catch (Exception e1) {
				logger.error("Error detecting + " + name + ": " +e1);
			}
		}
		active.doStop();
		return hostDom;
	}

	private void doesExist(Element hostEleme, SnmpStarter active, String name, String doesExistOid) throws IOException {
		OID OidExist = new OID(doesExistOid);
		String label = getLabel(active, Collections.singletonList(OidExist));
		if(label != null) {
			Element rrdElem = hostEleme.getOwnerDocument().createElement("probe");
			rrdElem.setAttribute("type", name);
			hostEleme.appendChild(rrdElem);			
		}
		logger.trace(name + " does exist: " + label);
	}

	private String getLabel(SnmpStarter active, List<OID> labelsOID) throws IOException {
		Map<OID, Object> ifLabel = SnmpRequester.RAW.doSnmpGet(active, labelsOID);
		for(Map.Entry<OID, Object> labelEntry: ifLabel.entrySet()) {
			String label = labelEntry.getValue().toString();
			if(label.length() >= 1)
				return label;
		}
		return null;
	}

	private void enumerateIndexed(Element hostEleme, SnmpStarter active, String name, String indexOid, String labelOid, boolean withOid ) throws IOException {
		logger.trace("Will enumerate " + indexOid);
		Set<OID> oidsSet = Collections.singleton(new OID(indexOid));
		Map<OID, Object> indexes= (Map<OID, Object>) SnmpRequester.TREE.doSnmpGet(active, oidsSet);
		logger.trace("Elements :"  + indexes);
		for(Map.Entry<OID, Object> e: indexes.entrySet()) {
			OID indexoid = e.getKey();
			String indexfName = e.getValue().toString();
			int index = indexoid.last();
			logger.trace("Append " + indexfName);
			Element rrdElem = hostEleme.getOwnerDocument().createElement("probe");
			rrdElem.setAttribute("type", name);
			Element arg1 = hostEleme.getOwnerDocument().createElement("arg");
			arg1.setAttribute("type", "String");
			arg1.setAttribute("value", indexfName.toString());
			rrdElem.appendChild(arg1);

			if(withOid) {
				Element arg2 = hostEleme.getOwnerDocument().createElement("arg");
				arg2.setAttribute("type", "OID");
				arg2.setAttribute("value", Integer.toString(index));
				rrdElem.appendChild(arg2);
			}

			OID Oidlabel = new OID(labelOid + "." + index);
			String label = getLabel(active, Collections.singletonList(Oidlabel));
			if(label != null) {
				rrdElem.setAttribute("label", label);
			}
			hostEleme.appendChild(rrdElem);
		}
	}
	
	private String operatingSystem(SnmpStarter active) throws IOException {
		Map<OID, Object> osType = SnmpRequester.RAW.doSnmpGet(active, Collections.singletonList(sysObjectID));
		OID  identity = (OID)osType.get(sysObjectID);
		
		return null;
	}
}
