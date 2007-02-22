package jrds;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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
	static private final Logger logger = Logger.getLogger(RdsHost.class);
	
	private String name = null;
	private final Set<Probe> allProbes = new TreeSet<Probe>();
	private Set<String> tags = null;
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
		String rrdDir = HostsList.getRootGroup().getRrdDir() + org.rrd4j.core.Util.getFileSeparator() + name;
		return rrdDir;
	}
	
	public void addProbe(Probe rrd)
	{
		
		rrd.setHost(this);
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
		if(starters != null)
			starters.startCollect();
		for(Probe currrd: allProbes) {
			if( ! currrd.isStarted())
				break;
			currrd.collect();
		}
		if(starters != null)
			starters.stopCollect();
	}

	public void graphAll(Date startDate, Date endDate)
	{
		for(Probe currrd: allProbes) {
			Collection gl = currrd.getGraphList();
			if(gl != null) {
				for(RdsGraph currGraph:  currrd.getGraphList()) {
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

	public void addTag(String tag) {
		if(tags == null)
			tags = new HashSet<String>();
		tags.add(tag);
	}

	public Set<String> getTags() {
		Set<String> temptags = tags;
		if(tags == null)
			temptags = new HashSet<String>();
		return temptags;
	}
	
	public Starter addStarter(Starter s) {
		return starters.registerStarter(s, this);
	}
	
	public StartersSet getStarters() {
		return starters;
	}

}
