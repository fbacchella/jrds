package jrds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Plottable;
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
class DatasourcesPopulator<T> {

    static final private Logger logger = LoggerFactory.getLogger(DatasourcesPopulator.class);

    final RrdGraphDef graphDef;
    final DataProcessor dp;

    private final List<DsDesc> toDo = new ArrayList<DsDesc>();

    static List<DsDesc> populate(RrdGraphDef graphDef, Probe<?, ?> defProbe, ExtractInfo ei, Map<String, ? extends Plottable> customData, List<DsDesc> allds, String name) {
        DatasourcesPopulator<RrdGraphDef> p = new DatasourcesPopulator<RrdGraphDef>(graphDef, defProbe, ei, customData, allds, name);
        return Collections.unmodifiableList(p.toDo);
    }

    static DataProcessor populate(Probe<?, ?> defProbe, ExtractInfo ei, Map<String, ? extends Plottable> customData, List<DsDesc> allds, String name) {
        DataProcessor dp = ei.getDataProcessor();
        new DatasourcesPopulator<DataProcessor>(dp, defProbe, ei, customData, allds, name);
        return dp;
    }

    private DatasourcesPopulator(T wrapped, Probe<?, ?> defProbe, ExtractInfo ei, Map<String, ? extends Plottable> customData, List<DsDesc> allds, String name) {
        HostsList hl = defProbe.getHostList();

        if (wrapped instanceof RrdGraphDef) {
            graphDef = (RrdGraphDef) wrapped;
            dp = null;
        } else if (wrapped instanceof DataProcessor) {
            dp = (DataProcessor) wrapped;
            graphDef = null;
        } else {
            throw new RuntimeException();
        }

        // The datasources already found
        Set<String> datasources = new HashSet<String>();

        // The needed extractors
        Map<Probe<?, ?>, Extractor> probeDS = new HashMap<Probe<?, ?>, Extractor>(1);
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
                    percentile(ds.name, ds.dsName, ds.percentile);
                    datasources.add(ds.name);
                }
            }
            // A rpn datasource
            else if(ds.rpn != null) {
                complete = true;
                if(!datasources.contains(ds.name)) {
                    datasource(ds.name, ds.rpn);
                    datasources.add(ds.name);
                }
            }
            // A legend
            else if(ds.graphType == GraphType.LEGEND) {
                complete = true;
            }
            // Does the datas existe in the provided values
            // It override existing values in the probe
            else if(customData != null && customData.containsKey(ds.dsName)) {
                complete = true;
                if(!datasources.contains(ds.name)) {
                    datasource(ds.name, customData.get(ds.dsName));
                    datasources.add(ds.name);
                    logger.trace("custom data found for {}", ds.dsName);
                }
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
                        logger.error("Invalid probe: " + pathHost + "/" + ds.dspath.probe);
                        continue;
                    }
                }
                if(!probe.dsExist(ds.dsName)) {
                    logger.error("Invalid datasource " + ds.dsName + ", not found in " + probe);
                    continue;
                }
                complete = true;

                // Add the dsName for the probe found
                if(!probeDS.containsKey(probe)) {
                    probeDS.put(probe, probe.getMainStore().getExtractor());
                }
                Extractor ex = probeDS.get(probe);
                if(!datasources.contains(ds.name)) {
                    ex.addSource(ds.name, ds.dsName);
                    datasources.add(ds.name);
                } else {
                    logger.error("Datasource '" + ds.dsName + "' defined twice in " + name + ", for found: " + ds);
                }
            }
            if(complete) {
                toDo.add(ds);
            } else {
                logger.debug("Error for " + ds);
                logger.error("No way to plot " + ds.name + " in " + name + " found");
            }
        }

        // Fill the graphdef with extracted data
        for(Extractor x: probeDS.values()) {
            if (graphDef != null) {
                x.fill(graphDef, ei);
            } else {
                x.fill(dp, ei);
            }
            x.release();
        }

        logger.trace("Datasource: {}", datasources);
        if (graphDef != null) {
            logger.trace("Todo: : {}", toDo);
        }

    }

    private void datasource(String name, Plottable plottable) {
        if (graphDef != null) {
            graphDef.datasource(name, plottable);
        } else {
            dp.addDatasource(name, plottable);
        }
    }

    private void datasource(String name, String rpn) {
        if (graphDef != null) {
            graphDef.datasource(name, rpn);
        } else {
            dp.addDatasource(name, rpn);
        }
    }

    private void percentile(String name, String dsName, int percentile) {
        if (graphDef != null) {
            graphDef.datasource(name, dsName, new Variable.PERCENTILE(percentile));
        } else {
            dp.addDatasource(name, dsName, new Variable.PERCENTILE(percentile));
        }
    }

}
