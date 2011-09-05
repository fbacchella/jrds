package jrds;

//----------------------------------------------------------------------------
//$Id$

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.webapp.ACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.graph.RrdGraphDef;

/**
 * @author bacchell
 * @version $Revision$
 * TODO
 */
public class GraphNode implements Comparable<GraphNode>, WithACL {

	static final private Logger logger = Logger.getLogger(GraphNode.class);

	protected Probe<?,?> probe;
	private String viewPath = null;
	private GraphDesc gd;
	private String name = null;
	private String graphTitle = null;
	private ACL acl = ACL.ALLOWEDACL;
	private ProxyPlottableMap customData = null;

	/**
	 *
	 */
	public GraphNode(Probe<?,?> theStore, GraphDesc gd) {
		super();
		this.probe = theStore;
		this.gd = gd;
		this.acl = gd.getACL();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getQualifieName().hashCode();
	}

	/**
	 * @return Returns the theStore.
	 */
	public Probe<?,?> getProbe() {
		return probe;
	}

	public LinkedList<String> getTreePathByHost() {
		return gd.getHostTree(this);
	}

	public LinkedList<String> getTreePathByView() {
		return gd.getViewTree(this);
	}

	private final String parseTemplate(String template) {
		//Don't lose time with an empty template
		if(template == null || "".equals(template.trim())) {
			return template;
		}

		String index = "";
		String url = "";
		if( probe instanceof IndexedProbe) {
			index =((IndexedProbe) probe).getIndexName();
		}
		if( probe instanceof UrlProbe) {
			url =((UrlProbe) probe).getUrlAsString();
		}

		Object[] arguments = {
				gd.getGraphName(),
				probe.getHost().getName(),
				index,
				url,
				probe.getName(),
				Util.stringSignature(index),
				Util.stringSignature(url)
		};
		String evaluted = jrds.Util.parseTemplate(template, probe, gd);
		String formated;
		try {
			formated = MessageFormat.format(evaluted, arguments);
			return formated;
		} catch (IllegalArgumentException e) {
			logger.error("Template invalid:" + template);
		}
		return evaluted;

	}

	public String getGraphTitle() {
		if(graphTitle == null) {
			graphTitle = parseTemplate(gd.getGraphTitle());
		}
		return graphTitle;
	}

	public String getName() {
		if(name == null) {
			name = parseTemplate(gd.getGraphName());
		}
		return name;
	}

	/**
	 * Return a uniq name for the graph
	 * @return
	 */
	public String getQualifieName() {
		return probe.getHost().getName() + "/"  + getName();
	}

	final public GraphDesc getGraphDesc() {
		return gd;
	}
	
    /**
     * Provide a RrdGraphDef with template resolved for the node
     * @return a RrdGraphDef with some default values
     * @throws IOException
     */
    public RrdGraphDef getEmptyGraphDef() {
        RrdGraphDef retValue = getGraphDesc().getEmptyGraphDef();
        retValue.setTitle(getGraphTitle());
        return retValue;
    }

//    public RrdGraphDef getRrdGraphDef() throws IOException {
//		return getGraphDesc().getGraphDef(getProbe(), customData);
//	}
//
//	public RrdGraphDef getRrdGraphDef(Map<String, Plottable> ownData) throws IOException {
//		return getGraphDesc().getGraphDef(getProbe(), ownData);
//	}

	public Graph getGraph() {
		return new Graph(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(GraphNode arg0) {
		if (viewPath == null)
			viewPath = this.getTreePathByView().toString();

		String otherPath = arg0.getTreePathByView().toString();

		return String.CASE_INSENSITIVE_ORDER.compare(viewPath, otherPath);
	}

	@Override
	public String toString() {
		return probe.toString() + "/" + getName();
	}
	
	public void addACL(ACL acl) {
		this.acl = this.acl.join(acl);
	}

	public ACL getACL() {
		return acl;
	}

	/**
	 * @return the customData
	 */
	public ProxyPlottableMap getCustomData() {
		return customData;
	}

	/**
	 * @param customData the customData to set
	 */
	public void setCustomData(ProxyPlottableMap customData) {
		this.customData = customData;
	}

}
