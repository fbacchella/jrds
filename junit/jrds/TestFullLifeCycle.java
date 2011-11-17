package jrds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

import jrds.mockobjects.Full;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFullLifeCycle {
    static final private Logger logger = Logger.getLogger(TestFullLifeCycle.class);

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        StoreOpener.prepare("FILE");
        Tools.setLevel(logger, Level.TRACE, "jrds.Graph", "jrds.GraphNode");
    }

    @AfterClass
    static public void clean() {
        new File("tmp/fullmock.rrd").delete();
    }

    @Test
    public void create() throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", "tmp");
        pm.setProperty("rrddir", "tmp");
        pm.setProperty("logevel", logger.getLevel().toString());
        pm.update();

        File rrdFile = new File("tmp", "fullmock.rrd");
        if(rrdFile.exists())
            rrdFile.delete();

        Probe<?,?> p = Full.create();
        logger.debug("Created " + p);
        long endSec = Full.fill(p);
        logger.debug("fill time: " + endSec);

        logger.debug(p.getLastUpdate());

        Date end = org.rrd4j.core.Util.getDate(endSec);
        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(end);
        calBegin.add(Calendar.MONTH, -1);
        Date begin = calBegin.getTime();

        end = jrds.Util.normalize(end, p.getStep());

        Period pr = new Period();
        pr.setEnd(end);
        pr.setBegin(begin);

        Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();

        GraphDesc gd = Full.getGd();
        gd.initializeLimits(g2d);
        int h = gd.getDimension().height;
        int w = gd.getDimension().width;
        logger.trace(h + " " + w);

        GraphNode gn = new GraphNode(p, Full.getGd());
        Graph g = new Graph(gn);
        g.setPeriod(pr);

        File outputFile =  new File("tmp", "fullmock.png");
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);
        BufferedImage img = ImageIO.read(outputFile);
        Assert.assertEquals(h, img.getHeight());
        Assert.assertEquals(w, img.getWidth());

        logger.trace(h + " " + w);

        StoreOpener.stop();

        Assert.assertTrue(rrdFile.exists());
        Assert.assertTrue(rrdFile.length() > 0);

    }
}
