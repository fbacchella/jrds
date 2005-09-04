//----------------------------------------------------------------------------
//$Id$
package jrds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrds.snmp.TargetFactory;

import org.apache.log4j.Logger;
import org.snmp4j.Target;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Used to parse config.xml
 * @author Fabrice Bacchella(
 * @version $Revision$
 */
public class HostConfigParser  extends DefaultHandler {
	
	static private final Logger logger = JrdsLogger.getLogger(HostConfigParser.class);
	
	//Tag names
	private final String HOST = "host";
	private final String HOSTGROUP= "group";
	private final String SNMP = "snmp";
	private final String SNMPCOMMUNITY = "community";
	private final String SNMPVERSION = "version";
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
	
	private final static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	private final static TargetFactory targetFactory = TargetFactory.getInstance();
	private Set hostsCollection;
	private RdsHost lastHost;
	private Target lastSnmpTarget;
	private List argsListValue;
	private String probeType;
	private File hostConfigFile;
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
	 * @throws SAXException
	 * @throws IOException
	 */
	public Collection parse()
	{
		hostsCollection = new HashSet();
		try {
			SAXParser saxParser = saxFactory.newSAXParser();
			logger.debug("Parsing " + hostConfigFile.getAbsoluteFile().getCanonicalPath());
			saxParser.parse(hostConfigFile, this);
		} catch (Exception e) {
			logger.warn("error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": ", e);
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
				String group = atts.getValue(HOSTGROUP);
				if(group != null)
					lastHost.setGroup(group);
				lastSnmpTarget = null;
			}
			else if(lastHost != null && ( RRD.equals(localName) || PROBE.equals(localName)) ) {
				probeType = atts.getValue(TYPE);
				argsListValue = new ArrayList(5);
				argsListValue.add(lastHost);
			} else if(argsListValue != null && ARG.equals(localName)) {
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
				targetFactory.setHostname(lastHost.getName());
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
		} catch (RuntimeException e) {
			logger.warn("error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": ", e);
		}
	}
	
	public void endElement(String namespaceURI, String localName, String qName)
	{
		try {
			if (lastHost != null && HOST.equals(localName)){
				hostsCollection.add(lastHost);
				lastHost = null;
			}
			else if(probeType != null && ( RRD.equals(localName) || PROBE.equals(localName)) ) {
				Probe newRdsRrd = ProbeFactory.makeProbe(probeType, argsListValue);
				if(lastSnmpTarget != null && newRdsRrd instanceof SnmpProbe)
					((SnmpProbe)newRdsRrd).setSnmpTarget(lastSnmpTarget);
					
				if(newRdsRrd != null) {
					lastHost.addProbe(newRdsRrd);
					logger.debug("adding probe " + newRdsRrd );
				}
				probeType = null;
				argsListValue = null;
				lastSnmpTarget = null;
				newRdsRrd = null;
			}
		} catch (RuntimeException e) {
			logger.warn("error during parsing of host config file " + hostConfigFile.getAbsolutePath() +
					": ", e);
		}
	}	
}
