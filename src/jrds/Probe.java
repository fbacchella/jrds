/*
 * Created on 22 nov. 2004
 */
package jrds;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A abstract class that needs to be derived for specific probe.<br>
 * the derived class must construct a <code>ProbeDesc</code> and
 * can overid some method as needed
 * @author Fabrice Bacchella
 */
public abstract class Probe
implements Comparable {

	static final private Logger logger = Logger.getLogger(Probe.class);

	private int timeout = HostsList.getRootGroup().getTimeout();
	private String name = null;
	private RdsHost monitoredHost;
	private Collection<RdsGraph> graphList = new ArrayList<RdsGraph>(0);
	private String stringValue = null;
	private ProbeDesc pd;
	private Set<String> tags = null;
	private final StartersSet starters = new StartersSet(this);
	long uptime = Long.MAX_VALUE;

	/**
	 * The constructor that should be called by derived class
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe(RdsHost monitoredHost, ProbeDesc pd) {
		this.monitoredHost = monitoredHost;
		this.pd = pd;
		starters.setParent(monitoredHost.getStarters());
	}

	public Probe() {
	}

	public void readProperties(Properties p) {

	}

	public RdsHost getHost() {
		return monitoredHost;
	}

	public void setHost(RdsHost monitoredHost) {
		this.monitoredHost = monitoredHost;
		starters.setParent(monitoredHost.getStarters());		
	}

	public void setPd(ProbeDesc pd) {
		this.pd = pd;
	}

	public void initGraphList(GraphFactory gf) {
		Collection graphClasses = pd.getGraphClasses();
		if(graphClasses != null) {
			graphList = new ArrayList<RdsGraph>(graphClasses.size());
			for (Object o:  graphClasses ) {
				RdsGraph newGraph = gf.makeGraph(o, this);
				if(newGraph != null)
					graphList.add(newGraph);
			}
		}
		else {
			logger.debug("No graph for probe" + this);
		}
	}

	/**
	 * @return Returns the graphList.
	 */
	public Collection<RdsGraph> getGraphList() {
		return graphList;
	}

	public String getName() {
		if (name == null)
			name = parseTemplate(getPd().getProbeName());
		return name;
	}

	public String getRrdName() {
		String rrdName = getName().replaceAll("/","_");
		return monitoredHost.getHostDir() +
		org.jrobin.core.Util.getFileSeparator() + rrdName + ".rrd";
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
		defaultArc[0] = new ArcDef("AVERAGE", 0.5, 1, 12 * 24 * 30 * 6);
		defaultArc[1] = new ArcDef("AVERAGE", 0.5, 12, 8760);
		defaultArc[2] = new ArcDef("AVERAGE", 0.5, 288, 730);
		return defaultArc;
	}

	public RrdDef getRrdDef() throws RrdException {
		RrdDef def = getDefaultRrdDef();
		def.addArchive(getArcDefs());
		def.addDatasource(getDsDefs());
		return def;
	}

	/**
	 * @throws RrdException
	 * @throws IOException
	 */
	private void create() throws RrdException, IOException {
		logger.info("Need to create rrd " + this);
		RrdDef def = getDefaultRrdDef();
		def.addArchive(getArcDefs());
		def.addDatasource(getDsDefs());
		final RrdDb rrdDb = new RrdDb(def);
		rrdDb.sync();
		rrdDb.close();
	}

	private void upgrade() {
		RrdDb rrdSource = null;
		try {
			logger.warn("Probe " + this + " definition is changed, the store needs to be upgraded");
			File source = new File(getRrdName());
			rrdSource = new RrdDb(source.getCanonicalPath());

			RrdDef rrdDef = getRrdDef();
			File dest = File.createTempFile("JRDS_", ".tmp", source.getParentFile());
			rrdDef.setPath(dest.getCanonicalPath());
			RrdDb rrdDest = new RrdDb(rrdDef);

			logger.debug("updating " +  source  + " to "  + dest);

			rrdSource.copyStateTo(rrdDest);
			rrdDest.close();
			rrdSource.close();
			logger.debug("Size difference : " + (dest.length() - source.length()));
			copyFile(dest.getCanonicalPath(), source.getCanonicalPath());
		} catch (RrdException e) {
			logger.error("Upgrade of " + this + " failed: " + e);
		} catch (IOException e) {
			logger.error("Upgrade of " + this + " failed: " + e);
		}
		finally {
			if(rrdSource != null)
				try {
					rrdSource.close();
				} catch (IOException e) {
				}

		}

	}

	private static void copyFile(String sourcePath, String destPath)
	throws IOException {
		File source = new File(sourcePath);
		File dest = new File(destPath);
		File destOld = new File(destPath + ".old");
		if (!dest.renameTo(destOld)) {
			throw new IOException("Could not rename file " + destPath + " from " + destOld);
		}
		if (!source.renameTo(dest)) {
			throw new IOException("Could not rename file " + destPath + " from " + sourcePath);
		}
		deleteFile(destOld);
	}

	private static void deleteFile(File file) throws IOException {
		if (file.exists() && !file.delete()) {
			throw new IOException("Could not delete file: " + file.getCanonicalPath());
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
		File rrdDir = new File(monitoredHost.getHostDir());
		if (!rrdDir.isDirectory()) {
			rrdDir.mkdir();
		}
		File rrdFile = new File(getRrdName());
		RrdDb rrdDb = null;
		try {
			if ( rrdFile.isFile()) {
				rrdDb = new RrdDb(getRrdName());
				//old definition
				RrdDef tmpdef = rrdDb.getRrdDef();
				Date startTime = new Date();
				tmpdef.setStartTime(startTime);
				String oldDef = tmpdef.dump();
				logger.trace("Definition found for " + this + ":\n" + oldDef);

				//new definition
				tmpdef = getRrdDef();
				tmpdef.setStartTime(startTime);
				String newDef = tmpdef.dump();

				if(! newDef.equals(oldDef)) {
					rrdDb.close();
					rrdDb = null;
					upgrade();
					rrdDb = new RrdDb(getRrdName());
				}
				logger.trace("******");
			} else
				create();
			retValue = true;
		} catch (Exception e) {
			if(logger.isDebugEnabled())
				logger.error("Store " + getRrdName() + " unusable: " + e,e);
			else
				logger.error("Store " + getRrdName() + " unusable: " + e);
		}
		finally {
			if(rrdDb != null)
				try {
					rrdDb.close();
				} catch (IOException e) {
				}

		}
		return retValue;
	}

	/**
	 * The method that return a map of data to be stored.<br>
	 * the key is resolved using the <code>ProbeDesc</code>. A key not associated with an existent datastore will generate a warning
	 * but will not prevent the other values to be stored.<br>
	 * the value should be a <code>java.lang.Number<code><br>
	 * @return the map of values
	 */
	public abstract Map getNewSampleValues();

	/**
	 * A method that might be overiden if specific treatement is needed
	 * by a probe.<br>
	 * By default, it does nothing.
	 * @param valuesList
	 * @return an map of value to be stored
	 */
	public Map<?, Number>  filterValues(Map valuesList) {
		return (Map<?, Number>)valuesList;
	}


	/**
	 * Store the values on the rrd backend.
	 * Overiding should be avoided.
	 * @param oneSample
	 */
	protected void updateSample(Sample oneSample) {
		if(isStarted()) {
			Map sampleVals = getNewSampleValues();
			if (sampleVals != null) {
				if(getUptime() >= ProbeDesc.HEARTBEATDEFAULT) {
					Map<?, String> nameMap = getPd().getCollectkeys();
					Map<?, Number >filteredSamples = filterValues(sampleVals);
					for(Map.Entry<?, Number> e: filteredSamples.entrySet()) {
						String dsName = nameMap.get(e.getKey());
						double value = e.getValue().doubleValue();
						if (dsName != null) {
							try {
								oneSample.setValue(dsName, value);
							}
							catch (RrdException ex) {
								logger.warn("Unable to update value " + value +
										" from " + this +": " +
										ex);
							}
						}
						else {
							logger.debug("Dropped entry: " + e.getKey() + " for " + this);
						}
					}
				}
				else {
					logger.info("uptime too low for " + toString());

				}
			}
		}

	}

	/**
	 * Launch an collect of values.
	 * You should not try to overide it
	 * @throws IOException
	 * @throws RrdException
	 */
	public void collect() {
		HostsList hl = HostsList.getRootGroup();
		//We only collect if the HostsList allow it
		if(hl.isStarted()) {
			logger.debug("launch collect for " + this);
			starters.startCollect();
			RrdDb rrdDb = null;
			Sample onesample;
			try {
				//No collect if the thread was interrupted
				if( ! Thread.currentThread().isInterrupted()) {
					rrdDb = StoreOpener.getRrd(getRrdName());
					onesample = rrdDb.createSample();
					updateSample(onesample);
					logger.trace(onesample.dump());
					//The collect might have been stopped
					//during the reading of samples
					//We also do not store if the thread was interrupted
					if(hl.isStarted() && ! Thread.currentThread().isInterrupted())
						onesample.update();
				}
			}
			catch (ArithmeticException ex) {
				logger.warn("Error while storing sample for probe " + this + ": " +
						ex.getMessage());
			} catch (Exception e) {
				if(logger.isDebugEnabled())
					logger.debug("Error with probe " + this + ": ", e);
				else
					logger.error("Error with probe " + this + ": " + e.getMessage());
			}
			finally  {
				if(rrdDb != null)
					StoreOpener.releaseRrd(rrdDb);
			}
			starters.stopCollect();
		}
	}

	/**
	 * Return the string value of the probe as a path constitued of
	 * the host name / the probe name
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		String hn = "<empty>";
		if(getHost() != null)
			hn = getHost().getName();
		stringValue = hn + "/" + getName();
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
		return String.CASE_INSENSITIVE_ORDER.compare(toString(),
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
			logger.error("Unable to get last update date for" + getName() + ": " + e.getMessage());
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

	/**
	 * Return the probe datas for the given period
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public FetchData fetchData(Date startDate, Date endDate) {
		FetchData retValue = null;
		RrdDb rrdDb = null;
		try {
			rrdDb = StoreOpener.getRrd(getRrdName());
			FetchRequest fr = rrdDb.createFetchRequest(GraphDesc.DEFAULTCF.toString(), startDate.getTime() /1000, endDate.getTime() / 1000);
			retValue = fr.fetchData();
		} catch (Exception e) {
			logger.error("Unable to fetch data for" + this.getName() + ": " + e.getMessage());
		}
		finally {
			if(rrdDb != null)
				StoreOpener.releaseRrd(rrdDb);
		}
		return retValue;
	}

	public Map<String, Number> getLastValues() {
		Map<String, Number> retValues = new HashMap<String, Number>();
		RrdDb rrdDb = null;
		try {
			rrdDb = StoreOpener.getRrd(getRrdName());
			String[] dsNames = rrdDb.getDsNames();
			for(int i = 0; i < dsNames.length ; i ++) {
				retValues.put(dsNames[i], rrdDb.getDatasource(i).getLastValue());
			}
		} catch (Exception e) {
			logger.error("Unable to get last values for" + getName() + ": " + e.getMessage());
		}
		finally {
			if(rrdDb != null)
				StoreOpener.releaseRrd(rrdDb);
		}
		return retValues;
	}
	/**
	 * Return a uniq name for the graph
	 * @return
	 */
	public String getQualifiedName() {
		return getHost().getName() + "/"  + getName();
	}

	public int hashCode() {
		return getQualifiedName().hashCode();
	}

	public void addTag(String tag) {
		if(tags == null)
			tags = new HashSet<String>();
		tags.add(tag);
	}

	public Set<String> getTags() {
		int tagsize = 0;
		if(tags != null)
			tagsize = tags.size();
		Set<String> ptags = getHost().getTags();
		Set<String> alltags = new HashSet<String>(ptags.size() + tagsize);
		if(ptags.size() > 0)
			alltags.addAll(ptags);
		if(tags != null)
			alltags.addAll(tags);
		return alltags;
	}

	public void addStarter(Starter s) {
		starters.registerStarter(s, this);
	}

	public StartersSet getStarters() {
		return starters;
	}

	public boolean isStarted() {
		return HostsList.getRootGroup().isStarted() && ! Thread.currentThread().isInterrupted();
	}

	public abstract String getSourceType();

	public String getSpecific() {
		return pd.getSpecific();
	}

	/**
	 * This function should return the uptime of the probe
	 * If it's not overriden, it will return Long.MAX_VALUE
	 * and it will because usell, as it used to make the probe pause 
	 * after a restart of the probe.
	 * It's called after filterValues
	 * @return the uptime in second
	 */
	public long getUptime() {
		return uptime;
	}

	public void setUptime(long uptime) {
		this.uptime = uptime;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Document dumpAsXml() throws ParserConfigurationException, IOException, RrdException {
		return dumpAsXml(false);
	}

	public Document dumpAsXml(boolean sorted) throws ParserConfigurationException, IOException, RrdException {
		String probeName = getPd().getName();
		String name = getName();
		String host = getHost().getName();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();  // Create from whole cloth
		Element root = 
			(Element) document.createElement("probe"); 
		document.appendChild(root);
		root.setAttribute("name", name);
		root.setAttribute("host", host);
		Element probeNameElement = document.createElement("probeName");
		probeNameElement.appendChild(document.createTextNode(probeName));

		root.appendChild(probeNameElement);
		if(this instanceof UrlProbe) {
			Element urlElement = document.createElement("url");
			String url = ((UrlProbe)this).getUrlAsString();
			urlElement.appendChild(document.createTextNode(url));
			root.appendChild(urlElement);
		}
		if(this instanceof IndexedProbe) {
			Element urlElement = document.createElement("index");
			String index = ((IndexedProbe)this).getIndexName();
			urlElement.appendChild(document.createTextNode(index));
			root.appendChild(urlElement);
		}
		Element dsElement = document.createElement("ds");
		root.appendChild(dsElement);
		DsDef[] dss= getDsDefs();
		HostsList hl = HostsList.getRootGroup();
		GraphFactory gf = new GraphFactory(false);
		if (sorted)
			Arrays.sort(dss, new Comparator<DsDef>() {
				public int compare(DsDef arg0, DsDef arg1) {
					return String.CASE_INSENSITIVE_ORDER.compare(arg0.getDsName(), arg1.getDsName());
				}
			});
		for(DsDef ds: dss) {
			String dsName = ds.getDsName();
			String id = getHost().getName() + "." + getName() + "." + dsName;
			String graphDescName = getName() + "." + dsName;

			GraphDesc gd = new GraphDesc();

			gd.setName(graphDescName);
			gd.setGraphName(id);
			gd.setGraphTitle(getName() + "." + dsName + " on {1}");
			gd.add(dsName, GraphDesc.LINE);
			gf.addGraphDesc(gd);
			RdsGraph g = gf.makeGraph(graphDescName, this);
			hl.addGraphs(Collections.singleton(g));

			Element dsNameElement = document.createElement("name");
			dsNameElement.setAttribute("id", "" + g.hashCode());
			dsNameElement.appendChild(document.createTextNode(dsName));
			dsElement.appendChild(dsNameElement);
		}
		return document;
	}
}
