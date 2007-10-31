package jrds.graphe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.Probe;
import jrds.probe.SumProbe;

import org.apache.log4j.Logger;
import org.rrd4j.core.FetchData;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;
import org.rrd4j.graph.RrdGraphDef;

public class Sum extends GraphNode {
	static final private Logger logger = Logger.getLogger(Sum.class);
	static int i;

	public Sum(Probe theStore) {
		super(theStore, new GraphDesc() {
			String name = "sum" + i++;
			/* (non-Javadoc)
			 * @see jrds.GraphDesc#getGraphName()
			 */
			@Override
			public String getGraphName() {
				return name;
			}


		});
		GraphDesc gd = this.getGraphDesc();
		gd.setGraphName(theStore.getName());
		gd.setGraphTitle(theStore.getName());
		gd.setName(theStore.getName());
		gd.setHostTree(new Object[] {GraphDesc.TITLE});
		gd.setViewTree(new Object[] {});
		logger.debug(this.getQualifieName());
	}

	/* (non-Javadoc)
	 * @see jrds.GraphNode#getGraph()
	 */
	@Override
	public Graph getGraph() {
		logger.debug("Wants to graph a sum");
		return new Graph(this) {

			/* (non-Javadoc)
			 * @see jrds.Graph#getRrdGraphDef()
			 */
			@Override
			public RrdGraphDef getRrdGraphDef() throws IOException {
				SumProbe p = (SumProbe) getNode().getProbe();
				double[][] allvalues = null;
				GraphDesc tempgd = null;
				FetchData fd = null;
				HostsList hl = HostsList.getRootGroup();
				for(String name : p.getProbeList()) {
					GraphNode g = hl.getGraphById(name.hashCode());
					if(g != null) {
						tempgd = g.getGraphDesc();
						fd = g.getProbe().fetchData(getStart(), getEnd());
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
				RrdGraphDef tempGraphDef = null;
				if(allvalues != null) {
					Map<String,Plottable> ownValues = new HashMap<String,Plottable>(allvalues.length);
					long[] ts = fd.getTimestamps();
					String[] dsNames = fd.getDsNames();
					for(int i= 0; i < fd.getColumnCount(); i++) {
						Plottable pl = new LinearInterpolator(ts, allvalues[i]);
						ownValues.put(dsNames[i], pl);
					}
					tempGraphDef = tempgd.getGraphDef(p, ownValues);
				}
				return tempGraphDef;
			}
		};
	}
}
