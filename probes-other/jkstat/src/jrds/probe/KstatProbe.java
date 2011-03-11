package jrds.probe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jrds.factories.ProbeMeta;
import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;

@ProbeMeta(discoverAgent = JKStatDiscoverAgent.class)
public class KstatProbe extends jrds.ProbeConnected<String, Number, KstatConnection> {

    private String module;
    private int instance;
    private String name;

    public KstatProbe() {
        super(KstatConnection.class.getName());
    }

    protected Boolean setup(String module, int instance, String name) {
        this.module = module;
        this.instance = instance;
        this.name = name;
        return true;
    }

    public Boolean configure() {
        String module = getPd().getSpecific("module");
        String name = getPd().getSpecific("name");
        return setup(module, 0, name) && super.configure();
    }

    public Map<String,Number> getNewSampleValuesConnected(KstatConnection cnx) {
        JKstat remoteJk = cnx.getConnection();
        Kstat active  = remoteJk.getKstat(module, instance, name);
        if(active == null) {
            return Collections.emptyMap();
        }
        Set<java.lang.String> statistics = active.statistics();
        Map<String,Number> retValues = new HashMap<String,Number>(statistics.size());
        for(String name: statistics) {
            if(active.isNumeric(name))
                retValues.put(name, (Number) active.getData(name));
        }
        return retValues;
    }

    /* (non-Javadoc)
     * @see jrds.Probe#filterValues(java.util.Map)
     */
    @Override
    public Map<String, Number> filterValues(Map<String, Number> valuesList) {
        Map<String, Number> retValues = new HashMap<String, Number>(getPd().getCollectStrings().size());
        for(String stat: getPd().getCollectStrings().values()) {
            Number val = valuesList.get(stat);
            if(val != null)
                retValues.put(stat, val);
        }
        return retValues;
    }

    public String getSourceType() {
        return "kstat";
    }
}
