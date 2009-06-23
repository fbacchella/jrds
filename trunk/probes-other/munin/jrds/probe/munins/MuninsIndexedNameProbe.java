/*
 * Created on 8 f�vr. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.probe.IndexedProbe;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class MuninsIndexedNameProbe extends MuninsProbe  implements IndexedProbe {
	static final private Logger logger = Logger.getLogger(MuninsIndexedNameProbe.class);
	String indexKey;
	Collection muninsName = null;
	private String label;

	/**
	 * @param monitoredHost
	 * @param pd
	 * @param indexKey
	 */
	public MuninsIndexedNameProbe(RdsHost monitoredHost, ProbeDesc pd, String indexKey) {
		super(monitoredHost, pd);
		this.indexKey = indexKey;
	}
	
	public Collection getMuninsName()
	{
		if(muninsName == null) {
			Collection muninsTplName = initMuninsName();
			muninsName = new ArrayList(muninsTplName.size());
			for(Iterator i = muninsTplName.iterator() ; i.hasNext() ;) {
				String name = (String) i.next();
				name += "_" + indexKey;
				muninsName.add(name);
				logger.debug(name);
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
