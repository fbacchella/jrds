package jrds;

//----------------------------------------------------------------------------
//$Id$

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.rrd4j.graph.RrdGraphDef;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

/**
 * @author bacchell
 * @version $Revision$
 * TODO
 */
public class GraphNode implements Comparable<GraphNode> {

	protected Probe probe;
	private String viewPath = null;
	private GraphDesc gd;
	private String name = null;
	private String graphTitle = null;

	/**
	 *
	 */
	public GraphNode(Probe theStore, GraphDesc gd) {
		super();
		this.probe = theStore;
		this.gd = gd;
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
	public Probe getProbe() {
		return probe;
	}

	public LinkedList<String> getTreePathByHost() {
		return gd.getHostTree(this);
	}

	public LinkedList<String> getTreePathByView() {
		return gd.getViewTree(this);
	}

	private final String parseTemplate(String template) {
		String index = "";
		String url = "";
		if( probe instanceof IndexedProbe) {
			index =((IndexedProbe) probe).getIndexName();
		}
		if( probe instanceof UrlProbe) {
			url =((UrlProbe) probe).getUrlAsString();
		}
		Map<String, Object> env = new LinkedHashMap<String, Object>();
		env.put("graphdesc.name", gd.getGraphName());
		env.put("host", probe.getHost().getName());
		env.put("index", index);
		env.put("url", url);
		env.put("probename", probe.getName());
		env.put("index.signature", jrds.Util.stringSignature(index));
		env.put("url.signature", jrds.Util.stringSignature(url));
		env.put("graphdesc.title", gd.getGraphTitle());

		Object[] arguments = env.values().toArray();

		/*Object[] arguments = {
				gd.getGraphName(),
				probe.getHost().getName(),
				index,
				url,
				probe.getName(),
				Util.stringSignature(index),
				Util.stringSignature(url)
		};*/
		return MessageFormat.format(jrds.Util.evaluateVariables(template, env), arguments) ;

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

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getLowerLimit()
	 */
	//protected double getLowerLimit() {
	//	return gd.getLowerLimit();
	//}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getUpperLimit()
	 */
	//protected double getUpperLimit() {
	//	return gd.getUpperLimit();
	//}

	/*protected RrdGraphDef getRrdDef() throws IOException {
		return getGraphDesc().getGraphDef(probe);
	}*/

	/*public void writeXml(OutputStream out, Date startDate, Date endDate) {
		try {
			RrdExportDef exdef = getRrdDef();
			exdef.setTimePeriod(startDate, endDate);
			RrdExport ex = new RrdExport(exdef);
			ex.fetch().exportXml(out);
		}
		catch (RrdException ex) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex.getLocalizedMessage());
		}
		catch (IOException ex) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex.getLocalizedMessage());
		}
	}*/

	/*public String writeXml(Date startDate, Date endDate) {
		String xmlData = "";
		try {
			RrdExportDef exdef = getRrdDef();
			exdef.setTimePeriod(startDate, endDate);
			RrdExport ex = new RrdExport(exdef);
			xmlData = ex.fetch().exportXml();
		}
		catch (RrdException ex) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex.getLocalizedMessage());
		}
		catch (IOException ex) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex.getLocalizedMessage());
		}
		return xmlData;
	}*/


	/*public void writeCsv(OutputStream out, Date startDate, Date endDate){
		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			StreamSource stylesource = new StreamSource(jrds.xmlResources.ResourcesLocator.getResource("jrds.xsl"));
			transformer = tFactory.newTransformer(stylesource);
			StringReader reader = new java.io.StringReader(writeXml(startDate, endDate));
			Source source = new StreamSource(reader);
			StreamResult result = null;
			result = new StreamResult(out);
			transformer.transform(source, result);
		}
		catch (TransformerConfigurationException ex1) {
			logger.warn("Unable to creage csv for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex1.getLocalizedMessage(),ex1);
		}
		catch (TransformerException ex) {
			logger.warn("Unable to creage csv for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					ex.getLocalizedMessage(),ex);
		}

	}*/
	final public GraphDesc getGraphDesc() {
		return gd;
	}
	
	public RrdGraphDef getRrdGraphDef() throws IOException {
		return getGraphDesc().getGraphDef(getProbe());
	}

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

	/**
	 * @return Returns the height of the graphic zone.
	 */
	//public int getHeight() {
		//return gd.getHeight();
	//}

	/**
	 * @return Returns the width of the graphic zone.
	 */
	//public int getWidth() {
	//	return gd.getWidth();
	//}

	@Override
	public String toString() {
		return probe.toString() + "/" + getName();
	}
}
