package jrds;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jrds.factories.ArgFactory;
import jrds.factories.ProbeMeta;
import jrds.starter.StarterNode;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * The description of a probe that must be used for each probe.
 * It's purpose is to make the description of a probe easier to read or write.
 * @author Fabrice Bacchella
 * @version $Revision$
 */
/**
 * @author Fabrice Bacchella
 *
 */
public class ProbeDesc<KeyType> implements Cloneable {
    static final private Logger logger = Logger.getLogger(ProbeDesc.class);

    public static class DefaultBean {
        public final String value;
        public final boolean delayed;

        DefaultBean(String value, boolean delayed) {
            this.value = value;
            this.delayed = delayed;
        }
    }

    public static class Joined {
        final Object keyhigh;
        final Object keylow;
        Joined(Object keyhigh, Object keylow) {
            this.keyhigh = keyhigh;
            this.keylow = keylow;
        }
    }

    private static class DsDesc<KeyType> {
        final DsType dsType;
        final double minValue;
        final double maxValue;
        final KeyType collectKey;

        private DsDesc(DsType dsType, double minValue, double maxValue, KeyType collectKey) {
            this.dsType = dsType;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.collectKey = collectKey;
        }

        @Override
        public String toString() {
            return "DsDesc [dsType=" + dsType
                            + ", minValue=" + minValue + ", maxValue=" + maxValue
                            + ", collectKey=" + collectKey + "]";
        }
    }

    @ToString @EqualsAndHashCode
    public static class DataSourceBuilder {
        @Setter @Accessors(chain=true)
        private String name;
        @Setter @Accessors(chain=true)
        private DsType dsType;
        @Setter @Accessors(chain=true)
        private Double defaultValue = null;
        @Setter @Accessors(chain=true)
        private double minValue = MINDEFAULT;
        @Setter @Accessors(chain=true)
        private double maxValue = MAXDEFAULT;
        @Setter @Accessors(chain=true)
        private String collectKey;
        @Setter @Accessors(chain=true)
        private String collectKeyHigh;
        @Setter @Accessors(chain=true)
        private String collectKeyLow;
        @Setter @Accessors(chain=true)
        private boolean optionnal;

        private DataSourceBuilder(String name, DsType dsType) {
            this.name = name;
            this.dsType = dsType;
        }

        private DataSourceBuilder() {
        }
    }

    public static DataSourceBuilder getDataSourceBuilder(String name, DsType dsType) {
        return new DataSourceBuilder(name, dsType);
    }

    public static DataSourceBuilder getDataSourceBuilder() {
        return new DataSourceBuilder();
    }

    static public final double MINDEFAULT = 0;
    static public final double MAXDEFAULT = Double.NaN;

    private Map<String, DsDesc<KeyType>> dsMap;
    private Map<String, String> specific = new HashMap<String, String>();
    private String probeName;
    private String name;
    private final Collection<String> graphesList = new ArrayList<String>();
    private Class<? extends Probe<KeyType, ?>> probeClass = null;
    private Map<String, ProbeDesc.DefaultBean> defaultsBeans = Collections.emptyMap();
    private float uptimefactor = (float) 1.0;
    private Map<String, Double> defaultValues = new HashMap<String, Double>(0);
    private Map<String, GenericBean> beans = new HashMap<String, GenericBean>(0);
    private final Set<String> optionals = new HashSet<String>(0);
    private final Map<String, Joined> highlowcollectmap = new HashMap<String, Joined>();
    private Map<KeyType, String> collectMap = null;
    @SuppressWarnings("unchecked")
    private CollectResolver<KeyType> collectResolver = (CollectResolver<KeyType>) new CollectResolver.StringResolver();

    /**
     * Create a new Probe Description, with <em>size</em> elements in prevision
     * 
     * @param size estimated elements number
     */
    public ProbeDesc(int size) {
        dsMap = new LinkedHashMap<String, DsDesc<KeyType>>(size);
    }

    /**
     * Create a new Probe Description
     */
    public ProbeDesc() {
        dsMap = new LinkedHashMap<String, DsDesc<KeyType>>();
    }

    /**
     * A datastore that is stored but not collected
     * 
     * @param name the datastore name
     * @param dsType
     */
    public void add(String name, DsType dsType) {
        dsMap.put(name, new DsDesc<KeyType>(dsType, MINDEFAULT, MAXDEFAULT, collectResolver.resolve(name)));
    }

    public void add(DataSourceBuilder builder) {
        KeyType collectKey = null;
        String collectKeyName = null;
        String bname = null;

        // Where to look for the added name
        if(builder.name != null) {
            bname = builder.name;
        } else if(builder.collectKey != null) {
            bname = builder.collectKey;
        }

        // Where to look for the collect info
        if (builder.collectKeyHigh != null && builder.collectKeyLow != null) {
            try {
                dsMap.put(bname + "high", new DsDesc<KeyType>(null, builder.minValue, builder.maxValue, collectResolver.resolve(builder.collectKeyHigh)));
                dsMap.put(bname + "low", new DsDesc<KeyType>(null, builder.minValue, builder.maxValue, collectResolver.resolve(builder.collectKeyLow)));
                highlowcollectmap.put(bname, new Joined(builder.collectKeyHigh, builder.collectKeyLow));
            } catch (IllegalArgumentException e) {
                logger.error(String.format("Probe description %s: unable to parse collect key '%s': %s", name, collectKeyName, e.getMessage()));
                logger.debug(e);
            }
        } else if (builder.collectKey != null) {
            //If collect was given an empty value, it was to prevent collect
            if (! builder.collectKey.isEmpty()) {
                collectKeyName = builder.collectKey;
            }
        } else {
            collectKeyName = bname;
        }

        if (collectKeyName != null) {
            try {
                collectKey = collectResolver.resolve(collectKeyName);
            } catch (IllegalArgumentException e) {
                logger.error(String.format("Probe description %s: unable to parse collect key '%s': %s", name, collectKeyName, e.getMessage()));
                logger.debug(e);
            }
        }

        if(builder.defaultValue != null && builder.defaultValue != Double.NaN) {
            defaultValues.put(bname, builder.defaultValue);
        }

        if (builder.optionnal && collectKeyName != null) {
            optionals.add(collectKeyName);
        }

        dsMap.put(bname, new DsDesc<KeyType>(builder.dsType, builder.minValue, builder.maxValue, collectKey));
    }

    /**
     * @return the highlowcollectmap
     */
    public Map<String, Joined> getHighlowcollectmap() {
        return highlowcollectmap;
    }

    /**
     * Replace all the data source for this probe description with the list
     * provided
     * 
     * @param dsList a list of data source description as a map.
     */
    public void replaceDs(List<DataSourceBuilder> dsList) {
        defaultValues = new HashMap<String, Double>(0);
        dsMap = new HashMap<>(dsList.size());
        collectMap = null;
        dsList.forEach(this::add);
    }

    /**
     * Return a map that translate the probe technical name to the datastore
     * name
     * 
     * @return a Map of collect names to datastore name
     */
    public synchronized Map<KeyType, String> getCollectMapping() {
        if (collectMap == null) {
            collectMap = new LinkedHashMap<>(dsMap.size());
            for(Map.Entry<String, DsDesc<KeyType>> e: dsMap.entrySet()) {
                DsDesc<KeyType> dd = e.getValue();
                if(dd.collectKey != null) {
                    collectMap.put(dd.collectKey, e.getKey());
                }
            }
            collectMap = Collections.unmodifiableMap(collectMap);
        }
        return collectMap;
    }

    /**
     * Return a map that translate the probe technical name as a string to the
     * datastore name
     *
     * @return a Map of collect names to datastore name
     * @deprecated Replaced by {@link #getCollectMapping()}.
     */
    @Deprecated
    public Map<String, String> getCollectStrings() {
        Map<String, String> retValue = new LinkedHashMap<String, String>(dsMap.size());
        for (Map.Entry<String, DsDesc<KeyType>> e : dsMap.entrySet()) {
            DsDesc<KeyType> dd = e.getValue();
            if (dd.collectKey instanceof String && !"".equals(dd.collectKey))
                retValue.put((String) dd.collectKey, e.getKey());
        }
        return retValue;
    }

    public DsDef[] getDsDefs(long requiredUptime) {
        List<DsDef> dsList = new ArrayList<DsDef>(dsMap.size());
        for(Map.Entry<String, DsDesc<KeyType>> e: dsMap.entrySet()) {
            DsDesc<KeyType> desc = e.getValue();
            if(desc.dsType != null)
                dsList.add(new DsDef(e.getKey(), desc.dsType, requiredUptime, desc.minValue, desc.maxValue));
        }
        return dsList.toArray(new DsDef[dsList.size()]);
    }

    public Collection<String> getDs() {
        HashSet<String> dsList = new HashSet<String>(dsMap.size());
        for(Map.Entry<String, DsDesc<KeyType>> e: dsMap.entrySet()) {
            if(e.getValue().dsType != null)
                dsList.add(e.getKey());
        }
        return dsList;
    }

    public boolean dsExist(String dsName) {
        DsDesc<KeyType> dd = dsMap.get(dsName);
        return (dd != null && dd.dsType != null);
    }

    /**
     * @return The number of data store
     */
    public int getSize() {
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
     * Used to set à string template
     * 
     * @param index
     */
    public void setIndex(String index) {
        if(index != null && !index.isEmpty())
            specific.put("index", index);
    }

    /**
     * Return the string template or null
     * 
     * @return
     */
    public String getIndex() {
        return specific.get("index");
    }

    /**
     * @param uptimefactor the uptimefactor to set
     */
    public void setUptimefactor(float uptimefactor) {
        this.uptimefactor = uptimefactor;
    }

    /**
     * @return Returns the list of graph names.
     */
    public Collection<String> getGraphs() {
        return graphesList;
    }

    /**
     * @param graph a graph name to add.
     */
    public void addGraph(String graph) {
        graphesList.add(graph);
    }

    public Class<? extends Probe<KeyType, ?>> getProbeClass() {
        return probeClass;
    }

    @SuppressWarnings("unchecked")
    public void setProbeClass(Class<? extends Probe<KeyType, ?>> probeClass) throws InvocationTargetException {
        beans.putAll(ArgFactory.getBeanPropertiesMap(probeClass, Probe.class));
        for (ProbeMeta pm: ArgFactory.enumerateAnnotation(probeClass, ProbeMeta.class, StarterNode.class)) {
            Class<? extends CollectResolver<?>> cr = pm.collectResolver();
            if (cr == CollectResolver.NoneResolver.class) {
                continue;
            } else {
                try {
                    collectResolver = (CollectResolver<KeyType>) pm.collectResolver().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new InvocationTargetException(e);
                }
                break;
            }
        }
        this.probeClass = probeClass;
    }

    public Iterable<GenericBean> getBeans() {
        return new Iterable<GenericBean>() {
            public Iterator<GenericBean> iterator() {
                return beans.values().iterator();
            }
        };
    }

    public GenericBean getBean(String name) {
        return beans.get(name);
    }

    public void addBean(GenericBean bean) {
        beans.put(bean.getName(), bean);
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

    public void addDefaultBean(String beanName, String beanValue, boolean finalBean) throws InvocationTargetException {
        ProbeDesc.DefaultBean attr = new ProbeDesc.DefaultBean(beanValue, finalBean);
        if(defaultsBeans.size() == 0)
            defaultsBeans = new HashMap<String, ProbeDesc.DefaultBean>();
        if(beans.containsKey(beanName)) {
            defaultsBeans.put(beanName, attr);
            logger.trace(Util.delayedFormatString("Adding bean %s=%s to default beans", beanName, beanValue));
        }
    }

    public Map<String, ProbeDesc.DefaultBean> getDefaultBeans() {
        Map<String, ProbeDesc.DefaultBean> beans = new HashMap<String, ProbeDesc.DefaultBean>(defaultsBeans.size());
        beans.putAll(defaultsBeans);
        return beans;
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
        Element root = document.createElement("probedesc");
        document.appendChild(root);
        root.appendChild(document.createElement("name")).setTextContent(name);
        if(probeName != null)
            root.appendChild(document.createElement("probeName")).setTextContent(probeName);
        root.appendChild(document.createElement("probeClass")).setTextContent(probeClass.getName());

        // Setting specific values
        for(Map.Entry<String, String> e: specific.entrySet()) {
            Element specElement = (Element) root.appendChild(document.createElement("specific"));
            specElement.setAttribute("name", e.getKey());
            specElement.setTextContent(e.getValue());
        }
        // Setting the uptime factor
        if(uptimefactor != 1.0)
            root.appendChild(document.createElement("uptimefactor")).setTextContent(Float.toString(uptimefactor));

        // Adding all the datastores
        for(Map.Entry<String, DsDesc<KeyType>> e: dsMap.entrySet()) {
            Element dsElement = (Element) root.appendChild(document.createElement("ds"));
            dsElement.appendChild(document.createElement("dsName")).setTextContent(e.getKey());
            DsDesc<KeyType> desc = e.getValue();
            if(desc.dsType != null)
                dsElement.appendChild(document.createElement("dsType")).setTextContent(desc.dsType.toString());
            if(desc.collectKey instanceof String)
                dsElement.appendChild(document.createElement("collect")).setTextContent(desc.collectKey.toString());
            if(desc.minValue != MINDEFAULT)
                dsElement.appendChild(document.createElement("minValue")).setTextContent(Double.toString(desc.minValue));
            if(!Double.isNaN(desc.maxValue))
                dsElement.appendChild(document.createElement("maxValue")).setTextContent(Double.toString(desc.maxValue));

        }
        Element graphsElement = (Element) root.appendChild(document.createElement("graphs"));
        for(String graph: graphesList) {
            graphsElement.appendChild(document.createElement("name")).setTextContent(graph);
        }
        return document;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        return (ProbeDesc<KeyType>) super.clone();
    }

    /**
     * Return true if the given datasource was associated with an optional
     * collect string
     * 
     * @param dsName
     * @return
     */
    boolean isOptional(String dsName) {
        return optionals.contains(dsName);
    }

    public Set<KeyType> getOptionalsCollect(Probe<KeyType, ?> p) {
        Set<KeyType> newOptionals = new HashSet<>(optionals.size());
        optionals.stream()
        .map(o -> Util.parseTemplate(o, p, this))
        .map(collectResolver::resolve)
        .forEach(newOptionals::add);
        return newOptionals;
    }

}
