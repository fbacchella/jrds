/*
 * Created on 23 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.snmp4j.Target;



/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RdsHost implements Comparable {
	static protected Logger logger = JrdsLogger.getLogger(RdsHost.class);
	
	private String name;
	private Target target;
	private Set allProbes;
	private String group = null;
	
	public RdsHost(String newName)
	{
		allProbes = new TreeSet();
		name = newName;
	}
	
	/**
	 * 
	 */
	public RdsHost() {
		allProbes = new TreeSet();
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
		allProbes.add(rrd);
		
		Collection rdsGraphs = rrd.getGraphList();
		if(rdsGraphs != null)
			for(Iterator i = rdsGraphs.iterator(); i.hasNext() ;) {
				RdsGraph oneGraph = (RdsGraph) i.next();
			}
	}
	
	public Collection getProbes() {
		return allProbes;
		
	}
	
	public void collectAll()
	{
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			try {
				currrd.collect();
			} catch (Exception e) {
				logger.error("Error with probe " + currrd.getName() + ": " + e.getLocalizedMessage(), e);
			}
		}
	}
	
	public void openAll()
	{
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			try {
				currrd.open();
			} catch (IOException e) {
				logger.error("Error while opening probe " + currrd.getName() + ": " + e.getLocalizedMessage());
				e.printStackTrace();
			} catch (RrdException e) {
				logger.error("Error while opening probe " + currrd.getName() + ": " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void closeAll()
	{
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			try {
				currrd.close();
			} catch (IOException e) {
				logger.error("Error while closing probe " + currrd.getName() + ": " + e.getLocalizedMessage());
			} catch (RrdException e) {
				logger.error("Error while closing probe " + currrd.getName() + ": " + e.getLocalizedMessage());
			}
		}
	}
	
	public void syncAll() 
	{
		for(Iterator j = allProbes.iterator() ; j.hasNext() ;) {
			Probe currrd= (Probe) j.next();
			try {
				currrd.sync();
			} catch (IOException e) {
				logger.error("Error while syncing probe " + currrd.getName() + ": " + e.getLocalizedMessage());
			}
		}
	}
	public void graphAll(Date startDate, Date endDate)
	{
		for(Iterator i = allProbes.iterator() ; i.hasNext() ;) {
			Probe currrd= (Probe) i.next();
			for(Iterator j = currrd.getGraphList().iterator() ; j.hasNext(); ) {
				RdsGraph currGraph= (RdsGraph) j.next();
				byte[] pngBytes= currGraph.getPngBytes(startDate, endDate);

				//currGraph.graph(startDate, endDate);
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
}
