package jrds;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.snmp4j.Target;



/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RdsHost implements Comparable {
	static protected Logger logger = Logger.getLogger(RdsHost.class);
	
	private String name;
	private Target target;
	private Set<Probe> allProbes;
	private String group = null;
	//Default value should be bigger than anything
	//So if something prevent uptime to be collected
	//We still collect values
	private long uptime = Long.MAX_VALUE;
	private Date upTimeProbe = new Date(0);
	private String tree = null;
	private final Set<String> tags = new HashSet<String>();
	private final StartersSet starters = new StartersSet();
	
	public RdsHost(String newName)
	{
		allProbes = new TreeSet<Probe>();
		name = newName;
	}
	
	/**
	 * 
	 */
	public RdsHost() {
		allProbes = new TreeSet<Probe>();
		name = null;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Target getTarget()
	{
		return target;
	}
	
	/**
	 * @param target The target to set.
	 */
	public void setTarget(Target target) {
		this.target = target;
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
		starters.startCollect(this);
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			currrd.collect();
		}
		starters.stopCollect(this);
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
	
	/**
	 * @return Returns the group.
	 */
	public String getGroup() {
		return group;
	}
	/**
	 * @param group The group to set.
	 */
	public void setGroup(String group) {
		this.group = group;
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

	public String getTree() {
		return tree;
	}

	public void setTree(String treeRoot) {
		this.tree = treeRoot;
	}
	public void addTag(String tag) {
		tags.add(tag);
	}

	public Set<String> getTags() {
		return tags;
	}
	
	public void addStarter(Starter s) {
		starters.register(s);
	}

}
