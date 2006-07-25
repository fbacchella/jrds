package jrds;


public abstract class Filter {
	public abstract boolean acceptGraph(RdsGraph graph, String path);
	public abstract String getName();
}
