package jrds;

// ----------------------------------------------------------------------------
// $Id$

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
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

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;




/**
 * @author bacchell
 * @version $Revision$
 * TODO
 */
public class RdsGraph
    implements Comparable {

    static final private Logger logger = JrdsLogger.getLogger(RdsGraph.class);
    static final private PropertiesManager pm = PropertiesManager.getInstance();
    static final private SimpleDateFormat lastUpdateFormat = new
        SimpleDateFormat("dd/MM/yyyy HH:mm");

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
        if (viewPath == null)
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

    protected String initFileNamePrefix() {
        return gd.getFilename();
    }

    public LinkedList getTreePathByHost() {
        return gd.getHostTree(this);
    }

    public LinkedList getTreePathByView() {
        return gd.getViewTree(this);
    }

    public String getPngName() {
        return gd.getFilename() + ".png";
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename(String filename) {
        if (!gd.isCloned())
            gd = (GraphDesc) gd.clone();
        gd.setFilename(filename);
    }

    protected void setGraphTitle(String title) {
        if (!gd.isCloned()) {
            gd = (GraphDesc) gd.clone();
            logger.debug("Need to clone for " + title);
        }
        gd.setGraphTitle(title);
    }

    protected String getGraphTitle() {
        return gd.getGraphTitle();
    }

    protected String getGraphSubTitle() {
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

    private RrdGraph getRrdGraph(Date startDate, Date endDate) throws
        IOException, RrdException {
        RrdGraphDef tempGraphDef;
        tempGraphDef = fillGraphDef();
        tempGraphDef.setTimePeriod(startDate, endDate);
        Date lastUpdate = probe.getLastUpdate();
        tempGraphDef.comment("@l");
        tempGraphDef.comment("@l");
        tempGraphDef.comment("Last update : " +
                             lastUpdateFormat.format(lastUpdate) + "@l");
        tempGraphDef.comment("Period from " + lastUpdateFormat.format(startDate) +
                             " to " + lastUpdateFormat.format(endDate) + "@l");
        return new RrdGraph(tempGraphDef);
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
            logger.warn("Unable to creage png for " + gd.getFilename() +
                        " on host " + probe.getHost().getName() + ": " +
                        e.getLocalizedMessage());
        }
        catch (IOException e) {
            logger.warn("Unable to creage png for " + gd.getFilename() +
                        " on host " + probe.getHost().getName() + ": " +
                        e.getLocalizedMessage());
        }
        return img;
    }

    public void graph(Date startDate, Date endDate) {
        try {
            writePng(new BufferedOutputStream(new FileOutputStream(new File(
                getPngName()))), startDate, endDate);
        }
        catch (FileNotFoundException e) {
            logger.warn("Unable to creage png for " + gd.getFilename() +
                        " on host " + probe.getHost().getName() + ": " +
                        e.getLocalizedMessage(), e);
        }
    }

    public void writePng(OutputStream out, Date startDate, Date endDate) {
        BufferedImage img = makeImg(startDate, endDate);
        if (img != null)
            JAI.create("encode", img, out, "PNG", null);
    }

    public void writeXml(OutputStream out, Date startDate, Date endDate) {
        try {
            RrdGraph rrdGraph = getRrdGraph(startDate, endDate);
            rrdGraph.fetchExportData().exportXml(out);
        }
        catch (RrdException ex) {
            logger.warn("Unable to creage png for " + gd.getFilename() +
                       " on host " + probe.getHost().getName() + ": " +
                       ex.getLocalizedMessage());
       }
        catch (IOException ex) {
            logger.warn("Unable to creage png for " + gd.getFilename() +
                       " on host " + probe.getHost().getName() + ": " +
                       ex.getLocalizedMessage());
       }
   }

   public String writeXml(Date startDate, Date endDate) {
       String xmlData = null;
     try {
         RrdGraph rrdGraph = getRrdGraph(startDate, endDate);
         xmlData = rrdGraph.fetchExportData().exportXml();
     }
     catch (RrdException ex) {
         logger.warn("Unable to creage png for " + gd.getFilename() +
                    " on host " + probe.getHost().getName() + ": " +
                    ex.getLocalizedMessage());
    }
     catch (IOException ex) {
         logger.warn("Unable to creage png for " + gd.getFilename() +
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
           logger.warn("Unable to creage csv for " + gd.getFilename() +
                      " on host " + probe.getHost().getName() + ": " +
                      ex1.getLocalizedMessage(),ex1);
      }
        catch (TransformerException ex) {
            logger.warn("Unable to creage csv for " + gd.getFilename() +
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

    protected GraphDesc getGraphDesc() {
        return gd;
    }

    /**
     * @param gd The gd to set.
     */
    public void setGraphDesc(GraphDesc gd) {
        this.gd = gd;
    }

    private RrdGraphDef fillGraphDef() throws RrdException, IOException {
        RrdGraphDef graphDef = getGraphDesc().getGraphDef(probe);
        return graphDef;
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
