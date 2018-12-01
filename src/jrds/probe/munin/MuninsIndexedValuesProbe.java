package jrds.probe.munin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabrice Bacchella
 *
 */
public abstract class MuninsIndexedValuesProbe extends MuninIndexed {

    /* (non-Javadoc)
     * @see jrds.Probe#filterValues(java.util.Map)
     */
    @Override
    public Map<String, Number> filterValues(Map<String, Number> valuesList)
    {
        Set<String> collected = getPd().getCollectMapping().keySet();
        Map<String, Number> retValues = new HashMap<String, Number>(collected.size());
        for(String collect: collected) {
            Number value = valuesList.get(getIndexName() + "_" + collect);
            if(value != null)
                retValues.put(collect, value);
        }
        return retValues;
    }
}
