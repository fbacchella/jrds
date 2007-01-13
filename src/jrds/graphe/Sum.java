package jrds.graphe;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.GraphDesc;
import jrds.HostsList;
import jrds.Probe;
import jrds.RdsGraph;
import jrds.probe.SumProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.FetchData;
import org.jrobin.core.RrdException;
import org.jrobin.data.LinearInterpolator;
import org.jrobin.data.Plottable;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;

public class Sum extends RdsGraph {
	static final private Logger logger = Logger.getLogger(Sum.class);
	
	static final private GraphDesc gd = new GraphDesc();
	static {
		gd.setGraphName("Sum");
		gd.setGraphTitle("Sum");
		gd.setHostTree(new Object[] {GraphDesc.TITLE});
		gd.setViewTree(new Object[] {});
	}

	Date startDate;
	Date endDate;

	public Sum(Probe theStore) {
		super(theStore, gd);
		gd.setGraphName(theStore.getName());
		gd.setGraphTitle(theStore.getName());
	}
	
	protected RrdGraphDef getRrdDef() throws RrdException, IOException {
		SumProbe p = (SumProbe) probe;
		double[][] allvalues = null;
		GraphDesc tempgd = null;
		FetchData fd = null;
		HostsList hl = HostsList.getRootGroup();
		for(String name : p.getProbeList()) {
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
		Map<String,Plottable> ownValues = new HashMap<String,Plottable>(allvalues.length);
		long[] ts = fd.getTimestamps();
		String[] dsNames = fd.getDsNames();
		for(int i= 0; i < fd.getColumnCount(); i++) {
			Plottable pl = new LinearInterpolator(ts, allvalues[i]);
			ownValues.put(dsNames[i], pl);
		}
		RrdGraphDef tempGraphDef = tempgd.getGraphDef(p, ownValues);
		return tempGraphDef;
	}

	
	public RrdGraph getRrdGraph(Date startDate, Date endDate) throws
	IOException, RrdException {
		this.startDate = startDate;
		this.endDate = endDate;
		return super.getRrdGraph(startDate, endDate);
	}
}
