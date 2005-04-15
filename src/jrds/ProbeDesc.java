/*
 * Created on 7 févr. 2005
 */
package jrds;

import java.util.*;

import org.apache.log4j.*;
import org.jrobin.core.*;
import org.snmp4j.smi.*;
import jrds.snmp.SnmpRequester;



/**
 * The description of a probe that must be used for each probe.
 * It's purpose is to make the description of a probe easier to read or write.
 * @author bacchell
 *
 * TODO
 */
public class ProbeDesc implements Cloneable {
	static final private Logger logger = JrdsLogger.getLogger(ProbeDesc.class);

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



	private Map dsMap;
	private String rrdName;
	private Collection namedProbesNames;
	private Collection graphClasses;
	private boolean cloned = false;
	private boolean readOnly = false;
	private OID indexOid = null;
	private boolean asynchronous = false;
        private SnmpRequester requester = SnmpRequester.RAW;

	private final class DsDesc {
		//public String dsName;
		//public String namedProbeName;
		//public OID oid;
		public Object key;
		public DsType dsType;
		public long heartbeat;
		public double minValue;
		public double maxValue;
		public DsDesc(/*String dsName, */DsType dsType, long heartbeat, double minValue, double maxValue, /*String namedProbeName, OID oid, */Object key)
		{
			//this.dsName = dsName;
			//this.namedProbeName = namedProbeName;
			//this.oid = oid;
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
		dsMap = new HashMap(size);
	}

	/**
	 * Create a new Probe Description
	 */
	public ProbeDesc() {
		dsMap = new HashMap();
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

	public void add(String dsName, DsType dsType, String probeName)
	{
		dsMap.put(dsName, new DsDesc(dsType, HEARTBEATDEFAULT, MINDEFAULT, MAXDEFAULT, probeName));
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

	/**
	 * Return a map that translate an OID to the datastore name
	 * @return a Map of oid to datastore name
	 */
	public Map getOidNameMap()
	{
		Map retValue = new HashMap(dsMap.size());
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
		Map retValue = new HashMap(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc dd = (DsDesc) e.getValue();
			if(dd.key != null  && dd.key instanceof String)
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}

	public Map getDsNameMap() {
		Map retValue = new HashMap(dsMap.size());
		for(Iterator i = dsMap.entrySet().iterator(); i.hasNext() ;) {
			Map.Entry e = (Map.Entry) i.next();
			DsDesc dd = (DsDesc) e.getValue();
			if(dd.key != null )
				retValue.put(dd.key, e.getKey());
		}
		return retValue;
	}

	public DsDef[] getDsDefs() throws RrdException
	{
		List dsList = new ArrayList(dsMap.size());
		int j = 0;
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
	public String getRrdName() {
		return rrdName;
	}

	/**
	 * @param rrdName The rrdName to set.
	 */
	public void setRrdName(String rrdName) {
		if(cloned || ! readOnly )
			this.rrdName = rrdName;
		else
			logger.error("RRD name tried to be set twice");
	}
	/**
	 * @return Returns the muninsProbeName.
	 */
	public Collection getNamedProbesNames() {
		return namedProbesNames;
	}

	public void setNamedProbesNames(Collection muninsProbesNames) {
		if(cloned || ! readOnly )
			this.namedProbesNames = muninsProbesNames;
		else
			logger.error("munins probe name tried to be set twice");
	}

	public void setMuninsProbesNames(String[] muninsProbesNames) {
		if(cloned || ! readOnly )
			this.namedProbesNames = Arrays.asList(muninsProbesNames);
		else
			logger.error("munins probe name tried to be set twice");
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
		if(cloned || ! readOnly )
			this.graphClasses = graphClasses;
		else
			logger.error("graph classes tried to be set twice");
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Object[] graphClasses) {
		if(cloned || ! readOnly )
			this.graphClasses = Arrays.asList(graphClasses);
		else
			logger.error("graph classes tried to be set twice");
	}

	/**
	 * @param graphClasses The graphClasses to set.
	 */
	public void setGraphClasses(Class[] graphClasses) {
		if(cloned || ! readOnly )
			this.graphClasses = Arrays.asList(graphClasses);
		else
			logger.error("graph classes tried to be set twice");
	}

	/**
	 * the clone method is automaticaly called by the all thje<code>Probe</code> methods that
	 * set an <code>ProbeDesc</code> attribue. It allow to define a static <code>ProbeDesc</code> in each
	 * <code>Probe</code> subclass.
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		Object o = null;
		try {
			o = super.clone();
			((ProbeDesc) o).cloned = true;
		} catch (CloneNotSupportedException e) {
			logger.error("Clone not suported for this object");
		}
		return o;
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

    public void setRequester(SnmpRequester requester) {
        this.requester = requester;
    }

    /**
	 * used to verify if <code>ProbeDesc</code> as been already cloned
	 * @return Returns the cloned.
	 */
	public boolean isCloned() {
		return cloned;
	}

    public SnmpRequester getRequester() {
        return requester;
    }
}
