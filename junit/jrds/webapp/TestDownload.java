package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.data.DataProcessor;

public class TestDownload extends Download {
    static final private Logger logger = Logger.getLogger(TestDownload.class);

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.webapp.ParamsBean");
    }

    @Test
    public void testEpochFormat() throws Exception
    {
        Date start = epochFormat.get().parse("0");
        String formatted = epochFormat.get().format(start);

        Assert.assertEquals("0", formatted);
        Assert.assertEquals(new Date(0), start);

    }

    @Test
    public void testwriteCsv() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataProcessor dp =  new DataProcessor(1, 10000);
        dp.setStep(10000/2);
        dp.processData();
        writeCsv(out, dp, epochFormat.get() );
        logger.debug(out.toString());
    }
}
