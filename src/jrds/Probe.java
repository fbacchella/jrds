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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.factories.GraphFactory;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.Header;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A abstract class that needs to be derived for specific probe.<br>
 * the derived class must construct a <code>ProbeDesc</code> and
 * can overid some method as needed
 * @author Fabrice Bacchella
 */
public abstract class Probe
implements Comparable<Probe>, StarterNode {

	static final private Logger logger = Logger.getLogger(Probe.class);

	private int timeout = HostsList.getRootGroup().getTimeout();
	private String name = null;
	private RdsHost monitoredHost;
	private Collection<GraphNode> graphList = new ArrayList<GraphNode>(0);
	private String stringValue = null;
	private ProbeDesc pd;
	private Set<String> tags = null;
	private final StartersSet starters = new StartersSet(this);
	long uptime = Long.MAX_VALUE;

	private Map<String, Set<Threshold>> thresholds = new HashMap<String, Set<Threshold>>();

	/**
	 * The constructor that should be called by derived class
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe(RdsHost monitoredHost, ProbeDesc pd) {
		this.pd = pd;
		setHost(monitoredHost);
		starters.setParent(monitoredHost.getStarters());
		if( ! readSpecific()) {
			throw new RuntimeException("Creation failed");
		}
	}

	public Probe() {
	}

	public RdsHost getHost() {
		return monitoredHost;
	}

	public void setHost(RdsHost monitoredHost) {
		this.monitoredHost = monitoredHost;
		starters.setParent(monitoredHost.getStarters());
		//Name can be set by other means
		if(name == null)
			name = parseTemplate(getPd().getProbeName());
	}

	public void setPd(ProbeDesc pd) {
		this.pd = pd;
		if( ! readSpecific()) {
			throw new RuntimeException("Creation failed");
		}
	}

	public void initGraphList(GraphFactory gf) {
		Collection<?> graphClasses = pd.getGraphClasses();
		if(graphClasses != null) {
			graphList = new ArrayList<GraphNode>(graphClasses.size());
			for (Object o:  graphClasses ) {
				GraphNode newGraph = gf.makeGraph(o, this);
				if(newGraph != null)
					graphList.add(newGraph);
			}
		}
		else {
			logger.debug("No graph for probe" + this);
		}
	}

	public void addGraph(GraphDesc gd) {
		graphList.add(new GraphNode(this, gd));
	}

	public void addGraph(GraphNode node) {
		graphList.add(node);
	}

	/**
	 * @return Returns the graphList.
	 */
	public Collection<GraphNode> getGraphList() {
		return graphList;
	}

	public String getName() {
		return name;
	}

	public String getRrdName() {
		String rrdName = getName().replaceAll("/","_");
		return monitoredHost.getHostDir() +
		Util.getFileSeparator() + rrdName + ".rrd";
	}

	private final String parseTemplate(String template) {
		String index = "";
		String url = "";
		int port = 0;
		if( this instanceof IndexedProbe) {
			index =((IndexedProbe) this).getIndexName();
		}
		if( this instanceof UrlProbe) {
			url =((UrlProbe) this).getUrlAsString();
			port = ((UrlProbe) this).getPort();
		}
		String hn = "<empty>";
		if(getHost() != null)
			hn = getHost().getName();
		/*Map<String, Object> env = new LinkedHashMap<String, Object>();
		env.put("host", hn);
		env.put("index", index);
		env.put("url", url);
		env.put("port", port);
		env.put("index.signature", jrds.Util.stringSignature(index));
		env.put("url.signature", jrds.Util.stringSignature(url));
				Object[] arguments = env.values().toArray();

		 */
		Object[] arguments = {
				hn,
				index,
				url,
				port,
				jrds.Util.stringSignature(index),
				jrds.Util.stringSignature(url)
		};
		String evaluted = jrds.Util.parseTemplate(template, this);
		return MessageFormat.format(evaluted, arguments) ;

	}

	public void setName(String name) {
		this.name = name;
	}

	protected DsDef[] getDsDefs() {
		return getPd().getDsDefs();
	}

	protected RrdDef getDefaultRrdDef() {
		RrdDef def = new RrdDef(getRrdName());
		def.setVersion(2);
		return def;

	}

	private ArcDef[] getArcDefs() {
		ArcDef[] defaultArc = new ArcDef[3];
		//Five minutes step
		defaultArc[0] = new ArcDef(ConsolFun.AVERAGE, 0.5, 1, 12 * 24 * 30 * 3);
		//One hour step
		defaultArc[1] = new ArcDef(ConsolFun.AVERAGE, 0.5, 12, 24 * 365);
		//One day step
		defaultArc[2] = new ArcDef(ConsolFun.AVERAGE, 0.5, 288, 365 * 2);
		return defaultArc;
	}

	public RrdDef getRrdDef() {
		RrdDef def = getDefaultRrdDef();
		def.addArchive(getArcDefs());
		def.addDatasource(getDsDefs());
		return def;
	}

	/**
	 * @throws RrdException
	 * @throws IOException
	 */
	private void create() throws IOException {
		logger.info("Need to create rrd " + this);
		RrdDef def = getDefaultRrdDef();
		def.addArchive(getArcDefs());
		def.addDatasource(getDsDefs());
		RrdDb rrdDb = new RrdDb(def);
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

			Set<String> badDs = new HashSet<String>();
			Header header = rrdSource.getHeader();
			int dsCount = header.getDsCount();;
			header.copyStateTo(rrdDest.getHeader());
			for (int i = 0; i < dsCount; i++) {
				Datasource srcDs = rrdSource.getDatasource(i);
				String dsName = srcDs.getDsName();
				Datasource dstDS = rrdDest.getDatasource(dsName);
				if (dstDS != null ) {
					try {
						srcDs.copyStateTo(dstDS);
						logger.trace("Update " + dsName + " on " + this);
					} catch (RuntimeException e) {
						badDs.add(dsName);
						logger.error("Datasource " + dsName +" can't be upgraded: " + e.getMessage());
					}
				}
			}
			int robinMigrated = 0;
			for (int i = 0; i < rrdSource.getArcCount(); i++) {
				Archive srcArchive = rrdSource.getArchive(i);
				ConsolFun consolFun = srcArchive.getConsolFun();
				int steps = srcArchive.getSteps();
				Archive dstArchive = rrdDest.getArchive(consolFun, steps);
				if (dstArchive != null) {
					if ( dstArchive.getConsolFun().equals(srcArchive.getConsolFun())  &&
							dstArchive.getSteps() == srcArchive.getSteps() ) {
						for (int k = 0; k < dsCount; k++) {
							Datasource srcDs = rrdSource.getDatasource(k);
							String dsName = srcDs.getDsName();
							try {
								int j = rrdDest.getDsIndex(dsName);
								if (j >= 0 && ! badDs.contains(dsName)) {
									logger.trace("Upgrade of " + dsName + " from " + srcArchive);
									srcArchive.getArcState(k).copyStateTo(dstArchive.getArcState(j));
									srcArchive.getRobin(k).copyStateTo(dstArchive.getRobin(j));
									robinMigrated++;
								}
							}
							catch (IllegalArgumentException e) {
								logger.trace("Datastore " + dsName + " removed for " + this);
							}

						}
						logger.trace("Update " + srcArchive + " on " + this);
					}
				}
			}
			logger.debug("Robin migrated: " + robinMigrated);

			rrdDest.close();
			rrdSource.close();
			logger.debug("Size difference : " + (dest.length() - source.length()));
			copyFile(dest.getCanonicalPath(), source.getCanonicalPath());
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
			if ( rrdFile.isFile() ) {
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
	public abstract Map<?, ?> getNewSampleValues();

	/**
	 * A method that might be overriden if specific treatement is needed
	 * by a probe.<br>
	 * By default, it does nothing.
	 * @param valuesList
	 * @return an map of value to be stored
	 */
	@SuppressWarnings("unchecked")
	public Map<?, Number>  filterValues(Map<?, ?> valuesList) {
		return (Map<?, Number>)valuesList;
	}

	public Map<?, String> getCollectkeys() {
		return getPd().getCollectkeys();
	}

	/**
	 * Store the values on the rrd backend.
	 * Overriding should be avoided.
	 * @param oneSample
	 */
	protected void updateSample(Sample oneSample) {
		if(isCollectRunning()) {
			Map<?, ?> sampleVals = getNewSampleValues();
			if (sampleVals != null) {
				if(getUptime() * pd.getUptimefactor() >= ProbeDesc.HEARTBEATDEFAULT) {
					Map<?, String> nameMap = getCollectkeys();
					Map<?, Number>filteredSamples = filterValues(sampleVals);
					for(Map.Entry<?, Number> e: filteredSamples.entrySet()) {
						String dsName = nameMap.get(e.getKey());
						//A collect key may be null or empty to prevent collect, use the original name in this case
						if(dsName == null || "".equals(dsName))
							dsName = e.getKey().toString();
						double value = e.getValue().doubleValue();
						if (dsName != null) {
							oneSample.setValue(dsName, value);
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
	 * You should not try to override it
	 * @throws IOException
	 * @throws RrdException
	 */
	public void collect() {
		//We only collect if the HostsList allow it
		if(monitoredHost.isCollectRunning()) {
			logger.debug("launch collect for " + this);
			starters.startCollect();
			RrdDb rrdDb = null;
			try {
				//No collect if the thread was interrupted
				if( isCollectRunning()) {
					rrdDb = StoreOpener.getRrd(getRrdName());
					Sample onesample = rrdDb.createSample();
					updateSample(onesample);
					//The collect might have been stopped
					//during the reading of samples
					if( isCollectRunning()) {
						logger.trace(onesample.dump());
						onesample.update();
						checkThreshold(rrdDb);
					}
				}
			}
			catch (ArithmeticException ex) {
				logger.warn("Error while storing sample for probe " + this + ": " +
						ex.getMessage());
			} catch (Exception e) {
				if(logger.isDebugEnabled())
					logger.debug("Error with probe collect " + this + ": ", e);
				else
					logger.error("Error with probe collect " + this + ": " + e.getMessage());
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
	public String toString() {
		String hn = "<empty>";
		if(getHost() != null)
			hn = getHost().getName();
		stringValue = hn + "/" + getName();
		return stringValue;
	}

	/**
	 * The comparaison order of two object of the class is a case insensitive
	 * comparaison of it's string value.
	 *
	 * @param arg0 Object
	 * @return int
	 */
	public int compareTo(Probe arg0) {
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
			FetchRequest fr = rrdDb.createFetchRequest(GraphDesc.DEFAULTCF, startDate.getTime() /1000, endDate.getTime() / 1000);
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

	public StartersSet getStarters() {
		return starters;
	}

	public boolean isCollectRunning() {
		return getHost().isCollectRunning();
	}

	public abstract String getSourceType();

	/**
	 * This function reads all the specified arguments
	 * Every override should finish by:
	 * return super();
	 * @return
	 */
	public boolean readSpecific() {
		return true;
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

	/**
	 * Define the uptime of the probe
	 * @param uptime in seconds
	 */
	public void setUptime(long uptime) {
		this.uptime = uptime;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Document dumpAsXml() throws ParserConfigurationException, IOException {
		return dumpAsXml(false);
	}

	public Document dumpAsXml(boolean sorted) throws ParserConfigurationException, IOException {
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
			GraphNode g = gf.makeGraph(graphDescName, this);
			hl.addGraphs(Collections.singleton(g));

			Element dsNameElement = document.createElement("name");
			dsNameElement.setAttribute("id", "" + g.hashCode());
			dsNameElement.appendChild(document.createTextNode(dsName));
			dsElement.appendChild(dsNameElement);
		}
		return document;
	}

	public void addThreshold(Threshold t) {
		Set<Threshold> tset = thresholds.get(t.dsName);
		if(tset == null) {
			tset = new HashSet<Threshold>();
			thresholds.put(t.dsName, tset);
		}
		logger.trace("Threshold added: " + t.name);
		tset.add(t);
	}


	private void checkThreshold(RrdDb rrdDb) throws IOException {
		//No thresholds, nothing to do
		if(thresholds.size() == 0)
			return;

		String[] dsNames = rrdDb.getDsNames();
		long lastUpdate = Util.getDate(rrdDb.getLastUpdateTime()).getTime();

		for(int i=0; i< dsNames.length; i++) {
			Set<Threshold> tset = thresholds.get(dsNames[i]);
			if(tset == null)
				continue;
			double value = rrdDb.getDatasource(i).getLastValue();
			if(Double.isNaN(value))
				continue;
			for(Threshold t: tset) {
				if(t != null &&  t.check(value, lastUpdate)) {
					t.run(this);
				}
			}
		}
	}

}
