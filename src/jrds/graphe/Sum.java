package jrds.graphe;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.AutonomousGraphNode;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.ProxyPlottableMap;

import org.apache.log4j.Logger;
import org.rrd4j.core.FetchData;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;

public class Sum extends AutonomousGraphNode {
    static final private Logger logger = Logger.getLogger(Sum.class);
    static int i;

    String name;
    ArrayList<String> graphList;
    HostsList hl;
    GraphDesc oldgd;
    GraphDesc newgd;

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

        String aname = graphList.get(0);
        GraphNode g = hl.getGraphById(aname.hashCode());
        if(g != null){
            oldgd = g.getGraphDesc();
            try {
                newgd  = (GraphDesc) oldgd.clone();
                newgd.setGraphTitle(name);
                setGraphDesc(newgd);
            } catch (CloneNotSupportedException e) {
                logger.fatal("GraphDesc is supposed to be clonnable, what happened ?", e);
                throw new RuntimeException("GraphDesc is supposed to be clonnable, what happened ?");
            }
        }

        logger.debug(getQualifieName());       
    }

    /* (non-Javadoc)
     * @see jrds.GraphNode#getCustomData()
     */
    @Override
    public ProxyPlottableMap getCustomData() {
        ProxyPlottableMap sumdata = new ProxyPlottableMap() {
            @Override
            public void configure(long start, long end, long step) {
                for(Map.Entry<String, Plottable> e: getSum(start, end).entrySet()) {
                    ProxyPlottableMap.ProxyPlottable pp = new ProxyPlottableMap.ProxyPlottable();
                    pp.setReal(e.getValue());
                    put(e.getKey(), pp);
                }
            }
            private Map<String,Plottable> getSum(long start, long end) {
                Map<String,Plottable> ownValues = new HashMap<String,Plottable>();  

                //Used to kept the last fetched data and analyse the
                FetchData fd = null;

                double[][] allvalues = null;
                for(String name : graphList) {
                    GraphNode g = hl.getGraphById(name.hashCode());
                    logger.trace("Looking for " + name + " in graph base, and found " + g);
                    if(g != null) {
                        fd = g.getProbe().fetchData(new Date(start * 1000), new Date(end * 1000));
                        
                        //First pass, no data tu use
                        if(allvalues == null) {
                            allvalues = (double[][]) fd.getValues().clone();
                        }
                        //Next step, sum previous values
                        else {
                            double[][] tempallvalues = fd.getValues();
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
                    long[] ts = fd.getTimestamps();
                    String[] dsNames = fd.getDsNames();
                    for(int i= 0; i < fd.getColumnCount(); i++) {
                        Plottable pl = new LinearInterpolator(ts, allvalues[i]);
                        ownValues.put(dsNames[i], pl);
                    }
                }
                return ownValues;
            }
        };
        return sumdata;
    }

}
