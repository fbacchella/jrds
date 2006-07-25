package jrds;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;

public class FilterXml {
	static private final Logger logger = Logger.getLogger(FilterXml.class);

	final Set<Pattern> goodPaths = new HashSet<Pattern>();
	final Set<Pattern> tags = new HashSet<Pattern>();
	String name;
	
	static final private Map<String, FilterXml> all = new HashMap<String, FilterXml>();
	
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
	
	public boolean acceptGraph(RdsGraph graph) {
		return acceptTag(graph.getProbe().getTags());
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
	
	public static void add(FilterXml newFilter) {
		all.put(newFilter.getName(), newFilter);
		logger.debug("Filter " + newFilter.getName() + " added");
	}
	
	public static FilterXml get(String name) {
		return all.get(name);
	}
	
	public static void purge() {
		all.clear();
	}
	
	public static boolean getJavaScriptCode(Writer out, String queryString, String curNode, FilterXml f) throws IOException {
		out.append("foldersTree = gFld('All filters');");
		out.append("foldersTree.addChildren([\n");
		for(String filterName: all.keySet()) {
			out.append("    [");
			out.append("'" + filterName +"',");
			out.append("'");
			out.append("index.jsp?filter=");
			out.append(filterName);
			if(queryString != null &&  ! "".equals(queryString)) {
				out.append("&");
				out.append(queryString);
			}
			out.append("'],");
		}
		out.append("]);\n");
		out.append("initializeDocument();");
		return true;
	}

	public static void addToDigester(Digester digester) {
		digester.register("-//jrds//DTD Filter//EN", digester.getClass().getResource("/filter.dtd").toString());
		digester.addObjectCreate("filter", jrds.FilterXml.class);
		digester.addCallMethod("filter/name", "setName", 0);
		digester.addCallMethod("filter/path", "addPath", 0);
		digester.addCallMethod("filter/tag", "addTag", 0);
		digester.addRule("filter", new Rule() {
			public void end(String namespace, String name) throws Exception {
				FilterXml v = (FilterXml) digester.peek();
				FilterXml.add(v);
			}
		});
	}
}
