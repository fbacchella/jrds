/*##########################################################################
_##
_##  $Id: BackEndCommiter.java 235 2006-03-01 21:29:48 +0100 (mer., 01 mars 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jrds.GraphNode;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.graphe.Sum;

import org.apache.log4j.Level;

public class SumProbe extends VirtualProbe {

	static final ProbeDesc pd = new ProbeDesc(0) {
		@Override
		public String getName() {
			return "SumProbeDesc";
		}
		@Override
		public Class<? extends Probe<?, ?>> getProbeClass() {
			return (Class<? extends Probe<?, ?>>) SumProbe.class;
		}
		public String getProbeName() {
			return "SumProbeDesc";
		}	
	};

	Collection<String> graphList;
	Set<String> roles = new HashSet<String>();

	//An array list is needed, the introspection is picky
	public SumProbe(String name, ArrayList<String> graphList) {
		super(pd);
		log(Level.DEBUG, "new sum: %s", name);
		setName(name);
		this.graphList = graphList;
	}

	/**
	 * @return Returns the probeList.
	 */
	public Collection<String> getProbeList() {
		return graphList;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getGraphList()
	 */
	@Override
	public Collection<GraphNode> getGraphList() {
		log(Level.DEBUG, "Returning a sum graphnode");
		Sum s = new Sum(this);
		s.addRoles(roles);
		return Collections.singleton((GraphNode)s);
	}

	@Override
	public Date getLastUpdate() {
		return new Date();
	}
	
	public void addRole(String role) {
		roles.add(role);
	}

	/**
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}
	
}
