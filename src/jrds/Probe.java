/*
 * Created on 22 nov. 2004
 */
package jrds;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;

/**
 * A abstract class that needs to be derived for specific probe.<br>
 * the derived class must construct a <code>ProbeDesc</code> and
 * can overid some method as needed
 * @author Fabrice Bacchella
 */
public abstract class Probe
implements Comparable {
	
	static final private Logger logger = Logger.getLogger(Probe.class);
	
	private String name;
	private RdsHost monitoredHost;
	private Collection graphList;
	private final Object lock = new Object();
	private String stringValue = null;
	private ProbeDesc pd;
	
	/**
	 * The constructor that should be called by derived class
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe(RdsHost monitoredHost, ProbeDesc pd) {
		name = null;
		RrdDb rrdDb = null;
		this.monitoredHost = monitoredHost;
		this.pd = pd;
	}
	
	public RdsHost getHost() {
		return monitoredHost;
	}
	
	private Collection initGraphList() {
		Collection graphClasses = pd.getGraphClasses();
		Collection graphList = null;
		if(graphClasses != null) {
			graphList = new ArrayList(graphClasses.size());
			Class[] thisClass = new Class[] {
					Probe.class};
			Object[] args = new Object[] {
					this};
			for (Iterator i = graphClasses.iterator(); i.hasNext(); ) {
				Object o = i.next();
				RdsGraph newGraph = GraphFactory.makeGraph(o, this);
				if(newGraph != null)
					graphList.add(newGraph);
			}
		}
		else {
			logger.debug("No graph for probe" + this);
		}
		return graphList;
	}
	
	/**
	 * @return Returns the graphList.
	 */
	public Collection getGraphList() {
		if (graphList == null)
			graphList = initGraphList();
		return graphList;
	}
	
	public String getName() {
		if (name == null)
			name = parseTemplate(getPd().getName());
		return name;
	}
	
	public String getRrdName() {
		return monitoredHost.getHostDir() +
		PropertiesManager.getInstance().fileSeparator + getName() + ".rrd";
	}
	
	private final String parseTemplate(String template) {
		String index = "";
		String url = "";
		if( this instanceof IndexedProbe) {
			index =((IndexedProbe) this).getIndexName();
		}
		if( this instanceof UrlProbe) {
			url =((UrlProbe) this).getUrlAsString();
		}
		Object[] arguments = {
				getHost().getName(),
				index,
				url,
		};
		return MessageFormat.format(template, arguments) ;
		
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	protected DsDef[] getDsDefs() throws RrdException {
		return getPd().getDsDefs();
	}
	
	protected RrdDef getDefaultRrdDef() throws RrdException {
		RrdDef def = new RrdDef(getRrdName());
		return def;
		
	}
	
	private ArcDef[] getArcDefs() throws RrdException {
		ArcDef[] defaultArc = new ArcDef[3];
		defaultArc[0] = new ArcDef("AVERAGE", 0.5, 1, 105120);
		defaultArc[1] = new ArcDef("AVERAGE", 0.5, 12, 8760);
		defaultArc[2] = new ArcDef("AVERAGE", 0.5, 288, 730);
		return defaultArc;
	}
	
	/**
	 * @throws RrdException
	 * @throws IOException
	 */
	private void create() throws RrdException, IOException {
		synchronized (lock) {
			logger.info("Need to create rrd " + this);
			RrdDef def = getDefaultRrdDef();
			def.addArchive(getArcDefs());
			def.addDatasource(getDsDefs());
			final RrdDb rrdDb = new RrdDb(def);
			rrdDb.sync();
			rrdDb.close();
		}
	}
	
	/**
	 * Open the rrd backend of the probe.
	 * it's created if it's needed
	 * @throws IOException
	 * @throws RrdException
	 */
	public boolean checkStore()  {
		boolean retValue = false;
		synchronized (lock) {
			File rrdDir = new File(monitoredHost.getHostDir());
			if (!rrdDir.isDirectory()) {
				rrdDir.mkdir();
			}
			File rrdFile = new File(getRrdName());
			RrdDb rrdDb = null;
			try {
				if ( rrdFile.isFile()) {
					 rrdDb = StoreOpener.getRrd(getRrdName());
				} else
					create();
				retValue = true;
			} catch (Exception e) {
				logger.error("Store " + getRrdName() + " unusable: " + e);
			}
			finally {
				if(rrdDb != null)
					StoreOpener.releaseRrd(rrdDb);				
			}
		}
		return retValue;
	}
	
	/**
	 * The method that return a map of data to be stored.<br>
	 * the key is resolved using the <code>ProbeDesc</code>. A key not associated with an existent datastore will generate a warning
	 * but will not prevent the other values to be stored.<br>
	 * the value must be a <code>java.lang.Number<code><br>
	 * @return the map of values
	 */
	public abstract Map getNewSampleValues();
	
	/**
	 * A method that might be overiden if specif treatement is needed
	 * by a probe.<br>
	 * By default, it does nothing.
	 * @param valuesList
	 * @return an map of value to be stored
	 */
	public Map filterValues(Map valuesList) {
		return valuesList;
	}
	
	/**
	 * A method to filter by uptime value. Used to avoid nonense values when a counter is reset to zero.
	 * The uptime value must be exprimed in seconds
	 * @param id The id of value
	 * @param retValue
	 * @return the values unmodified or an empty map if uptime is to low
	 */
	final public Map filterUpTime(Object id, Map retValue) {
		if(retValue != null) {
			Number uptime = (Number) retValue.get(id);
			if(uptime != null && uptime.intValue() <= ProbeDesc.HEARTBEATDEFAULT) {
				retValue = new HashMap(0);
				logger.info("uptime too low for " + this.toString());
			}
			else {
				retValue.remove(id);
			}
		}
		return retValue;
	}
	
	/**
	 * Store the values on the rrd backend.
	 * Overiding should be avoided.
	 * @param oneSample
	 */
	protected void updateSample(Sample oneSample) {
		Map sampleVals = getNewSampleValues();
		Map nameMap = getPd().getDsNameMap();
		if (sampleVals != null && this.getHost().getUptime() > ProbeDesc.HEARTBEATDEFAULT * 1000) {
			sampleVals = filterValues(sampleVals);
			for (Iterator i = sampleVals.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry) i.next();
				String dsName = (String) nameMap.get(e.getKey());
				double value = ( (Number) e.getValue()).doubleValue();
				if (dsName != null) {
					try {
						oneSample.setValue(dsName, value);
					}
					catch (RrdException e1) {
						logger.warn("Unable to update value " + value +
								" from " + this +": " +
								e1.getLocalizedMessage());
					}
				}
			}
		}
		
	}
	
	/**
	 * Launch an collect of values.
	 * It cannot be overiden
	 * @throws IOException
	 * @throws RrdException
	 */
	public final void collect() {
		logger.debug("launch collect for " + this);
		RrdDb rrdDb = null;
		Sample onesample;
		try {
			rrdDb = StoreOpener.getRrd(getRrdName());
			onesample = rrdDb.createSample();
			updateSample(onesample);
			logger.debug(onesample.dump());
			onesample.update();
		}
		catch (ArithmeticException ex) {
			logger.warn("Error while storing sample: " +
					ex.getLocalizedMessage());
		} catch (Exception e) {
			logger.error("Error with probe " + this + ": " + e);
		}
		finally  {
			if(rrdDb != null)
				StoreOpener.releaseRrd(rrdDb);
		}
	}

	/**
	 * Return the string value of the probe as a path constitued of
	 * the host name / the probe name
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		if (stringValue == null) {
			stringValue = getHost().getName() + "/" + getName();
		}
		return stringValue;
	}
	
	/**
	 * The comparaison order of two object of ths class is a case insensitive
	 * comparaison of it's string value.
	 *
	 * @param arg0 Object
	 * @return int
	 */
	public final int compareTo(Object arg0) {
		return String.CASE_INSENSITIVE_ORDER.compare(this.toString(),
				arg0.toString());
	}
	
	/**
	 * @return Returns the <code>ProbeDesc</code> of the probe.
	 */
	public ProbeDesc getPd() {
		return pd;
	}

	/**
	 * Return the date of the last update of the rrd backend
	 * @return The date
	 */
	public Date getLastUpdate() {
		Date lastUpdate = null;
		RrdDb rrdDb = null;
		try {
			rrdDb = StoreOpener.getRrd(getRrdName());
			lastUpdate = Util.getDate(rrdDb.getLastUpdateTime());
		} catch (Exception e) {
			logger.error("Unable to get last update date for" + this.getName() + ":" + e);
		}
		finally {
			if(rrdDb != null)
				StoreOpener.releaseRrd(rrdDb);
		}
		return lastUpdate;
	}

	final public boolean dsExist(String dsName) {
		boolean retValue = false;
		RrdDb rrddb = null;
		try {
			rrddb = StoreOpener.getRrd(getRrdName());
			retValue = rrddb.getDatasource(dsName) != null;
		} catch (Exception e) {
		}
		finally {
			if(rrddb != null)
				StoreOpener.releaseRrd(rrddb);
		}
		return retValue;
	}
}
