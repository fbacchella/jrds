package jrds.graphe;

import java.util.ArrayList;
import java.util.Date;

import jrds.AutonomousGraphNode;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.PlottableMap;
import jrds.Util;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;

import jrds.store.ExtractInfo;
import jrds.store.Extractor;

import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;

public class Sum extends AutonomousGraphNode {
    static final private Logger logger = Logger.getLogger(Sum.class);

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
    };

    public void configure(HostsList hl) {
        super.configure(hl);
        this.hl = hl;

        GraphNode g = null;
        //Check the sum consistency
        for(String graphname: graphList) {
            g = hl.getGraphById(graphname.hashCode());
            if(g == null) {
                logger.warn(Util.delayedFormatString("graph %s not found for sum %s", graphname, getName()));
            }
        }
        //The last graph found is used to clone the graphdesc and use it
        if(g != null){
            try {
                GraphDesc oldgd = g.getGraphDesc();
                GraphDesc newgd  = (GraphDesc) oldgd.clone();
                newgd.setGraphTitle(getName());
                setGraphDesc(newgd);
                logger.debug(Util.delayedFormatString("Adding sum called %s", getQualifieName()));       
            } catch (CloneNotSupportedException e) {
                logger.fatal("GraphDesc is supposed to be clonnable, what happened ?", e);
                throw new RuntimeException("GraphDesc is supposed to be clonnable, what happened ?");
            }
        }
        else {
            logger.error(Util.delayedFormatString("Not graph found in %s definition, unusable sum", getName()) );
        }
    }

    /* (non-Javadoc)
     * @see jrds.GraphNode#getCustomData()
     */
    @Override
    public PlottableMap getCustomData() {
        PlottableMap sumdata = new PlottableMap() {
            @Override
            public void configure(long start, long end, long step) {
                ExtractInfo ei = ExtractInfo.get()
                        .make(new Date(start * 1000), new Date(end * 1000))
                        .make(step)
                        .make(ConsolFun.AVERAGE);
                logger.debug(Util.delayedFormatString("Configuring the sum %s from %d to %d, step %d", Sum.this.getName(), start, end, step));
                //Used to kept the last fetched data and analyse the
                Extractor fd = null;

                double[][] allvalues = null;
                for(String name : graphList) {
                    GraphNode g = hl.getGraphById(name.hashCode());
                    logger.trace("Looking for " + name + " in graph base, and found " + g);
                    if(g != null) {
                        fd = g.getProbe().fetchData();
                        
                        //First pass, no data to use
                        if(allvalues == null) {
                            allvalues = (double[][]) fd.getValues(ei).clone();
                        }
                        //Next step, sum previous values
                        else {
                            double[][] tempallvalues = fd.getValues(ei);
                            for(int c = 0 ; c < tempallvalues.length ; c++) {
                                for(int r = 0 ; r < tempallvalues[c].length; r++) {
                                    double v = tempallvalues[c][r];
                                    if ( ! Double.isNaN(v) ) {
                                        if(! Double.isNaN(allvalues[c][r]))
                                            allvalues[c][r] += v;
                                        else    
                                            allvalues[c][r] = v;

                                    }
                                }
                            }
                        }
                    }
                    else {
                        logger.error("Graph not found: " + name);
                    }
                }
                if(fd != null) {
                    long[] ts = fd.getTimestamps(ei);
                    String[] dsNames = fd.getDsNames();
                    for(int i= 0; i < fd.getColumnCount(); i++) {
                        Plottable pl = new LinearInterpolator(ts, allvalues[i]);
                        put(dsNames[i], pl);
                        logger.trace(Util.delayedFormatString("Added %s to sum plottables", dsNames[i]));
                    }
                }
            }
        };
        return sumdata;
    }

}
