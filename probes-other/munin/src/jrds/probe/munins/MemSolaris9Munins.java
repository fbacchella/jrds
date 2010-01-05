/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;

import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class MemSolaris9Munins extends Munin {
	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		
		pd.add("swap_free", DsType.GAUGE, "swap_free.value");
		pd.add("swap_resv", DsType.GAUGE, "swap_resv.value");
		pd.add("swap_avail", DsType.GAUGE, "swap_avail.value");
		pd.add("swap_alloc", DsType.GAUGE, "swap_alloc.value");
		pd.add("ram_total", DsType.GAUGE, "ram_total.value");
		pd.add("freemem", DsType.GAUGE, "freemem.value");
		pd.add("availrmem", DsType.GAUGE, "availrmem.value");
		pd.add("pp_kernel", DsType.GAUGE, "pp_kernel.value");
		pd.add("pageslocked", DsType.GAUGE, "pageslocked.value");
		pd.add("pagestotal", DsType.GAUGE, "pagestotal.value");
		pd.add("physmem", DsType.GAUGE, "physmem.value");
		pd.add("swapfs_minfree", DsType.GAUGE, "swapfs_minfree.value");
		pd.add("disk_total", DsType.GAUGE, "disk_total.value");
		pd.add("disk_free", DsType.GAUGE, "disk_free.value");
		pd.setName("memsolaris9munins");
		pd.setProbeName("memsolaris9munins");
//		pd.setMuninsProbesNames(new String[] { "memorysolaris9"});
//		pd.setGraphClasses(new Object[] {/*"memorysolaris9.xml", "swapmemorysolaris9.xml",*/ "MemSolaris9Munins"});
	}
}
