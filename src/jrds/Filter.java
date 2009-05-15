package jrds;

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
			return path.startsWith("/" + getName() + "/");
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
}
