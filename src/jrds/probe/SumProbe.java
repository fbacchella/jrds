/*##########################################################################
_##
_##  $Id: BackEndCommiter.java 235 2006-03-01 21:29:48 +0100 (mer., 01 mars 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import jrds.GraphNode;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.graphe.Sum;

import org.apache.log4j.Logger;

public class SumProbe extends VirtualProbe {
	static final private Logger logger = Logger.getLogger(SumProbe.class);

	static final ProbeDesc pd = new ProbeDesc(0) {
		@Override
		public String getName() {
			return "SumProbeDesc";
		}
		@Override
		public Class<? extends Probe> getProbeClass() {
			return SumProbe.class;
		}
		public String getProbeName() {
			return "SumProbeDesc";
		}	
	};

	Collection<String> graphList;

	//An array list is needed, the introspection is picky
	public SumProbe(String name, ArrayList<String> graphList) {
		super(pd);
		logger.debug("new sum: " + name);
		setName(name);
		this.graphList = graphList;
	}

	/**
	 * @return Returns the probeList.
	 */
	public Collection<String> getProbeList() {
		return graphList;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getGraphList()
	 */
	@Override
	public Collection<GraphNode> getGraphList() {
		logger.debug("Returning a sum graphnode");
		return Collections.singleton((GraphNode)new Sum(this));
	}

	@Override
	public Date getLastUpdate() {
		return new Date();
	}
	
}
