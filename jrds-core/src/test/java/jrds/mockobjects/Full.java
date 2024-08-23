package jrds.mockobjects;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.core.Util;

import jrds.GraphDesc;
import jrds.GraphDesc.GraphType;
import jrds.HostInfo;
import jrds.JrdsSample;
import jrds.Period;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.starter.HostStarter;
import jrds.store.Store;

public class Full {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final String FILE = "fullmock";

    static public final long START = Util.getTimestamp(2003, 4, 1);
    static public final long END = Util.getTimestamp(2003, 5, 1);
    static public final int STEP = 300;

    static final int IMG_WIDTH = 500;
    static final int IMG_HEIGHT = 300;

    static public <KeyType> ProbeDesc<KeyType> getPd() {
        ProbeDesc<KeyType> pd = new ProbeDesc<>();

        pd.add(ProbeDesc.getDataSourceBuilder("sun", DsType.GAUGE));
        pd.add(ProbeDesc.getDataSourceBuilder("shade", DsType.GAUGE));

        pd.setName(FILE);
        pd.setProbeName(FILE);

        return pd;
    }

    static public GraphDesc getGd() {
        GraphDesc gd = GraphDesc.getBuilder().setGraphTitle("Temperatures in May 2003").setVerticalLabel("temperature").build();
        gd.add(GraphDesc.getDsDescBuilder().setName("sun").setGraphType(GraphType.LINE).setColorString("green"));
        gd.add(GraphDesc.getDsDescBuilder().setName("shade").setGraphType(GraphType.LINE).setColorString("blue"));
        gd.add(GraphDesc.getDsDescBuilder().setName("median").setRpn("sun,shade,+,2,/").setGraphType(GraphType.LINE).setColorString("magenta"));
        gd.add(GraphDesc.getDsDescBuilder().setName("diff").setRpn("sun,shade,-,ABS,-1,*").setGraphType(GraphType.AREA).setColorString("magenta"));
        return gd;
    }

    static public Probe<?, ?> getProbe() throws InvocationTargetException {
        Probe<?, ?> p = new Probe<String, Number>() {

            @Override
            public Map<String, Number> getNewSampleValues() {
                return Collections.emptyMap();
            }

            @Override
            public String getSourceType() {
                return "fullmoke";
            }
        };
        Map<String, String> empty = Collections.emptyMap();
        p.setMainStore(new jrds.store.RrdDbStoreFactory(), empty);
        p.setPd(getPd());
        return p;

    }

    static public Probe<?, ?> create(TemporaryFolder testFolder, int step) throws InvocationTargetException {
        HostInfo host = new HostInfo("Empty");
        host.setHostDir(testFolder.getRoot());

        Probe<?, ?> p = getProbe();
        p.setHost(new HostStarter(host));
        p.setStep(step);

        Assert.assertTrue("Fail creating probe", p.checkStore());

        return p;
    }

    static public <SO> long fill(Probe<?, ?> p) {
        long start = System.currentTimeMillis() / 1000;
        long end = start + 3600 * 24 * 30;

        // update database
        GaugeSource sunSource = new GaugeSource(1200, 20);
        GaugeSource shadeSource = new GaugeSource(300, 10);
        long t = start;

        @SuppressWarnings("unchecked")
        Store<SO> store = (Store<SO>) p.getMainStore();
        // Keep a handle to the object, for faster run
        SO o = store.getStoreObject();

        while (t <= end + 86400L) {
            JrdsSample sample = p.newSample();
            sample.setTime(new Date(t * 1000));
            sample.put("sun", sunSource.getValue());
            sample.put("shade", shadeSource.getValue());
            p.getMainStore().commit(sample);
            t += RANDOM.nextDouble() * STEP + 1;
        }
        store.closeStoreObject(o);
        return t;
    }

    static public Period getPeriod(Probe<?, ?> p, long endSec) {
        Date end = org.rrd4j.core.Util.getDate(endSec);
        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(end);
        calBegin.add(Calendar.MONTH, -1);
        Date begin = calBegin.getTime();

        end = jrds.Util.normalize(end, p.getStep());

        Period pr = null;
        try {
            pr = new Period(Long.toString(begin.getTime()), Long.toString(end.getTime()));
        } catch (ParseException e) {
        }
        return pr;
    }

    static class GaugeSource {
        private double value;
        private final double step;

        GaugeSource(double value, double step) {
            this.value = value;
            this.step = step;
        }

        long getValue() {
            double oldValue = value;
            double increment = RANDOM.nextDouble() * step;
            if(RANDOM.nextDouble() > 0.5) {
                increment *= -1;
            }
            value += increment;
            if(value <= 0) {
                value = 0;
            }
            return Math.round(oldValue);
        }
    }

}
