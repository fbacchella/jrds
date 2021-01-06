package jrds.graphe;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.IPlottable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.AutonomousGraphNode;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.PlottableMap;
import jrds.Util;
import jrds.store.ExtractInfo;
import jrds.store.Extractor;

public class Sum extends AutonomousGraphNode {
    static final private Logger logger = LoggerFactory.getLogger(Sum.class);

    private final ArrayList<String> graphList;
    private HostsList hl;

    public Sum(String name, ArrayList<String> graphList) {
        super(name);
        this.graphList = graphList;
        GraphDesc gd = new GraphDesc();
        gd.setGraphName(name);
        gd.setGraphTitle(name);
        gd.setName(name);
        setGraphDesc(gd);
        getProbe().addGraph(this);
    }

    public void configure(HostsList hl) {
        super.configure(hl);
        this.hl = hl;

        GraphNode g = null;
        // Check the sum consistency
        for(String graphname: graphList) {
            g = hl.getGraphById(graphname.hashCode());
            if(g == null) {
                logger.warn("graph {} not found for sum '{}'", graphname, Util.delayedFormatString(this::getName));
            }
        }
        // The last graph found is used to clone the graphdesc and use it
        if(g != null) {
            GraphDesc oldgd = g.getGraphDesc();
            GraphDesc newgd = GraphDesc.getBuilder().fromGraphDesc(oldgd).setGraphTitle(getName()).build();
            newgd.setGraphTitle(getName());
            setGraphDesc(newgd);
            logger.debug("Adding sum called {}", Util.delayedFormatString(this::getQualifiedName));
        } else {
            throw new RuntimeException(String.format("Not graph found in {} definition, unusable sum", Util.delayedFormatString(this::getName)));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.GraphNode#getCustomData()
     */
    @Override
    public PlottableMap getCustomData() {
        return new PlottableMap() {
            @Override
            public void configure(long start, long end, long step) {
                ExtractInfo ei = ExtractInfo.builder()
                                            .interval(Instant.ofEpochSecond(start), Instant.ofEpochSecond(end))
                                            .step(step)
                                            .cf(ConsolFun.AVERAGE)
                                            .build();
                logger.debug("Configuring the sum {} from {} to {}, step {}", Util.delayedFormatString(Sum.this::getName), start, end, step);
                // Used to kept the last fetched data and analyse the
                DataProcessor dp = null;

                double[][] allvalues = null;
                for(String name: graphList) {
                    GraphNode g = hl.getGraphById(name.hashCode());
                    logger.trace("Looking for {} in graph base, and found {}", name, g);
                    if(g == null) {
                        logger.error("Graph not found: {}", name);
                        continue;
                    }

                    try {
                        dp = g.getPlottedDate(ei);
                    } catch (IOException e) {
                        logger.error("Failed to read {}", g.getProbe());
                        continue;
                    }

                    if(g != null) {
                        try (Extractor ex = g.getProbe().fetchData()) {
                            ex.fill(dp, ei);
                        }
                        // First pass, no data to use
                        if(allvalues == null) {
                            allvalues = dp.getValues().clone();
                        }
                        // Next step, sum previous values
                        else {
                            double[][] tempallvalues = dp.getValues();
                            for(int c = 0; c < tempallvalues.length; c++) {
                                for(int r = 0; r < tempallvalues[c].length; r++) {
                                    double v = tempallvalues[c][r];
                                    if(!Double.isNaN(v)) {
                                        if(!Double.isNaN(allvalues[c][r]))
                                            allvalues[c][r] += v;
                                        else
                                            allvalues[c][r] = v;
                                    }
                                }
                            }
                        }
                    }
                }
                if(dp != null) {
                    long[] ts = dp.getTimestamps();
                    String[] dsNames = dp.getSourceNames();
                    for(int i = 0; i < dsNames.length; i++) {
                        IPlottable pl = new LinearInterpolator(ts, allvalues[i]);
                        put(dsNames[i], pl);
                        logger.trace("Added {} to sum plottables", dsNames[i]);
                    }
                } else {
                    logger.error("Sum {} unusable, not graph found", Sum.this);
                }
            }
        };
    }

}
