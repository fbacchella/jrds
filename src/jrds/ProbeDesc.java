/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
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
public class ProbeDesc implements Cloneable {
	static final private Logger logger = Logger.getLogger(ProbeDesc.class);

	static public final double MINDEFAULT = 0;
	static public final double MAXDEFAULT = Double.NaN;

	private long heartBeatDefault = 600;
	private Map<String, DsDesc> dsMap;
	private Map<String, String> specific = new HashMap<String, String>();;
	private String probeName;
	private String name;
	private Collection<String> graphesList = new ArrayList<String>();
	private boolean uniqIndex = false;
	private Class<? extends Probe<?,?>> probeClass = null;
	private List<Object> defaultsArgs = null;
	private float uptimefactor = (float) 1.0;
	private Map<String, String> properties = null;
	private Map<String, Double> defaultValues = new HashMap<String,Double>(0);

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

	public class Joined {
		Object keyhigh;
		Object keylow;
		Joined(Object keyhigh, Object keylow) {
			this.keyhigh = keyhigh;
			this.keylow = keylow;
		}
	}
	
	Map<String, Joined> highlowcollectmap = new HashMap<String, Joined>();
		
	/**
	 * @return the highlowcollectmap
	 */
	public Map<String, Joined> getHighlowcollectmap() {
		return highlowcollectmap;
	}

	public void add(Map<String, Object> valuesMap)
	{
		long heartbeat = heartBeatDefault;
		double min = MINDEFAULT;
		double max = MAXDEFAULT;
		Object collectKey = null;
		String name = null;
		DsType type = null;
		if(valuesMap.containsKey("dsName")) {
			name = (String) valuesMap.get("dsName");
		}
		if(valuesMap.containsKey("dsType")) {
			type = (DsType) valuesMap.get("dsType");
		}
		if(valuesMap.containsKey("collect")) {
			collectKey = valuesMap.get("collect");
		}
		else if(valuesMap.containsKey("collecthigh") && valuesMap.containsKey("collectlow")) {
			Object keyHigh = valuesMap.get("collecthigh");
			Object keyLow = valuesMap.get("collectlow");
			dsMap.put(name + "high", new DsDesc(null, heartbeat, min, max, keyHigh));
			dsMap.put(name + "low", new DsDesc(null, heartbeat, min, max, keyLow));
			highlowcollectmap.put(name, new Joined(keyHigh, keyLow));
		}
		else {
			collectKey = name;
		}
		if(valuesMap.containsKey("defaultValue")) {
			defaultValues.put(name, jrds.Util.parseStringNumber(valuesMap.get("defaultValue").toString(), Double.NaN));
		}
        if(valuesMap.containsKey("minValue")) {
            min = jrds.Util.parseStringNumber(valuesMap.get("minValue").toString(), MINDEFAULT);
        }
        if(valuesMap.containsKey("maxValue")) {
            max = jrds.Util.parseStringNumber(valuesMap.get("maxValue").toString(), MAXDEFAULT);
        }
		dsMap.put(name, new DsDesc(type, heartbeat, min, max, collectKey));
	}
	
	/**
	 * Replace all the data source for this probe description with the list provided
	 * @param dsList a list of data source description as a map.
	 */
	public void replaceDs(List<Map<String, Object>> dsList) {
		defaultValues = new HashMap<String,Double>(0);
		dsMap = new HashMap<String, DsDesc>(dsList.size());
		for(Map<String, Object> dsinfo: dsList) {
			add(dsinfo);
		}
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
			if(dd.collectKey != null  && dd.collectKey instanceof String  && ! "".equals((String) dd.collectKey))
				retValue.put((String)dd.collectKey, e.getKey());
		}
		return retValue;
	}

	/**
	 * Return a map that translate the probe technical name to the datastore name
	 * @return a Map of collect names to datastore name
	 */
	public Map<Object, String> getCollectMapping() {
		Map<Object, String> retValue = new LinkedHashMap<Object, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd = e.getValue();
			if(dd.collectKey != null && dd.dsType != null)
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
		return dsList.toArray(new DsDef[dsList.size()]);
	}

	public Collection<String> getDs() {
		HashSet<String> dsList = new HashSet<String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet() ) {
			if(e.getValue().dsType != null)
				dsList.add(e.getKey());
		}
		return dsList;
	}

	public boolean dsExist(String dsName) {
		DsDesc dd = dsMap.get(dsName);
		return (dd !=null && dd.dsType != null);
	}

	/**
	 * @return The number of data store
	 */
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
	 * @return Returns the graphClasses.
	 */
	public Collection<String> getGraphClasses() {
		return graphesList;
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Collection<String> graphClasses) {
		this.graphesList = graphClasses;
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(String[] graphClasses) {
		this.graphesList = Arrays.asList(graphClasses);
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

	public Class<? extends Probe<?,?>> getProbeClass() {
		return probeClass;
	}

	public void setProbeClass(Class<? extends Probe<?,?>> probeClass) {
		this.probeClass = probeClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	/**
	 * @return the defaultValues
	 */
	public Map<String, Double> getDefaultValues() {
		return defaultValues;
	}

	public Document dumpAsXml() throws ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		Element root = 
			(Element) document.createElement("probedesc"); 
		document.appendChild(root);
		root.appendChild(document.createElement("name")).setTextContent(name);
		if(probeName != null)
			root.appendChild(document.createElement("probeName")).setTextContent(probeName);
		root.appendChild(document.createElement("probeClass")).setTextContent(probeClass.getName());
		
		//Setting specific values
		for(Map.Entry<String, String> e: specific.entrySet()) {
			Element specElement = (Element) root.appendChild(document.createElement("specific"));
			specElement.setAttribute("name", e.getKey());
			specElement.setTextContent(e.getValue());
		}
		//Setting the uptime factor
		if(uptimefactor != 1.0)
			root.appendChild(document.createElement("uptimefactor")).setTextContent(Float.toString(uptimefactor));

		//Adding all the datastores
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			Element dsElement = (Element) root.appendChild(document.createElement("ds"));
			dsElement.appendChild(document.createElement("dsName")).setTextContent(e.getKey());
			DsDesc desc = e.getValue();
			if(desc.dsType != null)
				dsElement.appendChild(document.createElement("dsType")).setTextContent(desc.dsType.toString());
			if(desc.collectKey instanceof OID)
				dsElement.appendChild(document.createElement("oid")).setTextContent(desc.collectKey.toString());
			if(desc.collectKey instanceof String)
				dsElement.appendChild(document.createElement("collect")).setTextContent(desc.collectKey.toString());
            if(desc.minValue != MINDEFAULT) 
                dsElement.appendChild(document.createElement("minValue")).setTextContent(Double.toString(desc.minValue));
            if(! Double.isNaN(desc.maxValue)) 
                dsElement.appendChild(document.createElement("maxValue")).setTextContent(Double.toString(desc.maxValue));
			
		}
		Element graphsElement = (Element) root.appendChild(document.createElement("graphs"));
		for(String graph: graphesList) {
			graphsElement.appendChild(document.createElement("name")).setTextContent(graph);
		}
		return document;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		ProbeDesc newpd = (ProbeDesc) super.clone();
		return newpd;
	}
	
	
}
