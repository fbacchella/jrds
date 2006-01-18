/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class MemSolaris9Munins extends MuninsProbe {
	static private final Logger logger = Logger.getLogger(MemSolaris9Munins.class);

	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		
		pd.add("swap_free", ProbeDesc.GAUGE, "swap_free.value");
		pd.add("swap_resv", ProbeDesc.GAUGE, "swap_resv.value");
		pd.add("swap_avail", ProbeDesc.GAUGE, "swap_avail.value");
		pd.add("swap_alloc", ProbeDesc.GAUGE, "swap_alloc.value");
		pd.add("ram_total", ProbeDesc.GAUGE, "ram_total.value");
		pd.add("freemem", ProbeDesc.GAUGE, "freemem.value");
		pd.add("availrmem", ProbeDesc.GAUGE, "availrmem.value");
		pd.add("pp_kernel", ProbeDesc.GAUGE, "pp_kernel.value");
		pd.add("pageslocked", ProbeDesc.GAUGE, "pageslocked.value");
		pd.add("pagestotal", ProbeDesc.GAUGE, "pagestotal.value");
		pd.add("physmem", ProbeDesc.GAUGE, "physmem.value");
		pd.add("swapfs_minfree", ProbeDesc.GAUGE, "swapfs_minfree.value");
		pd.add("disk_total", ProbeDesc.GAUGE, "disk_total.value");
		pd.add("disk_free", ProbeDesc.GAUGE, "disk_free.value");
		pd.setName("memsolaris9munins");
		pd.setMuninsProbesNames(new String[] { "memorysolaris9"});
		pd.setGraphClasses(new Object[] {/*"memorysolaris9.xml", "swapmemorysolaris9.xml",*/ "physicalmemorysolaris9.xml"});
	}
	
	/**
	 * @param monitoredHost
	 */
	public MemSolaris9Munins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
