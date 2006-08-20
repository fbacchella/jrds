package jrds;

public class FilterHost extends Filter {
	String hostname = "";
	public FilterHost(String hostname) {
		super();
		this.hostname = hostname;
	}

	@Override
	public boolean acceptGraph(RdsGraph graph, String path) {
		return graph.getProbe().getHost().getName().equals(hostname) && path.startsWith("/" + HostsList.HOSTROOT + "/");
	}

	@Override
	public String getName() {
		return "Filter " + hostname;
	}

	@Override
	public GraphTree setRoot(GraphTree gt) {
		return gt.getByPath("/" + HostsList.HOSTROOT + "/" + hostname);
	}

}
