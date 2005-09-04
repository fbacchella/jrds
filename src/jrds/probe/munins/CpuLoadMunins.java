/*
 * Created on 7 janv. 2005
 *
 * TODO
 */
package jrds.probe.munins;

import jrds.JrdsLogger;
import jrds.MuninsProbe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;
import jrds.GraphDesc;
import java.awt.Color;


/**
 * @author bacchell
 *
 * TODO
 */
public class CpuLoadMunins extends MuninsProbe {
	static private final Logger logger = JrdsLogger.getLogger(CpuLoadMunins.class.getPackage().getName());

	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("la1", ProbeDesc.GAUGE, "load.value");
		pd.setMuninsProbesNames(new String[] { "load" });
		pd.setRrdName("laveragemunins");

                GraphDesc gd = new GraphDesc(3);

                gd.setGraphName("LoadAverage");
                gd.setGraphName("Charge CPU");
                gd.add("la1", GraphDesc.LINE, Color.GREEN, "1mn");
                gd.add("la5", GraphDesc.LINE, Color.BLUE, "5mn");
                gd.add("la15", GraphDesc.LINE, Color.RED, "15mn");
                gd.setVerticalLabel("queue size");
                gd.setHostTree(GraphDesc.HSLT);
                gd.setViewTree(GraphDesc.SLHT);

                pd.setGraphClasses(new Object[] {gd});
	}

	/**
	 * @param monitoredHost
	 */
	public CpuLoadMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
