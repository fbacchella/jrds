package fr.jrds.pcp;

import org.junit.Assert;
import org.junit.Test;

public class TestPmId {

    @Test
    public void test() {
        Assert.assertEquals("LINUX.26.0", new PmId(0xf006800).toString());
    }

}
