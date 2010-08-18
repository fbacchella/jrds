package jrds.graphe;

import java.io.File;
import java.io.IOException;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.ProxyPlottableMap;
import jrds.jrrd.ConsolidationFunctionType;
import jrds.jrrd.DataChunk;
import jrds.jrrd.RRDatabase;
import jrds.probe.RRDToolProbe;

public class RRDToolGraphNode extends GraphNode {
	private final File rrdpath;

	public RRDToolGraphNode(RRDToolProbe theStore, GraphDesc gd, File rrdpath) {
		super(theStore, gd);
		this.rrdpath = rrdpath;
	}

	
	/* (non-Javadoc)
	 * @see jrds.GraphNode#getGraph()
	 */
	@Override
	public Graph getGraph() {
		try {
			ProxyPlottableMap pp = new ProxyPlottableMap() {
				{
					RRDatabase db = new RRDatabase(rrdpath);
					for(String name: db.getDataSourcesName()) {
						put(name, new ProxyPlottable());
					}
				}
				@Override
				public void configure(long start, long end, long step) {
					try {
						RRDatabase db = new RRDatabase(rrdpath);
						DataChunk chunck = db.getData(ConsolidationFunctionType.AVERAGE, start, end, step);
						for(String name: db.getDataSourcesName()) {
							get(name).setReal(chunck.toPlottable(name));
						}
					} catch (IOException e) {
					}

				}
			};
			setCustomData(pp);
		} catch (IOException e) {
			throw new RuntimeException("Unable to create ProxyPlottableMap", e);
		}
		return super.getGraph();
	}

}
