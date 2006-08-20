package jrds;

//----------------------------------------------------------------------------
//$Id$

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.media.jai.JAI;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdExport;
import org.jrobin.graph.RrdExportDef;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;

/**
 * @author bacchell
 * @version $Revision$
 * TODO
 */
public class RdsGraph
implements Comparable {

	static final private Logger logger = Logger.getLogger(RdsGraph.class);
	static final private SimpleDateFormat lastUpdateFormat = new
	SimpleDateFormat("dd/MM/yyyy HH:mm");

	protected Probe probe;
	private String viewPath = null;
	private GraphDesc gd;
	private String name = null;
	private String graphTitle = null;

	/**
	 *
	 */
	public RdsGraph(Probe theStore, GraphDesc gd) {
		super();
		this.probe = theStore;
		this.gd = gd;
	}

	public void ResolvRealSize(){
		
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getQualifieName().hashCode();
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return gd.getHeight();
	}

	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return gd.getWidth();
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

	public String getPngName() {
		return getName().replaceAll("/","_") + ".png";
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
		Object[] arguments = {
				gd.getGraphName(),
				probe.getHost().getName(),
				index,
				url,
				probe.getName()
		};
		return MessageFormat.format(template, arguments) ;

	}

	public String getGraphTitle() {
		if(graphTitle == null) {
			graphTitle = parseTemplate(gd.getGraphTitle());
		}
		return graphTitle;
	}

	protected String getName() {
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
	protected double getLowerLimit() {
		return gd.getLowerLimit();
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getUpperLimit()
	 */
	protected double getUpperLimit() {
		return gd.getUpperLimit();
	}

	protected RrdGraphDef getRrdDef() throws RrdException, IOException {
		return getGraphDesc().getGraphDef(probe);
	}

	/**
	 * Add the graph formating information to a RrdGraphDef
	 * @param graphDef
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws RrdException
	 */
	protected RrdGraphDef graphFormat(RrdGraphDef graphDef, Date startDate, Date endDate) throws RrdException {
		Date lastUpdate = probe.getLastUpdate();
		graphDef.setTitle(getGraphTitle());
		graphDef.comment("@l");
		graphDef.comment("@l");
		graphDef.comment("Last update: " + 
				lastUpdateFormat.format(lastUpdate) + "@l");
		graphDef.comment("Period from " + lastUpdateFormat.format(startDate) +
				" to " + lastUpdateFormat.format(endDate) + "@L");
		graphDef.comment("Source type: " + getProbe().getSourceType() + "@r");
		return graphDef;		
	}

	public RrdGraph getRrdGraph(Date startDate, Date endDate) throws
	IOException, RrdException {
		Date lastUpdate = probe.getLastUpdate();
		if(endDate.after(lastUpdate))
			endDate = new Date(lastUpdate.getTime() );
		RrdGraphDef tempGraphDef = getRrdDef();
		
		//We normalize the last update time, it can't be used directly
		Date lastUpdateNormalized = new Date(1000L * org.jrobin.core.Util.normalize(getProbe().getLastUpdate().getTime() / 1000L, HostsList.getRootGroup().getResolution()));
		//We dont want to graph past the last normalized update time
		if(endDate.after(lastUpdateNormalized))
			endDate = lastUpdateNormalized;
		tempGraphDef.setTimePeriod(startDate, endDate);
		tempGraphDef = graphFormat(tempGraphDef, startDate, endDate);
		return new RrdGraph(tempGraphDef, true);
	}

	public BufferedImage makeImg(Date startDate, Date endDate) {
		BufferedImage img = null;
		try {
			RrdGraph rrdGraph = getRrdGraph(startDate, endDate);
			img = rrdGraph.getBufferedImage(getWidth(), getHeight());
			gd.setRealHeight(img.getHeight());
			gd.setRealWidth(img.getWidth());
		}
		catch (RrdException e) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					e);
		}
		catch (IOException e) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					e);
		}
		return img;
	}

	public void graph(Date startDate, Date endDate) {
		try {
			writePng(new BufferedOutputStream(new FileOutputStream(new File(
					getPngName()))), startDate, endDate);
		}
		catch (FileNotFoundException e) {
			logger.warn("Unable to creage png for " + getName() +
					" on host " + probe.getHost().getName() + ": " +
					e, e);
		}
	}

	public void writePng(OutputStream out, Date startDate, Date endDate) {
		BufferedImage img = makeImg(startDate, endDate);
		if (img != null)
			try {
				javax.imageio.ImageIO.write(img, "png", out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	public void writeXml(OutputStream out, Date startDate, Date endDate) {
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
	}

	public String writeXml(Date startDate, Date endDate) {
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
	}


	public void writeCsv(OutputStream out, Date startDate, Date endDate){
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

	}
	public byte[] getPngBytes(Date startDate, Date endDate) {
		byte[] retValue = null;
		BufferedImage img = makeImg(startDate, endDate);
		if (img != null) {
			ByteArrayOutputStream imgBytesOs = new ByteArrayOutputStream(img.
					getHeight() * img.getWidth());
			JAI.create("encode", img, imgBytesOs, "PNG", null);
			retValue = imgBytesOs.toByteArray();
		}
		return retValue;
	}

	final public GraphDesc getGraphDesc() {
		return gd;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		if (viewPath == null)
			viewPath = this.getTreePathByView().toString();

		String otherPath = ( (RdsGraph) arg0).getTreePathByView().toString();

		return String.CASE_INSENSITIVE_ORDER.compare(viewPath, otherPath);
	}

	/**
	 * @return Returns the realHeight.
	 */
	public int getRealHeight() {
		return gd.getRealHeight();
	}

	/**
	 * @return Returns the realWidth.
	 */
	public int getRealWidth() {
		return gd.getRealWidth();
	}
	}
