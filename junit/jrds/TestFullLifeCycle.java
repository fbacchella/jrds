package jrds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import jrds.mockobjects.Full;
import jrds.mockobjects.GenerateProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestFullLifeCycle {
    static final private Logger logger = Logger.getLogger(TestFullLifeCycle.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Graph", "jrds.GraphNode");
    }

    @Test
    public void create() throws Exception {
        @SuppressWarnings("unchecked")
        Probe<?,?> p = GenerateProbe.quickProbe(testFolder);
        p.setPd(Full.getPd());
        File rrdFile = new File(p.getMainStore().getPath());
        Assert.assertTrue("Failed to create probe " + rrdFile.getAbsolutePath(), p.checkStore());

        logger.debug("Created " + p + " stored in " + p.getMainStore().getStoreObject());
        long endSec = Full.fill(p);
        logger.debug("fill time: " + endSec);

        logger.debug(p.getLastUpdate());

        Period pr = Full.getPeriod(p, endSec);

        Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();

        GraphDesc gd = Full.getGd();
        gd.initializeLimits(g2d);

        GraphNode gn = new GraphNode(p, gd);
        Graph g = new Graph(gn);
        g.setPeriod(pr);

        int h = g.getDimension().height;
        int w = g.getDimension().width;

        File outputFile =  testFolder.newFile();
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);
        BufferedImage img = ImageIO.read(outputFile);
        Assert.assertEquals(h, img.getHeight());
        Assert.assertEquals(w, img.getWidth());
        Assert.assertTrue(rrdFile.exists());
        Assert.assertTrue(rrdFile.length() > 0);
    }

}
