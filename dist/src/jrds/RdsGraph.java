/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.media.jai.JAI;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdExportDef;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class RdsGraph implements Comparable {
	
	
	
	static final private Logger logger = JrdsLogger.getLogger(RdsGraph.class.getPackage().getName());
	static final private PropertiesManager pm = PropertiesManager.getInstance();
	static final private SimpleDateFormat lastUpdateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	protected Probe probe;
	private String viewPath = null;
	private GraphDesc gd;

	/**
	 * 
	 */
	public RdsGraph(Probe theStore, GraphDesc gd) {
		super();
		this.probe = theStore;
		this.gd = gd;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if(viewPath == null)
			viewPath = getTreePathByView().toString();
		return viewPath.hashCode();
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return gd.getHeight();
	}
	/**
	 * @param height The height to set.
	 */
	public void setHeight(int height) {
		gd.setHeight(height);
	}
	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return gd.getWidth();
	}
	/**
	 * @param width The width to set.
	 */
	public void setWidth(int width) {
		gd.setWidth(width);
	}
	/**
	 * @return Returns the theStore.
	 */
	public Probe getProbe() {
		return probe;
	}
	/**
	 * @param theStore The theStore to set.
	 */
	public void setProbe(Probe theStore) {
		this.probe = theStore;
	}
	
	protected String initFileNamePrefix()
	{
		return gd.getFilename();
	}
	
	public LinkedList getTreePathByHost() {
		return gd.getHostTree(this);
	}
	
	public LinkedList getTreePathByView() {
		return gd.getViewTree(this);
	}
	
	public String getPngName()
	{
		return gd.getFilename() + ".png";
	}

	/**
	 * @param filename The filename to set.
	 */
	public void setFilename(String filename) {
		if( ! gd.isCloned())
			gd = (GraphDesc) gd.clone();
		gd.setFilename(filename);
	}
	
	protected void setGraphTitle(String title){
		if( ! gd.isCloned()) {
			gd = (GraphDesc) gd.clone();
			logger.debug("Need to clone for " + title );
		}
		gd.setGraphTitle(title);
	}
	
	protected String getGraphTitle()
	{
		return gd.getGraphTitle();
	}

	protected String getGraphSubTitle()
	{
		return gd.getSubTitle();
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
	
	public BufferedImage makeImg(Date startDate, Date endDate)
	{
		BufferedImage img = null;
		try {
			RrdGraphDef tempGraphDef;
			tempGraphDef = fillGraphDef();
			tempGraphDef.setTimePeriod(startDate, endDate);
			Date lastUpdate = probe.getLastUpdate();
			tempGraphDef.comment("@l");
			tempGraphDef.comment("@l");
			tempGraphDef.comment("Dernière mesure : " + lastUpdateFormat.format(lastUpdate) + "@l");
			tempGraphDef.comment("Période du " + lastUpdateFormat.format(startDate) +
					" au "  + lastUpdateFormat.format(endDate) + "@l");
			RrdGraph rrdGraph = new RrdGraph(tempGraphDef);
			img = rrdGraph.getBufferedImage(getWidth(), getHeight());
		} catch (RrdException e) {
			logger.warn("Unable to creage png for " + gd.getFilename() + " on host " + probe.getHost().getName() + ": " + e.getLocalizedMessage(), e);
		} catch (IOException e) {
			logger.warn("Unable to creage png for " + gd.getFilename() + " on host " + probe.getHost().getName() + ": " + e.getLocalizedMessage(), e);
		}
		gd.setRealHeight(img.getHeight());
		gd.setRealWidth(img.getWidth());
		return img;
	}
	
	public void graph(Date startDate, Date endDate)
	{
		try {
			writePng(new BufferedOutputStream(new FileOutputStream(new File(getPngName()))), startDate, endDate);
		} catch (FileNotFoundException e) {
			logger.warn("Unable to creage png for " + gd.getFilename() + " on host " + probe.getHost().getName() + ": " + e.getLocalizedMessage(), e);
		}
	}
	
	public void writePng(OutputStream out, Date startDate, Date endDate)
	{
		BufferedImage img = makeImg(startDate, endDate);
		if(img != null)
			JAI.create("encode", img, out, "PNG", null);
	}
	
	public byte[] getPngBytes(Date startDate, Date endDate)
	{
		byte[] retValue = null;
		BufferedImage img = makeImg(startDate, endDate);
		if(img != null) {
			ByteArrayOutputStream imgBytesOs = new ByteArrayOutputStream(img.getHeight() * img.getWidth());
			JAI.create("encode", img, imgBytesOs, "PNG", null);
			retValue = imgBytesOs.toByteArray();
		}
		return retValue;
	}
	
	protected  GraphDesc getGraphDesc()
	{
		return gd;
	}
	
	/**
	 * @param gd The gd to set.
	 */
	public void setGraphDesc(GraphDesc gd) {
		this.gd = gd;
	}

	protected void addPlots(RrdGraphDef graphDef) throws RrdException
	{
	}

	protected void addDatasource(RrdExportDef graphDef) throws RrdException {
	}
	
	private RrdGraphDef fillGraphDef() throws RrdException, IOException
	{
		RrdGraphDef graphDef = getGraphDesc().getGraphDef(probe);
		addDatasource(graphDef);
		addPlots(graphDef);
		return graphDef;
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		if(viewPath == null)
			viewPath = this.getTreePathByView().toString();
		
		String otherPath = ((RdsGraph) arg0).getTreePathByView().toString();
		
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
