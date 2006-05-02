package jrds.graphe;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jrds.GraphDesc;
import jrds.HostsList;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.SumProbe;

import org.apache.log4j.Logger;
import org.rrd4j.core.FetchData;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class Sum extends RdsGraph {
	static final private Logger logger = Logger.getLogger(Sum.class);

	static final private GraphDesc gd = new GraphDesc();
	static {
		gd.setGraphName("Sum");
		gd.setGraphTitle("Sum");
		gd.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.TITLE});
		gd.setViewTree(new Object[] {GraphDesc.SERVICES,  "Sum", GraphDesc.TITLE});
	}

	/*Does not change during the short life of this object*/
	private final HostsList hl = HostsList.getRootGroup();

	public Sum(Probe theStore) {
		super(theStore, gd);
		gd.setGraphName(theStore.getName());
		gd.setGraphTitle(theStore.getName());
	}
	
	public RrdGraph getRrdGraph(Date startDate, Date endDate) throws
	IOException {
		SumProbe p = (SumProbe) probe;
		double[][] allvalues = null;
		GraphDesc tempgd = null;
		FetchData fd = null;
		for(Iterator i = p.getProbeList().iterator() ; i.hasNext() ;) {
			String name = (String)i.next();
			RdsGraph g = hl.getGraphById(name.hashCode());
			if(g != null) {
				tempgd = g.getGraphDesc();
				fd = g.getProbe().fetchData(startDate, endDate);
				if(allvalues != null) {
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
				else {
					allvalues = (double[][]) fd.getValues().clone();
				}
			}
			else {
				logger.error("Graph not found: " + name);
			}
		}
		Map ownValues = new HashMap(allvalues.length);
		long[] ts = fd.getTimestamps();
		String[] dsNames = fd.getDsNames();
		for(int i= 0; i < fd.getColumnCount(); i++) {
			Plottable pl = new LinearInterpolator(ts, allvalues[i]);
			ownValues.put(dsNames[i], pl);
		}
		RrdGraphDef tempGraphDef = tempgd.getGraphDef(p, ownValues);
		tempGraphDef.setTimeSpan(startDate.getTime()/1000, endDate.getTime()/1000);
		tempGraphDef = graphFormat(tempGraphDef, startDate, endDate);
		return new RrdGraph(tempGraphDef);
	}
	

}
