package jrds.objects;

import java.util.Set;

import jrds.GraphNode;

public class FilterTag extends Filter {
	private String tag;
	
	public FilterTag(String tag) {
		super();
		this.tag = tag;
	}

	@Override
	public boolean acceptGraph(GraphNode graph, String path) {
		Set<String> hostTags = graph.getProbe().getHost().getTags();
		for(String oneTag: hostTags) {
			if(tag.equals(oneTag))
				return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return tag;
	}

}
