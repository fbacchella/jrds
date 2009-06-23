/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.snmp4j.smi.OID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The description of a probe that must be used for each probe.
 * It's purpose is to make the description of a probe easier to read or write.
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ProbeDesc {
	static final private Logger logger = Logger.getLogger(ProbeDesc.class);

	static public final double MINDEFAULT = 0;
	static public final double MAXDEFAULT = Double.NaN;
	
	private long heartBeatDefault = 600;
	private Map<String, DsDesc> dsMap;
	private Map<String, String> specific = new HashMap<String, String>();;
	private String probeName;
	private String name;
	private Collection<?> namedProbesNames;
	private Collection<?> graphClasses = new ArrayList(0);
	private boolean uniqIndex = false;
	private Class<? extends Probe> probeClass = null;
	private List<Object> defaultsArgs = null;
	private float uptimefactor = (float) 1.0;
	private Map<String, String> properties = null;

	private final class DsDesc {
		public DsType dsType;
		public long heartbeat;
		public double minValue;
		public double maxValue;
		public Object collectKey;
		public DsDesc(DsType dsType, long heartbeat, double minValue, double maxValue, Object key)
		{
			this.dsType = dsType;
			this.heartbeat = heartbeat;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.collectKey = key;
		}
	}

	/**
	 * Create a new Probe Description, with <it>size<it> elements in prevision
	 * @param size estimated elements number
	 */
	public ProbeDesc(int size) {
		dsMap = new LinkedHashMap<String, DsDesc>(size);
	}

	/**
	 * Create a new Probe Description
	 */
	public ProbeDesc() {
		dsMap = new LinkedHashMap<String, DsDesc>();
	}

	//Differets way to add a munins probe
	/**
	 * A datastore that is stored but not collected
	 * @param name the datastore name
	 * @param dsType
	 */
	public void add(String name, DsType dsType)
	{
		dsMap.put(name, new DsDesc(dsType, heartBeatDefault, MINDEFAULT, MAXDEFAULT, name));
	}

	public void add(String name, DsType dsType, double min, double max)
	{
		dsMap.put(name, new DsDesc(dsType, heartBeatDefault, min, max, name));
	}

	public void add(String dsName, DsType dsType, String probeName)
	{
		dsMap.put(dsName, new DsDesc(dsType, heartBeatDefault, MINDEFAULT, MAXDEFAULT, probeName));
	}

	public void add(String dsName, DsType dsType, String probeName, double min, double max)
	{
		dsMap.put(dsName, new DsDesc(dsType, heartBeatDefault, min, max, probeName));
	}

	/** Add a SNMP probe what will be stored
	 * @param name
	 * @param dsType
	 * @param oid
	 */
	public void add(String name, DsType dsType, OID oid)
	{
		dsMap.put(name, new DsDesc(dsType, heartBeatDefault, MINDEFAULT, MAXDEFAULT, oid));
	}

	public void add(String name, DsType dsType, OID oid, double min, double max)
	{
		dsMap.put(name, new DsDesc(dsType, heartBeatDefault, min, max, oid));
	}

	/**Add a SNMP probe not to be stored
	 * @param name
	 * @param oid
	 */
	public void add(String name, OID oid)
	{
		dsMap.put(name, new DsDesc(null, heartBeatDefault, MINDEFAULT, MAXDEFAULT, oid));
	}

	public void add(String name, DsType dsType, Object index, double min, double max)
	{
		dsMap.put(name, new DsDesc(null, heartBeatDefault, MINDEFAULT, MAXDEFAULT, index));
	}

	public void add(Map<String, Object> valuesMap)
	{
		long heartbeat = heartBeatDefault;
		double min = MINDEFAULT;
		double max = MAXDEFAULT;
		Object collectKey = null;
		String name = null;
		DsType type = null;
		for(Map.Entry<String, Object> e: valuesMap.entrySet()) {
			String var = e .getKey();
			if("dsName".equals(var))
				name = (String) e.getValue();
			else if("dsType".equals(var))
				type = (DsType) e.getValue();
			else if("collectKey".equals(var))
				collectKey = e.getValue();
			if(collectKey == null && name != null)
				collectKey = name;
		}
		dsMap.put(name, new DsDesc(type, heartbeat, min, max, collectKey));
	}

	/**
	 * Return a map that translate an OID to the datastore name
	 * @return a Map of collect oids to datastore name
	 */
	public Map<OID, String> getCollectOids()
	{
		Map<OID, String> retValue = new LinkedHashMap<OID, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd = e.getValue();
			if(dd.collectKey != null && dd.collectKey instanceof OID)
				retValue.put((OID)dd.collectKey, e.getKey());
		}
		return retValue;
	}

	/**
	 * Return a map that translate the probe technical name  as a string to the datastore name
	 * @return a Map of collect names to datastore name
	 */
	public Map<String, String> getCollectStrings()
	{
		Map<String, String> retValue = new LinkedHashMap<String, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd =  e.getValue();
			if(dd.collectKey != null  && dd.collectKey instanceof String)
				retValue.put((String)dd.collectKey, e.getKey());
		}
		return retValue;
	}

	/**
	 * Return a map that translate the probe technical name to the datastore name
	 * @return a Map of collect names to datastore name
	 */
	public Map<Object, String> getCollectkeys() {
		Map<Object, String> retValue = new LinkedHashMap<Object, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd = e.getValue();
			if(dd.collectKey != null)
				retValue.put(dd.collectKey, e.getKey());
		}
		return retValue;
	}

	public DsDef[] getDsDefs() 
	{
		List<DsDef> dsList = new ArrayList<DsDef>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet() ) {
			DsDesc desc = e.getValue();
			if(desc.dsType != null)
				dsList.add(new DsDef(e.getKey(), desc.dsType, desc.heartbeat, desc.minValue, desc.maxValue));
		}
		return (DsDef[]) dsList.toArray(new DsDef[dsList.size()]);
	}

	public int getSize()
	{
		return dsMap.size();
	}

	/**
	 * @return Returns the rrdName.
	 */
	public String getProbeName() {
		return probeName;
	}

	/**
	 * @param probeName The rrdName to set.
	 */
	public void setProbeName(String probeName) {
		this.probeName = probeName;
	}
	
	
	/**
	 * @return the uptimefactor
	 */
	public float getUptimefactor() {
		return uptimefactor;
	}

	/**
	 * @param uptimefactor the uptimefactor to set
	 */
	public void setUptimefactor(float uptimefactor) {
		this.uptimefactor = uptimefactor;
	}

	/**
	 * @return Returns the muninsProbeName.
	 */
	public Collection getNamedProbesNames() {
		return namedProbesNames;
	}

	public void setNamedProbesNames(Collection<String> muninsProbesNames) {
		this.namedProbesNames = muninsProbesNames;
	}

	public void setMuninsProbesNames(String[] muninsProbesNames) {
		this.namedProbesNames = Arrays.asList(muninsProbesNames);
	}
	/**
	 * @return Returns the graphClasses.
	 */
	public Collection getGraphClasses() {
		return graphClasses;
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Collection graphClasses) {
		this.graphClasses = graphClasses;
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Object[] graphClasses) {
		this.graphClasses = Arrays.asList(graphClasses);
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Class[] graphClasses) {
		this.graphClasses = Arrays.asList(graphClasses);
	}

	/**
	 * @return Returns the unicity of the index.
	 */
	public boolean isUniqIndex() {
		return uniqIndex;
	}

	/**
	 * @param uniqIndex The value of the unicity index.
	 * It's used to avoid doing too much GET if the indes is found only ounce.<p>
	 * Default value is false.
	 */
	public void setUniqIndex(boolean uniqIndex) {
		this.uniqIndex = uniqIndex;
	}

	public Class<? extends Probe> getProbeClass() {
		return probeClass;
	}

	public void setProbeClass(Class<? extends Probe> probeClass) {
		this.probeClass = probeClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*public String getRmiClass() {
		return rmiClass;
	}

	public void setRmiClass(String rmiClass) {
		this.rmiClass = rmiClass;
	}*/

	public void dumpAsXml(Class<? extends Probe> class1) throws ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();  // Create from whole cloth
		Element root = 
			(Element) document.createElement("probedesc"); 
		document.appendChild(root);

		Element nameElement = document.createElement("name");
		nameElement.appendChild(document.createTextNode(class1.getSimpleName()));
		root.appendChild(nameElement);

		Element probeNamElement = document.createElement("probeName");
		probeNamElement.appendChild(document.createTextNode(probeName));
		root.appendChild(probeNamElement);

		Element probeClassElement = document.createElement("probeClass");
		probeClassElement.appendChild(document.createTextNode(class1.getName()));
		root.appendChild(probeClassElement);

		if(jrds.probe.snmp.SnmpProbe.class.isAssignableFrom(class1)) {
			Element requesterElement = document.createElement("snmpRequester");
			root.appendChild(requesterElement);
			//requesterElement.appendChild(document.createTextNode(requester.getName()));

			if(jrds.probe.snmp.RdsIndexedSnmpRrd.class.isAssignableFrom(class1)) {
				Element indexElement = document.createElement("index");
				//indexElement.appendChild(document.createTextNode(this.indexOid.toString()));
				root.appendChild(indexElement);
				Element uniqElement = document.createElement("uniq");
				uniqElement.appendChild(document.createTextNode(Boolean.toString(this.uniqIndex)));
				root.appendChild(uniqElement);
			}
		}


		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc ds = e.getValue();
			Element dsElement = document.createElement("ds");
			root.appendChild(dsElement);

			Element dsNameElement = document.createElement("dsName");
			dsElement.appendChild(dsNameElement);
			dsNameElement.appendChild(document.createTextNode(e.getKey()));

			Element dsTypeElement = document.createElement("dsType");
			if(ds.dsType != null) {
				dsTypeElement.appendChild(document.createTextNode(ds.dsType.toString().toLowerCase()));
				dsElement.appendChild(dsTypeElement);
			}

			if(! Double.isNaN(ds.maxValue)) {
				Element upperLimitElement = document.createElement("upperLimit");
				upperLimitElement.appendChild(document.createTextNode(Double.toString(ds.maxValue)));
				root.appendChild(upperLimitElement);
			}

			if(ds.minValue != MINDEFAULT) {
				Element lowerLimitElement = document.createElement("lowerLimit");
				lowerLimitElement.appendChild(document.createTextNode(Double.toString(ds.minValue)));
				root.appendChild(lowerLimitElement);
			}

			String keyName = null;
			if(ds.collectKey instanceof org.snmp4j.smi.OID) {
				keyName = "oid";
			}
			if(keyName != null) {
				Element keyElement = document.createElement(keyName);
				keyElement.appendChild(document.createTextNode(ds.collectKey.toString()));
				dsElement.appendChild(keyElement);
			}
		}

		Element graphElement = document.createElement("graphs");
		root.appendChild(graphElement);
		for(Object o: this.graphClasses) {
			String graphName = null;
			if(o instanceof String)
				graphName = o.toString();
			else if(o instanceof Class)
				graphName = ((Class<?>) o).getName();
			if(graphName != null) {
				Element graphNameElement = document.createElement("name");
				graphElement.appendChild(graphNameElement);
				graphNameElement.appendChild(document.createTextNode(graphName));
			}
		}

		FileOutputStream fos = new FileOutputStream("desc/autoprobe/" + class1.getSimpleName().toLowerCase() + ".xml");
//		XERCES 1 or 2 additionnal classes.
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setIndenting(true);
		of.setDoctype("-//jrds//DTD Graph Description//EN","urn:jrds:graphdesc");
		XMLSerializer serializer = new XMLSerializer(fos,of);
//		As a DOM Serializer
		serializer.asDOMSerializer();
		serializer.serialize( document.getDocumentElement() );
	}

	public String getSpecific(String name) {
		return specific.get(name);
	}

	public void addSpecific(String name, String value) {
		specific.put(name, value);
	}

	public void addDefaultArg(Object o){
		if(defaultsArgs == null) 
			defaultsArgs = new LinkedList<Object>();
		defaultsArgs.add(o);
		logger.trace("Adding " + o + " (" + o.getClass() + ") to default args");
	}

	public List<Object> getDefaultArgs() {
		return defaultsArgs;
	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	/**
	 * @return the heartBeatDefault
	 */
	public long getHeartBeatDefault() {
		return heartBeatDefault;
	}

	/**
	 * @param heartBeatDefault the heartBeatDefault to set
	 */
	public void setHeartBeatDefault(long heartBeatDefault) {
		this.heartBeatDefault = heartBeatDefault;
	}
}
