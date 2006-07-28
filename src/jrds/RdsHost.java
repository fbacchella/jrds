package jrds;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RdsHost implements Comparable {
	static protected Logger logger = Logger.getLogger(RdsHost.class);
	
	private String name = null;
	private final Set<Probe> allProbes = new TreeSet<Probe>();
	//Default value should be bigger than anything
	//So if something prevent uptime to be collected
	//We still collect values
	private long uptime = Long.MAX_VALUE;
	private Date upTimeProbe = new Date(0);
	private final Set<String> tags = new HashSet<String>();
	private final StartersSet starters = new StartersSet(this);
	
	public RdsHost(String newName)
	{
		name = newName;
		starters.setParent(HostsList.getRootGroup().getStarters());
	}
	
	/**
	 * 
	 */
	public RdsHost() {
		starters.setParent(HostsList.getRootGroup().getStarters());
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getHostDir()
	{
		String rrdDir = PropertiesManager.getInstance().rrddir + PropertiesManager.getInstance().fileSeparator + name;
		return rrdDir;
	}
	
	public void addProbe(Probe rrd)
	{
		//Do not add the probe if the physical store
		//cannot be checked
		if(rrd.checkStore()) {
			allProbes.add(rrd);
		}
	}
	
	public Collection<Probe> getProbes() {
		return allProbes;
		
	}
	
	public void  collectAll()
	{
		starters.startCollect();
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			currrd.collect();
		}
		starters.stopCollect();
	}

	public void graphAll(Date startDate, Date endDate)
	{
		for(Iterator i = allProbes.iterator() ; i.hasNext() ;) {
			Probe currrd= (Probe) i.next();
			Collection gl = currrd.getGraphList();
			if(gl != null) {
				for(Iterator j = currrd.getGraphList().iterator() ; j.hasNext(); ) {
					RdsGraph currGraph= (RdsGraph) j.next();
					currGraph.graph(startDate, endDate);
					logger.debug("Graphing " + currGraph.getName());
				}
			}
		}
	}
	
	public String toString()
	{
		return name;
	}
	
	public int compareTo(Object arg0) {
		
		return String.CASE_INSENSITIVE_ORDER.compare(name, arg0.toString());
	}
	/**
	 * @return Returns the uptime.
	 */
	public long getUptime() {
		return uptime;
	}
	/**
	 * @param uptime The uptime to set.
	 */
	public void setUptime(long uptime) {
		this.uptime = uptime;
		upTimeProbe.setTime(System.currentTimeMillis());
	}
	/**
	 * @return Returns the upTimeProbe.
	 */
	public Date getUpTimeProbe() {
		return upTimeProbe;
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	public Set<String> getTags() {
		return tags;
	}
	
	public Starter addStarter(Starter s) {
		return starters.registerStarter(s, this);
	}
	public StartersSet getStarters() {
		return starters;
	}

}
