package jrds.snmp;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OID;

import jrds.CollectResolver;
import jrds.Util;

public class SnmpCollectResolver implements CollectResolver<OID> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final OID NULLOID = new OID(new int[] {0,0});

    /**
     * An OID mapping cache, filled in {@link SnmpConfigurator#configure(jrds.PropertiesManager)}
     */
    static final Map<String, OID> oidmapping = new ConcurrentHashMap<>();

    @Override
    public OID resolve(String collectKey) {
        return oidmapping.computeIfAbsent(collectKey, k -> {
            OID collect = new OID(collectKey);
            if (NULLOID.equals(collect) || ! collect.isValid()) {
                throw new IllegalArgumentException("Resolved as invalid: '" + collectKey + "'");
            }
            logger.debug("Missing from OID mappings: {}={}", collectKey, Util.delayedFormatString(collect::toDottedString));
            return collect;
        });
    }

}
