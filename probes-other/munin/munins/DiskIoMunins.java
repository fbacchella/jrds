/*
 * Created on 8 févr. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.ProbeDesc;
import jrds.RdsHost;

/**
 * @author bacchell
 *
 * TODO 
 */
public class DiskIoMunins extends MuninsIndexedNameProbe {
	static final private ProbeDesc pd = new ProbeDesc(6);
	static {
		pd.add("rtime", DsType.GAUGE, "rtime.value");
		pd.add("wtime", DsType.GAUGE, "wtime.value");
		pd.add("diskIOReads", DsType.COUNTER, "reads.value");
		pd.add("diskIOWrites", DsType.COUNTER, "writes.value");
		pd.add("diskIONRead", DsType.COUNTER, "nread.value");
		pd.add("diskIONWritten", DsType.COUNTER, "nwritten.value");
		pd.setMuninsProbesNames(new String[] { "io_busy", "io_ops", "io_bytes"});
		pd.setProbeName("io-{1}_munins");
		pd.setGraphClasses(new Object[] {"DiskIoGraphBytes", "DiskIoGraphReq", "DiskIoGraphSize"});
	}

	/**
	 * @param monitoredHost
	 */
	public DiskIoMunins(RdsHost monitoredHost, String indexKey) {
		super(monitoredHost, pd, indexKey);
	}
	public Collection getMuninsName()
	{
		if(muninsName == null) {
			Collection muninsTplName = pd.getNamedProbesNames();
			muninsName = new ArrayList(muninsTplName.size());
			for(Iterator i = muninsTplName.iterator() ; i.hasNext() ;) {
				String name = (String) i.next();
				name += "_" + this.getIndexName().replaceAll("\\d","");
				muninsName.add(name);
			}
		}
		return muninsName;
	}

	public Map filterValues(Map valuesList)
	{
		Map tempMap = new HashMap(getPd().getSize());
		for(Iterator i = valuesList.keySet().iterator() ; i.hasNext(); ) {
			String indexedName = (String) i.next();
			String[] elements = indexedName.split("_");
			String index = elements[0];
			String rootName = elements[1];
			if(indexKey.equals(index) && getPd().getCollectStrings().containsKey(rootName))
				tempMap.put(rootName, valuesList.get(indexedName));
		}
		return tempMap;
	}

}
