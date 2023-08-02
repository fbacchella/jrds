package jrds;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This a a filter generated using an XML config file
 * 
 * @author Fabrice Bacchella
 */
public class FilterXml extends Filter {
    static private final Logger logger = LoggerFactory.getLogger(FilterXml.class);

    private final Set<Pattern> goodPaths = new HashSet<>();
    private final Set<Pattern> tags = new HashSet<>();
    private final Set<String> names = new HashSet<>();
    private final String name;

    /**
     * Build a XML based filter, given is name
     * 
     * @param name
     */
    public FilterXml(String name) {
        this.name = name;
    }

    /**
     * Add a path regular expression used to match a graph. Any graph that match
     * at least one graph will be accepted. If no path is defined, any graph
     * will match
     * 
     * @param path
     */
    public void addPath(String path) {
        goodPaths.add(Pattern.compile(path));
    }

    /**
     * Add a tag regular expression to filter hosts to get graph from. All the
     * tags added must be match
     * 
     * @param tag
     */
    public void addTag(String tag) {
        tags.add(Pattern.compile(tag));
    }

    /**
     * Add an explicit graph to match
     * 
     * @param qualifiedName
     */
    public void addGraph(String qualifiedName) {
        names.add(qualifiedName);
    }

    public boolean acceptGraph(GraphNode graph, String path) {
        boolean accepted = false;

        // An explicit graph is always accepted
        if(names.contains(graph.getQualifiedName()))
            accepted = true;
        // if neither tags or path, it's refused
        else if(!tags.isEmpty() || !goodPaths.isEmpty())
            accepted = (acceptPath(path) && acceptTag(graph.getProbe()));

        logger.trace("Tried to accept : {}={}, {}: {}", path, graph.getQualifiedName(), graph.getProbe() != null ? graph.getProbe().getTags() : "", accepted);

        return accepted;
    }

    /**
     * Return if a graph path match one of the required one.
     * 
     * @param path
     * @return true if one pattern match or no pattern is defined
     */
    private boolean acceptPath(String path) {
        if(goodPaths.isEmpty())
            return true;
        // If no path in filter, return true
        boolean valid = true;
        for(Pattern pathp: goodPaths) {
            valid = pathp.matcher(path).find();
            if(valid)
                break;
        }
        return valid;
    }

    /**
     * Return if a probe match all the required tags if the probe is null,
     * matches only if no tags are required
     * 
     * @param p
     * @return
     */
    private boolean acceptTag(Probe<?, ?> p) {
        if(tags.isEmpty())
            return true;
        if(p == null)
            return false;
        Set<String> probeTags = p.getTags();
        // All the tags must be matched
        boolean valid = false;
        for(String tag: probeTags) {
            for(Pattern tagp: tags) {
                valid = tagp.matcher(tag).matches();
                if(!valid)
                    break;
            }
            if(valid)
                break;
        }
        return valid;
    }

    public String getName() {
        return name;
    }
}
