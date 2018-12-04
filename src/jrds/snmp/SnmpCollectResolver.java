package jrds.snmp;

import org.snmp4j.smi.OID;

import jrds.CollectResolver;

public class SnmpCollectResolver implements CollectResolver<OID> {

    @Override
    public OID resolve(String collectKey) {
        return new OID(collectKey);
    }

}
