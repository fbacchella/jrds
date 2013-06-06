package jrds;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.factories.ProbeMeta;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.starter.HostStarter;
import jrds.starter.StarterNode;
import jrds.store.Extractor;
import jrds.store.Store;
import jrds.store.StoreFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.rrd4j.core.DsDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A abstract class that needs to be derived for specific probe.<br>
 * the derived class must construct a <code>ProbeDesc</code> and
 * can overid some method as needed
 * @author Fabrice Bacchella
 */
@ProbeMeta(
        topStarter=jrds.starter.SocketFactory.class
        )
public abstract class Probe<KeyType, ValueType> extends StarterNode implements Comparable<Probe<KeyType, ValueType>>  {

    private class LocalJrdsSample extends HashMap<String, Number> implements JrdsSample {
        Date time;

        /**
         * @return the time
         */
        public LocalJrdsSample() {
            super(Probe.this.pd.getSize());
            time = new Date();
        }

        public Date getTime() {
            return time;
        }

        /**
         * @param time the time to set
         */
        public void setTime(Date time) {
            this.time = time;
        }

        public void put(Map.Entry<String, Double> e) {
            this.put(e.getKey(), e.getValue());
        }

        public Probe<KeyType,ValueType> getProbe() {
            return Probe.this;
        }
    }

    private String name = null;
    private HostInfo monitoredHost;
    private Collection<GraphNode> graphList = new ArrayList<GraphNode>();
    private ProbeDesc pd;
    private long uptime = Long.MAX_VALUE;
    private boolean finished = false;
    private String label = null;
    private Logger namedLogger = Logger.getLogger("jrds.Probe.EmptyProbe");
    private volatile boolean running = false;
    private Set<Store> stores = new HashSet<Store>();
    private Store mainStore;

    /**
     * A special case constructor, mainly used by virtual probe
     * @param monitoredHost
     * @param pd
     */
    public Probe(ProbeDesc pd) {
        super();
        setPd(pd);
    }

    /**
     * A special case constructor, mainly used by virtual probe
     * @param monitoredHost
     * @param pd
     */
    public Probe() {
        super();
    }

    public HostInfo getHost() {
        return monitoredHost;
    }

    public void setHost(HostStarter monitoredHost) {
        this.monitoredHost = monitoredHost.getHost();
        setParent(monitoredHost);
    }

    public void setPd(ProbeDesc pd) {
        this.pd = pd;
        namedLogger =  Logger.getLogger("jrds.Probe." + pd.getName());
        if( ! readSpecific()) {
            throw new RuntimeException("Creation failed");
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

    public void setName(String name) {
        this.name = name;
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
     * This method take two unsigned 32 integers and return a signed 64 bits long
     * The input value me be stored in a Long object
     * @param high high bits of the value
     * @param low low bits of the value
     * @return
     */
    private Long joinCounter32(ValueType high, ValueType low) {
        if(high instanceof Long && low instanceof Long) {
            long highnum = ((Number) high).longValue();
            long lownum = ((Number) low).longValue();
            return (highnum << 32) + lownum;
        }
        return null;
    }

    /**
     * The sample itself can be modified<br>
     * @param sample
     * @param values
     */
    public void modifySample(JrdsSample sample, Map<KeyType, ValueType> values) {
        for(Map.Entry<String, ProbeDesc.Joined> e: getPd().getHighlowcollectmap().entrySet()) {
            Long joined = joinCounter32(values.get(e.getValue().keyhigh), values.get(e.getValue().keylow));
            if(joined != null)
                sample.put(e.getKey(), joined.doubleValue());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<KeyType, String> getCollectMapping() {
        Map<KeyType, String> rawMap = (Map<KeyType, String>)getPd().getCollectMapping();
        Map<KeyType, String> retValues = new HashMap<KeyType, String>(rawMap.size());
        for(Map.Entry<KeyType, String> e: rawMap.entrySet()) {
            String value = jrds.Util.parseTemplate(e.getValue(), this);
            KeyType key = e.getKey();
            if(key instanceof String)
                key = (KeyType)jrds.Util.parseTemplate((String)key, this);
            retValues.put(key, value);
        }
        return retValues;
    }

    /**
     * Store the values on the rrd backend.
     * @param oneSample
     */
    private JrdsSample updateSample() {
        JrdsSample sample = newSample();
        if(isCollectRunning()) {
            Map<KeyType, ValueType> sampleVals = getNewSampleValues();
            log(Level.TRACE, "Collected values: %s", sampleVals);
            if (sampleVals != null && ! sampleVals.isEmpty()) {
                if(getUptime() * pd.getUptimefactor() >= pd.getHeartBeatDefault()) {
                    //Set the default values that might be defined in the probe description
                    for(Map.Entry<String, Double> e: getPd().getDefaultValues().entrySet()) {
                        sample.put(e);
                    }
                    Map<?, String> nameMap = getCollectMapping();
                    log(Level.TRACE, "Collect keys: %s", nameMap);
                    Map<KeyType, Number>filteredSamples = filterValues(sampleVals);
                    log(Level.TRACE, "Filtered values: %s", filteredSamples);
                    for(Map.Entry<KeyType, Number> e: filteredSamples.entrySet()) {
                        String dsName = nameMap.get(e.getKey());
                        if (dsName != null) {
                            sample.put(dsName, e.getValue());
                        }
                        else {
                            log(Level.TRACE, "Dropped entry: %s", e.getKey());
                        }
                    }
                    modifySample(sample, sampleVals);
                    return sample;
                }
                else {
                    log(Level.INFO, "uptime too low: %.0f", getUptime() * pd.getUptimefactor());
                }
            }
        }
        return sample;
    }

    /**
     * Return a new JrdsSample. It can be overridden if a smarter sample is needed
     * @return
     */
    public JrdsSample newSample() {
        return this.new LocalJrdsSample();
    }

    /**
     * Launch an collect of values.
     * You should not try to override it
     */
    public void collect() {
        long start = System.currentTimeMillis();
        boolean interrupted = true;
        if(! finished) {
            log(Level.ERROR, "Using an unfinished probe");
            return;
        }
        if(running) {
            log(Level.ERROR, "Hanged from a previous collect");
            return;
        }
        startCollect();
        //We only collect if the HostsList allow it
        if(isCollectRunning()) {
            running = true;
            log(Level.DEBUG,"launching collect");
            try {
                //No collect if the thread was interrupted
                if( isCollectRunning()) {
                    JrdsSample sample = updateSample();                    
                    //The collect might have been stopped
                    //during the reading of samples
                    if( sample.size() > 0 && isCollectRunning()) {
                        mainStore.commit(sample);
                        for(Store store: stores) {
                            store.commit(sample);
                        }
                        interrupted = false;
                    }
                }
            }
            catch (ArithmeticException ex) {
                log(Level.WARN, ex, "Error while storing sample: %s", ex.getMessage());
            }
            catch (Exception e) {
                Throwable rootCause = e;
                Throwable upCause;
                StringBuilder message = new StringBuilder();
                do {
                    String cause = rootCause.getMessage();
                    if(cause == null || "".equals(cause)) {
                        message.append(": ").append(rootCause.toString());
                    }
                    else {
                        message.append(": ").append(cause);
                    }
                    upCause = rootCause.getCause();
                    if(upCause != null)
                        rootCause = upCause;
                } while (upCause != null);
                log(Level.ERROR, e, "Error while collecting: %s", message);
            }
            finally  {
                stopCollect();
            }
            if(interrupted) {
                long end = System.currentTimeMillis();
                float elapsed = ((float)(end - start))/1000;
                log(Level.DEBUG, "Interrupted after %.2fs", elapsed);
            }
            running = false;
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
        return hn + "/" + getName();
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

    public boolean dsExist(String dsName) {
        return pd.dsExist(dsName);
    }

    /**
     * Return a unique name for the graph
     * @return
     */
    public String getQualifiedName() {
        return getHost().getName() + "/"  + getName();
    }

    public int hashCode() {
        return getQualifiedName().hashCode();
    }

    public Set<String> getTags() {
        return getHost().getTags();
    }

    public abstract String getSourceType();

    /**
     * This function it used by the probe to read all the specific it needs from the probe description
     * It's called once during the probe initialization
     * Every override should finish by:
     * return super();
     * @return
     */
    public boolean readSpecific() {
        return true;
    }

    /**
     * A probe can override it to extract custom values from the properties.
     * It will be read just after it's created and before configuration.
     * 
     * @param pm
     */
    public void readProperties(PropertiesManager pm) {

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
        log(Level.TRACE, "Setting probe uptime to: %d", uptime);
        this.uptime = uptime;
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
        Document document = builder.newDocument();
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

        Element graphs = (Element) root.appendChild(document.createElement("graphs"));
        for(GraphNode gn: this.graphList) {
            String qualifiedGraphName = gn.getQualifieName();
            Element graph = (Element) graphs.appendChild(document.createElement("graphname"));
            graph.setTextContent(qualifiedGraphName);
            graph.setAttribute("id", String.valueOf(gn.hashCode()));
        }
        DsDef[] dss= new DsDef[0]; //getDsDefs();

        if (sorted)
            Arrays.sort(dss, new Comparator<DsDef>() {
                public int compare(DsDef arg0, DsDef arg1) {
                    return String.CASE_INSENSITIVE_ORDER.compare(arg0.getDsName(), arg1.getDsName());
                }
            });

        for(DsDef ds: dss) {
            String dsName = ds.getDsName();

            Element dsNameElement = document.createElement("name");

            dsNameElement.setAttribute("pid", String.valueOf(hashCode()));
            dsNameElement.setAttribute("dsName", dsName);
            dsNameElement.appendChild(document.createTextNode(dsName));
            dsElement.appendChild(dsNameElement);
        }
        return document;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public Date getLastUpdate() {
        return mainStore.getLastUpdate();
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
        ProbeDesc pd = getPd();
        if(pd == null) {
            log(Level.ERROR, "Missing Probe description");
            return false;
        }
        if(getHost() == null) {
            log(Level.ERROR, "Missing host");
            return false;
        }

        //Name can be set by other means
        if(name == null)
            name = parseTemplate(pd.getProbeName());

        finished = mainStore.checkStoreFile();
        return finished;
    }

    private final String parseTemplate(String template) {
        Object[] arguments = {
                "${host}",
                "${index}",
                "${url}",
                "${port}",
                "${index.signature}",
                "${url.signature}"
        };
        return jrds.Util.parseOldTemplate(template, arguments, this);
    }


    /**
     * @return the stores
     */
    public Set<Store> getStores() {
        return stores;
    }

    /**
     * @param stores the stores to set
     */
    public void addStore(StoreFactory factory) {
        stores.add(factory.create(this));
    }

    /**
     * @return the mainStore
     */
    public Store getMainStore() {
        return mainStore;
    }

    /**
     * @param mainStore the mainStore to set
     * @throws InvocationTargetException 
     */
    public void setMainStore(StoreFactory factory, Map<String, String> args) throws InvocationTargetException {
        this.mainStore = factory.configure(this, args);
    }

    public Map<String, Number> getLastValues() {
        return mainStore.getLastValues();
    }

    public Extractor fetchData() {
        return mainStore.fetchData();
    }

}
