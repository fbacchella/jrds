package jrds.probe.munin;

import jrds.ProbeDesc;

import org.rrd4j.DsType;

/**
 * @author bacchell
 *
 * TODO 
 */
public class DiskIoMunins extends MuninIndexed {
	static final private ProbeDesc pd = new ProbeDesc(6);
	static {
		pd.add("rtime", DsType.GAUGE, "rtime.value");
		pd.add("wtime", DsType.GAUGE, "wtime.value");
		pd.add("diskIOReads", DsType.COUNTER, "reads.value");
		pd.add("diskIOWrites", DsType.COUNTER, "writes.value");
		pd.add("diskIONRead", DsType.COUNTER, "nread.value");
		pd.add("diskIONWritten", DsType.COUNTER, "nwritten.value");
		//pd.setMuninsProbesNames(new String[] { "io_busy_${index}", "io_ops_${index}", "io_bytes_${index}"});
		pd.setProbeName("io-{1}_munins");
		//pd.setGraphClasses(new Object[] {"DiskIoGraphBytes", "DiskIoGraphReq", "DiskIoGraphSize"});
	}

//	public Collection getMuninsName()
//	{
//		if(muninsName == null) {
//			Collection muninsTplName = pd.getNamedProbesNames();
//			muninsName = new ArrayList(muninsTplName.size());
//			for(Iterator i = muninsTplName.iterator() ; i.hasNext() ;) {
//				String name = (String) i.next();
//				name += "_" + this.getIndexName().replaceAll("\\d","");
//				muninsName.add(name);
//			}
//		}
//		return muninsName;
//	}
}
