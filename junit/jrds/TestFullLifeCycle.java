package jrds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;

import jrds.mockobjects.Full;

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
        StoreOpener.prepare("FILE");
        Tools.setLevel(logger, Level.TRACE, "jrds.Graph", "jrds.GraphNode");
    }

    @Test
    public void create() throws IOException, InvocationTargetException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("logevel", logger.getLevel().toString());

        pm.update();

        //We don't want the file, just it's path
        File rrdFile = testFolder.newFile("fullmock.rrd");
        rrdFile.delete();

        Probe<?,?> p = Full.create(testFolder, pm.step);
        p.setStep(pm.step);
        p.setTimeout(pm.timeout);
        //logger.debug(p.getRrdDef().dump());

        logger.debug("Created " + p);
        long endSec = Full.fill(p);
        logger.debug("fill time: " + endSec);

        logger.debug(p.getLastUpdate());

        Period pr = Full.getPeriod(p, endSec);

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
