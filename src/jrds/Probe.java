/*
 * Created on 22 nov. 2004
 */
package jrds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.starter.Collecting;
import jrds.starter.Starter;
import jrds.starter.StarterNode;
import jrds.starter.StartersSet;

import org.apache.log4j.Level;
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
public abstract class Probe<KeyType, ValueType>
implements Comparable<Probe<KeyType, ValueType>>, StarterNode {

	private int timeout = 30;
	private long step = -1;
	private String name = null;
	private RdsHost monitoredHost;
	private Collection<GraphNode> graphList = new ArrayList<GraphNode>();
	private String stringValue = null;
	private ProbeDesc pd;
	private Set<String> tags = null;
	private final StartersSet starters = new StartersSet(this);
	private long uptime = Long.MAX_VALUE;
	private boolean finished = false;
	private String label = null;
	private Logger namedLogger = Logger.getLogger("jrds.Probe.EmptyProbe");

	//	private Map<String, Set<Threshold>> thresholds = new HashMap<String, Set<Threshold>>();

	/**
	 * The constructor that should be called by derived class
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe(RdsHost monitoredHost, ProbeDesc pd) {
		setPd(pd);
		setHost(monitoredHost);
		starters.setParent(monitoredHost.getStarters());
	}

	/**
	 * A special case constructor, mainly used by virtual probe
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe(ProbeDesc pd) {
		setPd(pd);
	}

	/**
	 * A special case constructor, mainly used by virtual probe
	 * @param monitoredHost
	 * @param pd
	 */
	public Probe() {
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
		if( ! readSpecific()) {
			throw new RuntimeException("Creation failed");
		}
		namedLogger =  Logger.getLogger("jrds.Probe." + pd.getName());
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

		Object[] arguments = {
				hn,
				index,
				url,
				port,
				jrds.Util.stringSignature(index),
				jrds.Util.stringSignature(url)
		};
		String evaluted = jrds.Util.parseTemplate(template, this);
		String formated;
		try {
			formated = MessageFormat.format(evaluted, arguments);
			return formated;
		} catch (IllegalArgumentException e) {
			log(Level.ERROR, "Template invalid: %s",template);
		}
		return evaluted;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected DsDef[] getDsDefs() {
		return getPd().getDsDefs();
	}

	protected ArcDef[] getArcDefs() {
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
		RrdDef def = new RrdDef(getRrdName());
		def.setVersion(2);
		def.addArchive(getArcDefs());
		def.addDatasource(getDsDefs());
		if(step > 0) {
			def.setStep(step);
		}
		return def;
	}

	/**
	 * @throws RrdException
	 * @throws IOException
	 */
	private void create() throws IOException {
		log(Level.INFO, "Need to create rrd");
		RrdDef def = getRrdDef();
		RrdDb rrdDb = new RrdDb(def);
		rrdDb.close();
	}

	private void upgrade() {
		RrdDb rrdSource = null;
		try {
			log(Level.WARN,"Definition is changed, the store needs to be upgraded");
			File source = new File(getRrdName());
			rrdSource = new RrdDb(source.getCanonicalPath());

			RrdDef rrdDef = getRrdDef();
			File dest = File.createTempFile("JRDS_", ".tmp", source.getParentFile());
			rrdDef.setPath(dest.getCanonicalPath());
			RrdDb rrdDest = new RrdDb(rrdDef);

			log(Level.DEBUG, "updating %s to %s",source, dest);

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
						log(Level.TRACE, "Update %s", dsName);
					} catch (RuntimeException e) {
						badDs.add(dsName);
						log(Level.ERROR, e, "Datasource %s can't be upgraded: %s", dsName,  e.getMessage());
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
									log(Level.TRACE, "Upgrade of %s from %s", dsName, srcArchive);
									srcArchive.getArcState(k).copyStateTo(dstArchive.getArcState(j));
									srcArchive.getRobin(k).copyStateTo(dstArchive.getRobin(j));
									robinMigrated++;
								}
							}
							catch (IllegalArgumentException e) {
								log(Level.TRACE, "Datastore %s removed", dsName);
							}

						}
						log(Level.TRACE, "Update %s", srcArchive);
					}
				}
			}
			log(Level.DEBUG, "Robin migrated: %s", robinMigrated);

			rrdDest.close();
			rrdSource.close();
			log(Level.DEBUG, "Size difference : %d", (dest.length() - source.length()));
			copyFile(dest.getCanonicalPath(), source.getCanonicalPath());
		} catch (IOException e) {
			log(Level.ERROR, e, "Upgrade failed: %s", e);
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
	 * Check the final status of the probe. It must be called once before an probe can be used
	 * 
	 * Open the rrd backend of the probe.
	 * it's created if it's needed
	 * @throws IOException
	 * @throws RrdException
	 */
	public boolean checkStore()  {
		if(pd == null) {
			log(Level.ERROR, "Missing Probe description");
			return false;
		}
		if(monitoredHost == null) {
			log(Level.ERROR, "Missing host");
			return false;
		}

		//Name can be set by other means
		if(name == null)
			name = parseTemplate(getPd().getProbeName());

		boolean retValue = false;
		File rrdDir = monitoredHost.getHostDir();
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
				long oldstep = tmpdef.getStep();
				log(Level.TRACE, "Definition found: %s\n", oldDef);

				//new definition
				tmpdef = getRrdDef();
				tmpdef.setStartTime(startTime);
				String newDef = tmpdef.dump();
				long newstep = tmpdef.getStep();

				if(newstep != oldstep ) {
					log(Level.ERROR, "step changed, you're in trouble" );
					return false;
				}
				else if(! newDef.equals(oldDef)) {

					rrdDb.close();
					rrdDb = null;
					upgrade();
					rrdDb = new RrdDb(getRrdName());
				}
				log(Level.TRACE, "******");
			} else
				create();
			retValue = true;
		} catch (Exception e) {
			log(Level.ERROR, e, "Store %s unusable: %s", getRrdName(), e);
		}
		finally {
			if(rrdDb != null)
				try {
					rrdDb.close();
				} catch (IOException e) {
				}

		}
		finished = retValue;
		return retValue;
	}

	/**
	 * The method that return a map of data collected.<br>
	 * It should return return as raw as possible, they can even be opaque data tied to the probe.
	 * the key is resolved using the <code>ProbeDesc</code>. A key not associated with an existent datastore will generate a warning
	 * but will not prevent the other values to be stored.<br>
	 * the value should be a <code>java.lang.Number<code><br>
	 * @return the map of collected object
	 */
	public abstract Map<KeyType, ValueType> getNewSampleValues();

	/**
	 * This method convert the collected object to numbers and can do post treatment
	 * @param valuesList
	 * @return an map of value to be stored
	 */
	@SuppressWarnings("unchecked")
	public Map<KeyType, Number>  filterValues(Map<KeyType, ValueType> valuesList) {
		return (Map<KeyType, Number>)valuesList;
	}

	/**
	 * The sample itself can be modified<br>
	 * The defautl function does nothing
	 * @param oneSample
	 * @param values
	 */
	public void modifySample(Sample oneSample, Map<KeyType, ValueType> values) {

	}

	@SuppressWarnings("unchecked")
	public Map<KeyType, String> getCollectMapping() {
		return (Map<KeyType, String>)getPd().getCollectMapping();
	}

	/**
	 * Store the values on the rrd backend.
	 * Overriding should be avoided.
	 * @param oneSample
	 */
	private void updateSample(Sample oneSample) {
		if(isCollectRunning()) {
			Map<KeyType, ValueType> sampleVals = getNewSampleValues();
			log(Level.TRACE, "Collected values: %s", sampleVals);
			if (sampleVals != null) {
				if(getUptime() * pd.getUptimefactor() >= pd.getHeartBeatDefault()) {
					Map<?, String> nameMap = getCollectMapping();
					log(Level.TRACE, "Collect keys: %s", nameMap);
					Map<KeyType, Number>filteredSamples = filterValues(sampleVals);
					log(Level.TRACE, "Filtered values: %s", filteredSamples);
					for(Map.Entry<KeyType, Number> e: filteredSamples.entrySet()) {
						String dsName = nameMap.get(e.getKey());
						double value = e.getValue().doubleValue();
						if (dsName != null) {
							oneSample.setValue(dsName, value);
						}
						else {
							log(Level.TRACE, "Dropped entry: %s", e.getKey());
						}
					}
					modifySample(oneSample, sampleVals);
				}
				else {
					log(Level.INFO, "uptime too low");
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
		boolean interrupted = true;
		if(! finished) {
			log(Level.ERROR, "Using an unfinished probe");
			return;
		}
		//We only collect if the HostsList allow it
		if(isCollectRunning()) {
			log(Level.DEBUG,"launching collect");
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
						if(namedLogger.isDebugEnabled())
							log(Level.DEBUG, "%s", onesample.dump());
						onesample.update();
						interrupted = false;
						//						checkThreshold(rrdDb);
					}
				}
			}
			catch (ArithmeticException ex) {
				log(Level.WARN, ex, "Error while storing sample: %s", ex.getMessage());
			} catch (Exception e) {
				log(Level.ERROR, e, "Error while collecting: %s", e.getMessage());
			}
			finally  {
				starters.stopCollect();
				if(rrdDb != null)
					StoreOpener.releaseRrd(rrdDb);
			}
			if(interrupted) 
				log(Level.INFO, "Interrupted");
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
	public int compareTo(Probe<KeyType, ValueType> arg0) {
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
			log(Level.ERROR, e, "Unable to get last update date: %s", e);
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
			log(Level.ERROR, e, "Unable to fetch data: %s", e.getMessage());
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
			log(Level.ERROR, e, "Unable to get last values: %s", e.getMessage());
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
		//Detected if a connected probe failed to start
		if(this instanceof ConnectedProbe) {
			ConnectedProbe cp = (ConnectedProbe) this;
			String cnxName = cp.getConnectionName();
			Starter cnx = getStarters().find(cnxName);
			if(cnx == null || ! cnx.isStarted())
				return false;
		}
		return getStarters().isStarted(Collecting.makeKey(this));
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
	 * If it's not overriden or fixed with setUptime, it will return Long.MAX_VALUE
	 * that's make it useless, as it used to make the probe pause 
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
		log(Level.TRACE, "Setting uptime to: %d", uptime);
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
		HostsList hl = getHostList();
		if (sorted)
			Arrays.sort(dss, new Comparator<DsDef>() {
				public int compare(DsDef arg0, DsDef arg1) {
					return String.CASE_INSENSITIVE_ORDER.compare(arg0.getDsName(), arg1.getDsName());
				}
			});
		Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
		for(DsDef ds: dss) {
			String dsName = ds.getDsName();
			String id = getHost().getName() + "." + getName() + "." + dsName;
			String graphDescName = getName() + "." + dsName;

			GraphDesc gd = new GraphDesc();
			gd.setName(graphDescName);
			gd.setGraphName(id);
			gd.setGraphTitle(getName() + "." + dsName + " on ${host}");
			gd.add(dsName, GraphDesc.LINE);
			gd.initializeLimits(g2d);

			GraphNode g = new GraphNode(this, gd);
			hl.addGraphs(Collections.singleton(g));

			Element dsNameElement = document.createElement("name");
			dsNameElement.setAttribute("id", "" + g.hashCode());
			dsNameElement.appendChild(document.createTextNode(dsName));
			dsElement.appendChild(dsNameElement);
		}
		return document;
	}

	//	public void addThreshold(Threshold t) {
	//		Set<Threshold> tset = thresholds.get(t.dsName);
	//		if(tset == null) {
	//			tset = new HashSet<Threshold>();
	//			thresholds.put(t.dsName, tset);
	//		}
	//		logger.trace("Threshold added: " + t.name);
	//		tset.add(t);
	//	}
	//
	//
	//	private void checkThreshold(RrdDb rrdDb) throws IOException {
	//	rrdDb = StoreOpener.getRrd(getRrdName());
	//		for(Set<Threshold> tset: thresholds.values()) {
	//			for(Threshold t: tset) {
	//				logger.trace("Threshold to " + this + ": " + t);
	//				if(t.check(rrdDb)) 
	//					t.run(this);
	//			}
	//		}
	//	}

	/**
	 * @return the time step (in seconds)
	 */
	public long getStep() {
		return step;
	}

	/**
	 * @param step the time step to set (in seconds)
	 */
	public void setStep(long step) {
		this.step = step;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public HostsList getHostList() {
		return (HostsList) getStarters().find(HostsList.class);
	}

	public void log(Level l, Throwable e, String format, Object... elements) {
		jrds.Util.log(this, namedLogger, l, e, format, elements);
	}

	public void log(Level l, String format, Object... elements) {
		jrds.Util.log(this, namedLogger,l, null, format, elements);
	}

	/**
	 * @return the namedLogger
	 */
	public Logger getNamedLogger() {
		return namedLogger;
	}

}
