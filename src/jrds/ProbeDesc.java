/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdException;
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

	private static final class DsType {
		private String id;
		private DsType(String id) { this.id = id ;}
		public String toString() { return id;}
		static public final DsType NONE = new DsType("NONE");
		static public final DsType COUNTER = new DsType("COUNTER");
		static public final DsType GAUGE = new DsType("GAUGE");
		static public final DsType DERIVE = new DsType("DERIVE");
		static public final DsType ABSOLUTE = new DsType("ABSOLUTE");
	};

	static public final DsType NONE = DsType.NONE;
	static public final DsType COUNTER = DsType.COUNTER;
	static public final DsType GAUGE = DsType.GAUGE;
	static public final DsType DERIVE = DsType.DERIVE;
	static public final DsType ABSOLUTE = DsType.ABSOLUTE;
	static public final double MINDEFAULT = 0;
	static public final double MAXDEFAULT = Double.NaN;
	static public final long HEARTBEATDEFAULT = 600;



	private Map<String, DsDesc> dsMap;
	private String probeName;
	private String name;
	private Collection namedProbesNames;
	private Collection graphClasses = new ArrayList(0);
	private OID indexOid = null;
	private String rmiClass = null;
	private SnmpRequester requester = SnmpRequester.RAW;
	private boolean uniqIndex = false;
	private Class probeClass = null;


	private final class DsDesc {
		public Object key;
		public DsType dsType;
		public long heartbeat;
		public double minValue;
		public double maxValue;
		public DsDesc(DsType dsType, long heartbeat, double minValue, double maxValue, Object key)
		{
			this.key = key;
			this.dsType = dsType;
			this.heartbeat = heartbeat;
			this.minValue = minValue;
			this.maxValue = maxValue;
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
		dsMap.put(name, new DsDesc(dsType, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, name));
	}

	public void add(String name, DsType dsType, double min, double max)
	{
		dsMap.put(name, new DsDesc(dsType, HEARTBEATDEFAULT, min, max, name));
	}

	public void add(String dsName, DsType dsType, String probeName)
	{
		dsMap.put(dsName, new DsDesc(dsType, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, probeName));
	}

	public void add(String dsName, DsType dsType, String probeName, double min, double max)
	{
		dsMap.put(dsName, new DsDesc(dsType, HEARTBEATDEFAULT, min, max, probeName));
	}

	/** Add a SNMP probe what will be stored
	 * @param name
	 * @param dsType
	 * @param oid
	 */
	public void add(String name, DsType dsType, OID oid)
	{
		dsMap.put(name, new DsDesc(dsType, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, oid));
	}

	public void add(String name, DsType dsType, OID oid, double min, double max)
	{
		dsMap.put(name, new DsDesc(dsType, HEARTBEATDEFAULT, min, max, oid));
	}

	/**Add a SNMP probe not to be stored
	 * @param name
	 * @param oid
	 */
	public void add(String name, OID oid)
	{
		dsMap.put(name, new DsDesc(null, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, oid));
	}

	public void add(String name, DsType dsType, Object index, double min, double max)
	{
		dsMap.put(name, new DsDesc(null, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, index));
	}

	public void add(Map valuesMap)
	{
		long heartbeat = HEARTBEATDEFAULT;
		double min = MINDEFAULT;
		double max = MAXDEFAULT;
		Object index = null;
		String name = null;
		DsType type = null;
		for(Iterator i = valuesMap.entrySet().iterator() ; i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			String var = (String)e .getKey();
			if("dsName".equals(var))
				name = (String) e.getValue();
			else if("dsType".equals(var))
				type = (DsType) e.getValue();
			else if("index".equals(var))
				index = e.getValue();
			if(index == null && name != null)
				index = name;
		}
		dsMap.put(name, new DsDesc(type, heartbeat, min, max, index));
	}

	/**
	 * Return a map that translate an OID to the datastore name
	 * @return a Map of oid to datastore name
	 */
	public Map<OID, String> getOidNameMap()
	{
		Map<OID, String> retValue = new LinkedHashMap<OID, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd = e.getValue();
			if(dd.key != null && dd.key instanceof OID)
				retValue.put((OID)dd.key, e.getKey());
		}
		return retValue;
	}

	/**
	 * Return a map that translate an String probe name to the datastore name
	 * @return a Map of probe name to datastore name
	 */
	public Map<String, String> getProbesNamesMap()
	{
		Map<String, String> retValue = new LinkedHashMap<String, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd =  e.getValue();
			if(dd.key != null  && dd.key instanceof String)
				retValue.put((String)dd.key, e.getKey());
		}
		return retValue;
	}

	public Map<Object, String> getDsNameMap() {
		Map<Object, String> retValue = new LinkedHashMap<Object, String>(dsMap.size());
		for(Map.Entry<String, DsDesc> e: dsMap.entrySet()) {
			DsDesc dd = e.getValue();
			if(dd.key != null )
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}

	public DsDef[] getDsDefs() throws RrdException
	{
		List<DsDef> dsList = new ArrayList<DsDef>(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc desc = (DsDesc) e.getValue();
			if(desc.dsType != DsType.NONE && desc.dsType != null)
				dsList.add(new DsDef((String) e.getKey(), desc.dsType.toString(), desc.heartbeat, desc.minValue, desc.maxValue));
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
	 * @return Returns the muninsProbeName.
	 */
	public Collection getNamedProbesNames() {
		return namedProbesNames;
	}

	public void setNamedProbesNames(Collection muninsProbesNames) {
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
	 * @return Returns the index.
	 */
	public OID getIndexOid() {
		return indexOid;
	}
	/**
	 * @param index The index to set.
	 */
	public void setIndexOid(OID index) {
		this.indexOid = index;
	}

	public void setIndexOid(String index) {
		this.indexOid = new OID(index);
	}

	public void setRequester(SnmpRequester requester) {
		this.requester = requester;
	}

	public SnmpRequester getRequester() {
		return requester;
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

	/**
	 * Instanciate a probe for this probe description
	 * @param constArgs
	 * @return
	 */
	public Probe makeProbe(List constArgs, Properties prop) {
		Probe retValue = null;
		if (probeClass != null) {
			Object o = null;
			try {
				Class[] constArgsType = new Class[constArgs.size()];
				Object[] constArgsVal = new Object[constArgs.size()];
				int index = 0;
				for (Iterator i = constArgs.iterator(); i.hasNext(); index++) {
					Object arg = i.next();
					constArgsType[index] = arg.getClass();
					constArgsVal[index] = arg;
				}
				Constructor theConst = probeClass.getConstructor(constArgsType);
				o = theConst.newInstance(constArgsVal);
				retValue = (Probe) o;
				retValue.setPd(this);
			}
			catch (ClassCastException ex) {
				logger.warn("didn't get a Probe but a " + o.getClass().getName());
			}
			catch(InstantiationException ex) {
				logger.warn("Instantation exception : " + ex.getCause().getMessage(),
						ex.getCause());
			}
			catch (Exception ex) {
				Throwable showException = ex;
				Throwable t = ex.getCause();
				if(t != null)
					showException = t;
				logger.warn("Error during probe creation of type " + getName() + " with args " + constArgs +
						": ", showException);
			}
		}
		return retValue;
	}


	public Class getProbeClass() {
		return probeClass;
	}

	public void setProbeClass(Class probeClass) {
		this.probeClass = probeClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRmiClass() {
		return rmiClass;
	}

	public void setRmiClass(String rmiClass) {
		this.rmiClass = rmiClass;
	}

	public void dumpAsXml(Class c) throws ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();  // Create from whole cloth
		Element root = 
			(Element) document.createElement("probedesc"); 
		document.appendChild(root);

		Element nameElement = document.createElement("name");
		nameElement.appendChild(document.createTextNode(c.getSimpleName()));
		root.appendChild(nameElement);
		
		Element probeNamElement = document.createElement("probeName");
		probeNamElement.appendChild(document.createTextNode(probeName));
		root.appendChild(probeNamElement);

		Element probeClassElement = document.createElement("probeClass");
		probeClassElement.appendChild(document.createTextNode(c.getName()));
		root.appendChild(probeClassElement);

		if(jrds.probe.snmp.SnmpProbe.class.isAssignableFrom(c)) {
			Element requesterElement = document.createElement("snmpRequester");
			root.appendChild(requesterElement);
			requesterElement.appendChild(document.createTextNode(requester.getName()));

			if(jrds.probe.snmp.RdsIndexedSnmpRrd.class.isAssignableFrom(c)) {
				Element indexElement = document.createElement("index");
				indexElement.appendChild(document.createTextNode(this.indexOid.toString()));
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
			if(ds.key instanceof org.snmp4j.smi.OID) {
				keyName = "oid";
			}
			if(keyName != null) {
				Element keyElement = document.createElement(keyName);
				keyElement.appendChild(document.createTextNode(ds.key.toString()));
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
				graphName = ((Class) o).getName();
			if(graphName != null) {
				Element graphNameElement = document.createElement("name");
				graphElement.appendChild(graphNameElement);
				graphNameElement.appendChild(document.createTextNode(graphName));
			}
		}
	
		FileOutputStream fos = new FileOutputStream("desc/autoprobe/" + c.getSimpleName().toLowerCase() + ".xml");
//		 XERCES 1 or 2 additionnal classes.
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setIndenting(true);
		of.setDoctype("-//jrds//DTD Graph Description//EN","urn:jrds:graphdesc");
		XMLSerializer serializer = new XMLSerializer(fos,of);
//		 As a DOM Serializer
		serializer.asDOMSerializer();
		serializer.serialize( document.getDocumentElement() );
	}
}
