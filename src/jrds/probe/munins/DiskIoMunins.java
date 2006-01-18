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

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.DiskIoGraphBytes;
import jrds.graphe.DiskIoGraphReq;
import jrds.graphe.DiskIoGraphSize;
import jrds.probe.IndexedProbe;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class DiskIoMunins extends MuninsProbe implements IndexedProbe {
	static private final Logger logger = Logger.getLogger(DiskIoMunins.class);
	
	static final private ProbeDesc pd = new ProbeDesc(6);
	static {
		pd.add("rtime", ProbeDesc.GAUGE, "rtime.value");
		pd.add("wtime", ProbeDesc.GAUGE, "wtime.value");
		pd.add("diskIOReads", ProbeDesc.COUNTER, "reads.value");
		pd.add("diskIOWrites", ProbeDesc.COUNTER, "writes.value");
		pd.add("diskIONRead", ProbeDesc.COUNTER, "nread.value");
		pd.add("diskIONWritten", ProbeDesc.COUNTER, "nwritten.value");
		pd.setMuninsProbesNames(new String[] { "io_busy", "io_ops", "io_bytes"});
		pd.setGraphClasses(new Class[] {DiskIoGraphBytes.class, DiskIoGraphReq.class, DiskIoGraphSize.class});
	}

	//static final Collection  muninsname = Arrays.asList(n);

	Collection muninsName = null;
	String indexKey = null;

	/**
	 * @param monitoredHost
	 */
	public DiskIoMunins(RdsHost monitoredHost, String indexKey) {
		super(monitoredHost, (ProbeDesc) pd.clone());
		this.indexKey = indexKey;
		getPd().setName("io-" + getIndexName() + "_munins");
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

	/* (non-Javadoc)
	 * @see com.aol.jrds.probe.IndexedProbe#getIndexName()
	 */
	public String getIndexName() {
		return indexKey;
	}

	public Map filterValues(Map valuesList)
	{
		Map tempMap = new HashMap(getPd().getSize());
		for(Iterator i = valuesList.keySet().iterator() ; i.hasNext(); ) {
			String indexedName = (String) i.next();
			String[] elements = indexedName.split("_");
			String index = elements[0];
			String rootName = elements[1];
			if(indexKey.equals(index) && getPd().getProbesNamesMap().containsKey(rootName))
				tempMap.put(rootName, valuesList.get(indexedName));
		}
		return tempMap;
	}

}
