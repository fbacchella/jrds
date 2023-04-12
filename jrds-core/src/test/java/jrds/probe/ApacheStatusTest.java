package jrds.probe;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import jrds.Probe;
import jrds.factories.ArgFactory;
import jrds.factories.ProbeBean;

public class ApacheStatusTest extends ApacheStatusDetails {

    @Test
    public void parse() {
        Map<String, Number> values = new HashMap<>();

        parseScoreboard("_SRWKDCLGI.", values);

        for(WorkerStat w: WorkerStat.values()) {
            Assert.assertEquals(1, values.get(w.name()));
        }

        for(Map.Entry<String, Number> e: values.entrySet()) {
            WorkerStat w = WorkerStat.valueOf(e.getKey());
            Assert.assertEquals(e.getValue(), values.get(w.name()));
            Assert.assertEquals(1, e.getValue());

        }
    }

    @Test
    public void testBeans() {
        Set<String> beans = new HashSet<>();
        for(ProbeBean beansAnnotation: ArgFactory.enumerateAnnotation(ApacheStatusDetails.class, ProbeBean.class, Probe.class)) {
            Collections.addAll(beans, beansAnnotation.value());
        }
        for(String goodBean: HCHttpProbe.class.getAnnotation(ProbeBean.class).value()) {
            Assert.assertTrue(goodBean + " not found", beans.contains(goodBean));
            beans.remove(goodBean);
        }
        for(String goodBean: HttpProbe.class.getAnnotation(ProbeBean.class).value()) {
            Assert.assertTrue(goodBean + " not found", beans.contains(goodBean));
            beans.remove(goodBean);
        }
        Assert.assertEquals("unknown beans  " + beans, 0, beans.size());
    }

}
