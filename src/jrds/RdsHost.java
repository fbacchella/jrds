package jrds;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jrds.starter.Collecting;
import jrds.starter.Resolver;
import jrds.starter.Starter;
import jrds.starter.StarterNode;
import jrds.starter.StartersSet;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RdsHost implements Comparable<RdsHost>, StarterNode {
	static private final Logger logger = Logger.getLogger(RdsHost.class);

	private String name = null;
	private String dnsName = null;
	private final Set<Probe> allProbes = new TreeSet<Probe>();
	private Set<String> tags = null;
	private final StartersSet starters = new StartersSet(this);
	private File hostdir = null;

	public RdsHost(String newName)
	{
		name = newName;
		Starter resolver = new Resolver(name);
		resolver.register(this);
	}

	public RdsHost(String name, String dnsName)
	{
		this.name = name;
		this.dnsName = dnsName;
		Starter resolver = new Resolver(name);
		resolver.register(this);
	}

	/**
	 * 
	 */
	public RdsHost() {
	}

	public void setName(String name)
	{
		this.name = name;
		Starter resolver = new Resolver(name);
		resolver.register(this);
	}

	public String getName()
	{
		return name;
	}

	/**
	 * @param hostdir the hostdir to set
	 */
	public void setHostDir(File hostdir) {
		this.hostdir = hostdir;
	}

	public File getHostDir()
	{
		return hostdir;
	}

//	public void addProbe(Probe rrd)
//	{
//
//		rrd.setHost(this);
//		//Do not add the probe if the physical store
//		//cannot be checked
//		if(rrd.checkStore()) {
//			allProbes.add(rrd);
//		}
//	}

	public Collection<Probe> getProbes() {
		return allProbes;

	}

	public void  collectAll()
	{
		long start = System.currentTimeMillis();
		if(starters != null)
			starters.startCollect();
		for(Probe currrd: allProbes) {
			if(! isCollectRunning() )
				break;
			long duration = (System.currentTimeMillis() - start) /1000 ;
			if(duration > (currrd.getStep() / 2 )) {
				logger.error("Collect for " + this + " ran too long: " + duration + "s");
				break;
			}
			currrd.collect();
		}
		if(starters != null)
			starters.stopCollect();
		long end = System.currentTimeMillis();
		float elapsed = ((float)(end - start))/1000;
		logger.debug("Collect time for " + name + ": " + elapsed + "s");
	}

	public String toString()
	{
		return name;
	}

	public int compareTo(RdsHost arg0)
	{
		return String.CASE_INSENSITIVE_ORDER.compare(name, arg0.toString());
	}

	public void addTag(String tag)
	{
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

	public StartersSet getStarters() {
		return starters;
	}

	public boolean isCollectRunning() {
		return getStarters().isStarted(Collecting.makeKey(this));
	}

	/**
	 * @return the dnsName
	 */
	public String getDnsName() {
		if(dnsName != null)
			return dnsName;
		else
			return name;
	}

	/**
	 * @param dnsName the dnsName to set
	 */
	public void setDnsName(String dnsName) {
		this.dnsName = dnsName;
		Starter resolver = new Resolver(dnsName);
		resolver.register(this);
	}
}
