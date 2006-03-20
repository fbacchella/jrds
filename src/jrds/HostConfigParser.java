//----------------------------------------------------------------------------
//$Id$
package jrds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrds.probe.SumProbe;
import jrds.probe.snmp.SnmpProbe;
import jrds.snmp.TargetFactory;

import org.apache.log4j.Logger;
import org.snmp4j.Target;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Used to parse config.xml
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class HostConfigParser  extends DefaultHandler {
	
	static private final Logger logger = Logger.getLogger(HostConfigParser.class);
	
	//Tag names
	private final String HOST = "host";
	private final String SNMP = "snmp";
	private final String SNMPCOMMUNITY = "community";
	private final String SNMPVERSION = "version";
	private final String SNMPHOST = "host";
	private final String RRD = "rrd";
	private final String PROBE = "probe";
	private final String ARG = "arg";
	private final String TYPE = "type";
	private final String HOSTNAME = "name";
	private final String ARGTYPE = "type";
	private final String ARGVALUE = "value";
	private final String INCLUDE = "include";
	private final String INCLUDE_NAME = "name";
	private final String MUNINS = "munins";
	private final String PORT = "port";
	private final String SUM = "sum";
	private final String SUMNAME = "name";
	private final String SUMELEMENT = "element";
	private final String SUMELEMENTNAME = "name";
	private final String MACRODEF = "macrodef";
	private final String MACROUSE = "macro";
	private final String MACRONAME = "name";
	
	private final static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	private final static TargetFactory targetFactory = TargetFactory.getInstance();
	private Set hostsCollection;
	private RdsHost lastHost;
	private Target lastSnmpTarget;
	private List argsListValue;
	private String probeType;
	private File hostConfigFile;
	private Collection list;
	private String name;
	private Macro lastMacro = null;
	private RdsHost sumhost =  new RdsHost("SumHost");
	static {
		saxFactory.setNamespaceAware(true);
		saxFactory.setValidating(true);		
	}
	
	public HostConfigParser(File hostConfigFile)
	{
		this.hostConfigFile = hostConfigFile;
	}
	
	/**
	 * Parse the config file
	 * @return all the valid hosts found in the config file
	 */
	public Collection parse()
	{
		hostsCollection = new HashSet();
		hostsCollection.add(sumhost);
		try {
			SAXParser saxParser = saxFactory.newSAXParser();
			logger.debug("Parsing " + hostConfigFile.getAbsoluteFile().getCanonicalPath());
			saxParser.parse(hostConfigFile, this);
		} catch (Exception e) {
			logger.warn("SAX error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": " + e.getMessage(), e);
		}
		return hostsCollection;
	}
	
	/**
	 * Receive notification of the start of an element.
	 * @param namespaceURI - The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param localName - The local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName - The qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param atts - The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
	 */
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	{
		try {
			if (HOST.equals(localName)){
				String name = atts.getValue(HOSTNAME);
				lastHost = new RdsHost(name);
				lastSnmpTarget = null;
			}
			else if( RRD.equals(localName) || PROBE.equals(localName) ) {
				probeType = atts.getValue(TYPE);
				argsListValue = new ArrayList(5);
				//argsListValue.add(lastHost);
			}
			else if(argsListValue != null && ARG.equals(localName)) {
				String type = atts.getValue(ARGTYPE);
				String value = atts.getValue(ARGVALUE);
				Object newArgs = ProbeFactory.makeArg(type, value); 
				if(newArgs  != null)
					argsListValue.add(newArgs);
				else {
					logger.warn("Loading of " + probeType + 
							" for host " + lastHost.getName() + " impossible");
					argsListValue = null;
					probeType = null;
				}
			}
			else if(lastHost != null && SNMP.equals(localName)) {
				targetFactory.prepareNew();
				targetFactory.setCommunity(atts.getValue(SNMPCOMMUNITY));
				targetFactory.setVersion(atts.getValue(SNMPVERSION));
				targetFactory.setPort(atts.getValue(PORT));
				String hostName = atts.getValue(SNMPHOST);
				if(hostName == null)
					hostName = lastHost.getName();
				targetFactory.setHostname(hostName);
				lastSnmpTarget =  targetFactory.makeTarget();
				if(probeType == null) {
					lastHost.setTarget(lastSnmpTarget);
					lastSnmpTarget = null;
				}
			}
			else if(INCLUDE.equals(localName)) {
				String oldwd = System.getProperty("user.dir");
				System.setProperty("user.dir", hostConfigFile.getAbsoluteFile().getParent());
				HostsList.getRootGroup().append(new File(atts.getValue(INCLUDE_NAME)));
				System.setProperty("user.dir", oldwd);
			}
			else if(MUNINS.equals(localName)) {
				
			}
			else if(SUM.equals(localName)) {
				name = atts.getValue(SUMNAME);
				list = new ArrayList();
			}
			else if(SUMELEMENT.equals(localName)) {
				if(list != null) {
					list.add(atts.getValue(SUMELEMENTNAME));
				}
			}
			else if (MACRODEF.equals(localName)){
				String name = atts.getValue(MACRONAME);
				lastMacro = new Macro();
				HostsList.getRootGroup().getMacroList().put(name, lastMacro);
				logger.debug("New macro " + name);
			}
			else if(MACROUSE.equals(localName)) {
				String name = atts.getValue(MACRONAME);
				Macro m = (Macro) HostsList.getRootGroup().getMacroList().get(name);
				if(m != null) {
					logger.debug("Will populate " + lastHost + " with macro " + name);
					m.populate(lastHost);
				}
				else
					logger.error("Macro " + name + " not found");
			}
		} catch (RuntimeException e) {
			logger.warn("error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": " + e.getMessage(), e);
		}
	}
	
	public void endElement(String namespaceURI, String localName, String qName)
	{
		try {
			if (lastHost != null && HOST.equals(localName)){
				hostsCollection.add(lastHost);
				lastHost = null;
			}
			else if ( RRD.equals(localName) || PROBE.equals(localName) ) {
				if(lastHost != null) {
					Probe newProbe = ProbeFactory.makeProbe(probeType, lastHost, argsListValue);
					if(lastSnmpTarget != null && newProbe instanceof SnmpProbe) {
						((SnmpProbe)newProbe).setSnmpTarget(lastSnmpTarget);
					}
					lastSnmpTarget = null;
					
					if(newProbe != null) {
						lastHost.addProbe(newProbe);
						logger.debug("adding probe " + newProbe );
					}
				}
				else if(lastMacro != null) {
					lastMacro.put(probeType, argsListValue);
				}
				else
					logger.warn("What to do with the probe of type " + probeType);
				probeType = null;
				argsListValue = null;
			}
			else if(SUM.equals(localName)) {
				Probe newRdsRrd = new SumProbe(sumhost, name, list);
				sumhost.addProbe(newRdsRrd);
				logger.debug("adding probe " + newRdsRrd );
				list = null;
				name = null;
			}
			else if(MACRODEF.equals(localName)) {
				lastMacro = null;
			}
		} catch (RuntimeException e) {
			logger.warn("error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": " + e.getMessage(), e);
		}
	}	
}
