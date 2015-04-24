package jrds.store;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;

public class TestExtractInfo {

    @Test
    public void test1() {
        Date now = new Date();
        ExtractInfo ei = ExtractInfo.get();

        ei = ei.make(now, now).make(1).make("ds").make(ConsolFun.AVERAGE);
        Assert.assertEquals(now, ei.start);
        Assert.assertEquals(now, ei.end);
        Assert.assertEquals("ds", ei.ds);
        Assert.assertEquals(ConsolFun.AVERAGE, ei.cf);
    }

}
