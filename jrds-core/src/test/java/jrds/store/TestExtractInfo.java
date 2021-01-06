package jrds.store;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;

public class TestExtractInfo {

    @Test
    public void test1() {
        Date now = new Date();
        ExtractInfo ei = ExtractInfo.builder().interval(now, now).step(1).ds("ds").cf(ConsolFun.MAX).build();
        Assert.assertEquals(now.getTime(), ei.start.toEpochMilli());
        Assert.assertEquals(now.getTime(), ei.end.toEpochMilli());
        Assert.assertEquals("ds", ei.ds);
        Assert.assertEquals(ConsolFun.MAX, ei.cf);
    }

}
