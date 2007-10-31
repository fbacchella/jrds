package jrds.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsHost;

public class GetMoke {
	static public RdsHost getHost() {
		return new RdsHost("MokeHost");
	}
	
	static public Probe getProbe() {
		Probe p = new Probe() {
			@Override
			public Map getNewSampleValues() {
				return new HashMap();
			}
			@Override
			public String getSourceType() {
				return "MokeSourceType";
			}
			/* (non-Javadoc)
			 * @see jrds.Probe#getName()
			 */
			@Override
			public String getName() {
				return "MokeProbe";
			}
			/* (non-Javadoc)
			 * @see jrds.Probe#getLastUpdate()
			 */
			@Override
			public Date getLastUpdate() {
				return new Date();
			}
		};
		p.setHost(getHost());
		return p;
	}
	
	static public GraphDesc getGraphDesc() {
		GraphDesc gd = new GraphDesc();
		gd.setGraphName("MokeGD");
		return gd;
	}
}
