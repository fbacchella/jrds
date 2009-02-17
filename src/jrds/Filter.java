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
			return HostsList.SUMROOT;
		}
	};
	static final public Filter CUSTOM = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return (graph.getProbe() instanceof ContainerProbe);
		}
		@Override
		public String getName() {
			return HostsList.CUSTOMROOT;
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
			return path.startsWith("/" + HostsList.HOSTROOT + "/");
		}
		@Override
		public String getName() {
			return HostsList.HOSTROOT;
		}
	};
	static final public Filter ALLVIEWS = new Filter() {
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return path.startsWith("/" + HostsList.VIEWROOT + "/");
		}
		@Override
		public String getName() {
			return HostsList.VIEWROOT;
		}
	};
	static final public Filter ALLSERVICES = new Filter() {
		static final private String ROOTNAME = "/" + HostsList.VIEWROOT + "/Services";
		@Override
		public boolean acceptGraph(GraphNode graph, String path) {
			return path.startsWith(ROOTNAME);
		}
		@Override
		public String getName() {
			return "All Services";
		}
		@Override
		public GraphTree setRoot(GraphTree gt) {
			return gt.getByPath(ROOTNAME);
		}
		
	};
	public abstract boolean acceptGraph(GraphNode graph, String path);
	public abstract String getName();
	public GraphTree setRoot(GraphTree gt) {
		return gt;
	}
}
