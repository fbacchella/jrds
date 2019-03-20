package jrds.snmp;

import org.snmp4j.smi.OID;

import jrds.CollectResolver;

public class SnmpCollectResolver implements CollectResolver<OID> {

    private static final OID NULLOID = new OID(new int[] {0,0});

    @Override
    public OID resolve(String collectKey) {
        try {
            OID collect = new OID(collectKey);
            if (NULLOID.equals(collect) || ! collect.isValid()) {
                throw new IllegalArgumentException("Resolved as invalid: '" + collect + "'");
            }
            return collect;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
