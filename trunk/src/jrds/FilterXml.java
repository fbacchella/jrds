package jrds;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
		boolean accepted  = acceptPath(path) &&  acceptTag(graph.getProbe().getTags());
		logger.trace("Trying to accept : " + path +", " + graph.getProbe().getTags() + ": " + accepted);
		return accepted;
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
}
