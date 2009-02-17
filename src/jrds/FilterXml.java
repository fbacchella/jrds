package jrds;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;

/**
 * This a a filter generated using an XML config file
 * @author Fabrice Bacchella 
 * @version $Revision: 302 $,  $Date: 2006-07-25 13:53:54 +0200 (Tue, 25 Jul 2006) $
 */
public class FilterXml extends Filter {
	static private final Logger logger = Logger.getLogger(FilterXml.class);

	final Set<Pattern> goodPaths = new HashSet<Pattern>();
	final Set<Pattern> tags = new HashSet<Pattern>();
	String name;
	
	
	public FilterXml() {
	}
	
	public void addPath(String path) {
		Pattern p = Pattern.compile(path);
		if(p != null)
			goodPaths.add(p);
	}
	
	public void addTag(String tag) {
		Pattern p = Pattern.compile(tag);
		if(p != null)
			tags.add(p);
	}
	
	public boolean acceptGraph(GraphNode graph, String path) {
		return acceptPath(path) &&  acceptTag(graph.getProbe().getTags());
	}

	public boolean acceptPath(String path) {
		//If no path in filter, return true
		boolean valid = true;
		for(Pattern pathp : goodPaths) {
			valid = pathp.matcher(path).matches();
			if(valid)
				break;
		}
		return valid;
	}
	
	public boolean acceptTag(Set<String> probeTags) {
		//All the tags must be matched
		boolean valid = false;
		if(tags.isEmpty())
			valid = true;
		else {
			for(String tag: probeTags) {
				for(Pattern tagp: tags) {
					valid = tagp.matcher(tag).matches();
					if(! valid)
						break;
				}
				if(valid)
					break;
			}
		}
		return valid;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public static void addToDigester(Digester digester) {
		digester.addObjectCreate("filter", jrds.FilterXml.class);
		digester.addCallMethod("filter/name", "setName", 0);
		digester.addCallMethod("filter/path", "addPath", 0);
		digester.addCallMethod("filter/tag", "addTag", 0);
		digester.addRule("filter", new Rule() {
			public void end(String namespace, String name) throws Exception {
				Filter v = (Filter ) digester.peek();
				logger.trace("Adding filter:" + v.getName());
				HostsList.getRootGroup().addFilter(v);
			}
		});
	}
}
