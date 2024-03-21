package jrds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rrd4j.core.DataHolder;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.IPlottable;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GraphDesc.DsDesc;
import jrds.GraphDesc.GraphType;
import jrds.store.ExtractInfo;
import jrds.store.Extractor;

/**
 * A class that populate a DataProcess or a RrdGraphDef with data from a probe and custom data
 * @author Fabrice Bacchella
 *
 * @param <T>
 */
class DatasourcesPopulator<T extends DataHolder> {

    static final private Logger logger = LoggerFactory.getLogger(DatasourcesPopulator.class);

    private final List<DsDesc> toDo = new ArrayList<>();

    static List<DsDesc> populate(RrdGraphDef graphDef, Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> customData, List<DsDesc> allds, String name) {
        DatasourcesPopulator<RrdGraphDef> p = new DatasourcesPopulator<>(graphDef, defProbe, ei, customData, allds, name);
        return Collections.unmodifiableList(p.toDo);
    }

    static DataProcessor populate(Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> customData, List<DsDesc> allds, String name) {
        DataProcessor dp = ei.getDataProcessor();
        new DatasourcesPopulator<>(dp, defProbe, ei, customData, allds, name);
        return dp;
    }

    private DatasourcesPopulator(T data, Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> customData, List<DsDesc> allds, String name) {
        HostsList hl = defProbe.getHostList();

        // The datasources already found
        Set<String> datasources = new HashSet<>();

        // The needed extractors
        Map<Probe<?, ?>, Extractor> probeDS = new HashMap<>(customData.size() + allds.size());
        probeDS.put(defProbe, defProbe.getMainStore().getExtractor());

        for (DsDesc ds: allds) {
            boolean complete;
            // Not a data source, don't try to add it in datasources
            if (!ds.graphType.datasource()) {
                complete = true;
            }
            // The graph is a percentile
            else if (ds.percentile != null) {
                complete = true;
                if (!datasources.contains(ds.name)) {
                    data.datasource(ds.name, ds.dsName, new Variable.PERCENTILE(ds.percentile));
                    datasources.add(ds.name);
                }
            }
            // A rpn datasource
            else if(ds.rpn != null) {
                complete = true;
                if(!datasources.contains(ds.name)) {
                    data.datasource(ds.name, ds.rpn);
                    datasources.add(ds.name);
                }
            }
            // A legend
            else if(ds.graphType == GraphType.LEGEND) {
                complete = true;
            }
            // Does the datas existe in the provided values
            // It overrides existing values in the probe
            else if(customData != null && customData.containsKey(ds.dsName)) {
                complete = true;
                if(!datasources.contains(ds.name)) {
                    data.datasource(ds.name, customData.get(ds.dsName));
                    datasources.add(ds.name);
                    logger.trace("custom data found for {}", ds.dsName);
                }
            }
            else if (datasources.contains(ds.dsName)) {
                // Already completed, no need to redo
                complete = true;
            }
            // Last but common case, datasource refers to a rrd
            // Or they might be on the associated rrd
            else {
                Probe<?, ?> probe = defProbe;
                if(ds.dspath != null) {
                    // If the host is not defined, use the current host
                    String pathHost = ds.dspath.host;
                    if(pathHost == null) {
                        pathHost = defProbe.getHost().getName();
                    }
                    logger.trace("External probe path: {}/{}/{}", pathHost, ds.dspath.probe, ds.dsName);
                    probe = hl.getProbeByPath(pathHost, ds.dspath.probe);
                    if(probe == null) {
                        logger.error("Invalid probe: {}/{}", pathHost, ds.dspath.probe);
                        continue;
                    }
                }
                if(!probe.dsExist(ds.dsName)) {
                    logger.error("Invalid datasource {}, not found in {}" , ds.dsName, probe);
                    continue;
                }
                complete = true;

                Extractor ex = probeDS.computeIfAbsent(probe, p -> p.getMainStore().getExtractor());
                if(!datasources.contains(ds.name)) {
                    ex.addSource(ds.name, ds.dsName);
                    datasources.add(ds.name);
                } else {
                    logger.error("Datasource '{}' defined twice in {}, for found: {}" , ds.dsName, name, ds);
                }
            }
            if(complete) {
                toDo.add(ds);
            } else {
                logger.debug("Error for {}", ds);
                logger.error("No way to plot {} in {} found", ds.name, name);
            }
        }

        // Fill the graphdef with extracted data
        for(Extractor x: probeDS.values()) {
            x.fill(data, ei);
            x.release();
        }

        logger.trace("Datasource: {}", datasources);
        if (data instanceof RrdGraphDef) {
            logger.trace("Todo: : {}", toDo);
        }

    }

}
