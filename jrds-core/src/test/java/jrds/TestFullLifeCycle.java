package jrds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.mockobjects.Full;
import jrds.mockobjects.GenerateProbe;

public class TestFullLifeCycle {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Graph", "jrds.GraphNode");
    }

    @Test
    public void create() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder);
        p.setPd(Full.getPd());
        File rrdFile = new File(p.getMainStore().getPath());
        Assert.assertTrue("Failed to create probe " + rrdFile.getAbsolutePath(), p.checkStore());

        logger.debug("Created " + p + " stored in " + p.getMainStore().getStoreObject());
        long endSec = Full.fill(p);
        logger.debug("fill time: " + endSec);

        logger.debug("{}", p.getLastUpdate());

        Period pr = Full.getPeriod(p, endSec);

        Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();

        GraphDesc gd = Full.getGd();
        gd.initializeLimits(g2d);

        GraphNode gn = new GraphNode(p, gd);
        Graph g = new Graph(gn);
        g.setPeriod(pr);

        int h = g.getDimension().height;
        int w = g.getDimension().width;

        File outputFile = testFolder.newFile();
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);
        BufferedImage img = ImageIO.read(outputFile);
        Assert.assertEquals(h, img.getHeight());
        Assert.assertEquals(w, img.getWidth());
        Assert.assertTrue(rrdFile.exists());
        Assert.assertTrue(rrdFile.length() > 0);
    }

}
