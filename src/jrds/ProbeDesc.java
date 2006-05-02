/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.snmp4j.smi.OID;
import jrds.snmp.SnmpRequester;



/**
 * The description of a probe that must be used for each probe.
 * It's purpose is to make the description of a probe easier to read or write.
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ProbeDesc {
	static final private Logger logger = Logger.getLogger(ProbeDesc.class);
	
	/*private static final class DsType {
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
	static public final DsType ABSOLUTE = DsType.ABSOLUTE;*/
	static public final double MINDEFAULT = 0;
	static public final double MAXDEFAULT = Double.NaN;
	static public final long HEARTBEATDEFAULT = 600;
	
	
	
	private Map<String, DsDesc> dsMap;
	private String probeName;
	private String name;
	private Collection namedProbesNames;
	private Collection graphClasses;
	private OID indexOid = null;
	private String rmiClass = null;
	private SnmpRequester requester = SnmpRequester.RAW;
	private boolean uniqIndex = false;
	private Class probeClass = Probe.class;
	
	
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
	
	//Differets way to add a probe
	public void add(String name)
	{
		dsMap.put(name, new DsDesc(null, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, name));
	}
	

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
		dsMap.put(name, new DsDesc(dsType, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, index));
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
	public Map getOidNameMap()
	{
		Map retValue = new LinkedHashMap(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc dd = (DsDesc) e.getValue();
			if(dd.key != null && dd.key instanceof OID)
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}
	
	/**
	 * Return a map that translate an String probe name to the datastore name
	 * @return a Map of probe name to datastore name
	 */
	public Map getProbesNamesMap()
	{
		Map retValue = new LinkedHashMap(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc dd = (DsDesc) e.getValue();
			if(dd.key != null  && dd.key instanceof String)
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}
	
	public Map getDsNameMap() {
		Map retValue = new LinkedHashMap(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc dd = (DsDesc) e.getValue();
			if(dd.key != null )
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}
	
	public DsDef[] getDsDefs()
	{
		List dsList = new ArrayList(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc desc = (DsDesc) e.getValue();
			if(desc.dsType != null && desc.dsType != null)
				dsList.add(new DsDef((String) e.getKey(), desc.dsType, desc.heartbeat, desc.minValue, desc.maxValue));
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
	public Probe makeProbe(RdsHost host, List constArgs) {
		Probe retValue = null;
		if (probeClass != null) {
			Object o = null;
			try {
				Class[] constArgsType = new Class[constArgs.size() + 2 ];
				Object[] constArgsVal = new Object[constArgs.size() +2 ];
				int index = 0;
				constArgsVal[index] = host;
				constArgsType[index] = constArgsVal[index].getClass();
				index++;
				constArgsVal[index] = this;
				constArgsType[index] = constArgsVal[index].getClass();
				index++;
				for (Iterator i = constArgs.iterator(); i.hasNext(); index++) {
					Object arg = i.next();
					constArgsType[index] = arg.getClass();
					constArgsVal[index] = arg;
				}
				Constructor theConst = probeClass.getConstructor(constArgsType);
				o = theConst.newInstance(constArgsVal);
				retValue = (Probe) o;
			}
			catch (ClassCastException ex) {
				logger.warn("didn't get a Probe but a " + o.getClass().getName());
			}
			catch (Exception ex) {
				logger.warn("Error during probe creation of type " + probeClass.getName() +
						": " + ex, ex);
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
}
