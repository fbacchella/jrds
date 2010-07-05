package jrds;

import java.util.HashSet;
import java.util.Set;

import jrds.graphe.Sum;
import jrds.probe.ContainerProbe;


public abstract class Filter {
	static final public Filter SUM = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return (graph instanceof Sum);
		}
		@Override
		public String getName() {
			return "All sums";
		}
	};
	static final public Filter CUSTOM = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return (graph.getProbe() instanceof ContainerProbe);
		}
		@Override
		public String getName() {
			return "All customs graph";
		}
	};
	static final public Filter EVERYTHING = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return true;
		}
		@Override
		public String getName() {
			return "Everything";
		}
	};
	static final public Filter ALLHOSTS = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			RdsHost host = graph.getProbe().getHost();
			return (! host.isHidden()) && path.startsWith("/" + HostsList.HOSTROOT + "/");
		}
		@Override
		public String getName() {
			return "All hosts";
		}
	};
	static final public Filter ALLVIEWS = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return path.startsWith("/" + HostsList.VIEWROOT + "/");
		}
		@Override
		public String getName() {
			return "All views";
		}
	};
	static final public Filter ALLSERVICES = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return path.startsWith("/" + HostsList.VIEWROOT + "/Services");
		}
		@Override
		public String getName() {
			return "All Services";
		}
		@Override
		public GraphTree setRoot(GraphTree gt) {
			return gt.getByPath("/" + HostsList.VIEWROOT + "/Services");
		}
		
	};
	public abstract boolean acceptGraph(GraphNode graph, String path);
	public abstract String getName();
	public GraphTree setRoot(GraphTree gt) {
		return gt;
	}
	
	private Set<String> roles = new HashSet<String>();
	
	public void addRole(String role) {
		roles.add(role);
	}
	
	public void addRoles(Set<String> roles) {
		this.roles.addAll(roles);
	}
	
	public boolean roleAllowed(String role) {
		return roles.contains(role);
	}
	
	public boolean rolesAllowed(Set<String> roles) {
		return jrds.Util.rolesAllowed(this.roles, roles);
	}

	/**
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

}
