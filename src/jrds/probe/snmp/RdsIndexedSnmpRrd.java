package jrds.probe.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.factories.ProbeBean;
import jrds.probe.IndexedProbe;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Level;
import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella
 */
@ProbeBean({"index",  "oid"})
public class RdsIndexedSnmpRrd extends SnmpProbe implements IndexedProbe {

    static public final String INDEXOIDNAME="indexOid";

    Object key;
    String indexKey;
    private OID indexOid;

    static final SnmpRequester indexFinder = SnmpRequester.TABULAR;
    static final SnmpRequester valueFinder = SnmpRequester.RAW;
    
    public boolean configure(String indexKey) {
        this.key = indexKey;
        this.indexKey = indexKey;
        return configure();
    }

    public boolean configure(Integer indexKey) {
        configure();
        this.key = indexKey;
        this.indexKey = String.valueOf(indexKey);
        return configure();
    }

    /**
     * If the key type is an OID, it is directly the OID suffix, no look up will be done
     * @param indexKey
     */
    public boolean configure(OID indexKey) {
        this.key = indexKey;
        this.indexKey = indexKey.toString();
        return configure();
    }

    public boolean configure(String keyName, OID indexKey) {
        this.key = indexKey;
        this.indexKey = keyName;
        return configure();
    }

    protected SnmpRequester getSnmpRequester() {
        return valueFinder;
    }

    /* (non-Javadoc)
     * @see jrds.probe.snmp.SnmpProbe#readSpecific()
     */
    @Override
    public boolean readSpecific() {
        getPd().addSpecific(REQUESTERNAME, "RAW");
        String oidString =  getPd().getSpecific(INDEXOIDNAME);
        if(oidString != null && oidString.length() > 0) 
            indexOid = new OID(oidString);
        return super.readSpecific();
    }

    public String getIndexName(){
        return indexKey;
    }

    public Set<OID> makeIndexed(Collection<OID> oids, int[] index)
    {
        Set<OID> oidToGet = new HashSet<OID>(oids.size());
        for(OID oidCurs: oids) {
                OID oidBuf = new OID(oidCurs);
                for(int i = 0; i <index.length; i++) {
                    oidBuf.append(index[i]);
                }
                oidToGet.add(oidBuf);
        }
        return oidToGet;
    }

    public Collection<OID> getIndexSet() {
        Collection<OID> retValue = null;
        if(indexOid != null)
            retValue = Collections.singleton(indexOid);
        return retValue;
    }

    public int getIndexPrefixLength() {
        return indexOid.size();
    }

    /**
     * Generate the index suffix for the probe
     * @return
     */
    public int[] setIndexValue() 
    {
        Collection<OID> soidSet = getIndexSet();

        //If we already have the key, no need to search for it
        if(key instanceof OID) {
            OID suffixOid = (OID) key;
            return suffixOid.getValue();
        }
        //If no index OID, the indexKey is already the snmp suffix.
        else if(soidSet == null || soidSet.size() == 0) {
            OID suffixOid = new OID(indexKey);
            setSuffixLength(suffixOid.size());
            return suffixOid.getValue();
        }
        else {
            try {
                Map<OID, Object> somevars = indexFinder.doSnmpGet(getSnmpStarter(), soidSet);

                boolean found = false;
                int newSuffixLength = 0;
                for(Map.Entry<OID, Object> e: somevars.entrySet()) {
                    OID tryoid = e.getKey();
                    String name = e.getValue().toString();
                    if(name != null && matchIndex(somevars.get(tryoid))) {
                        newSuffixLength = tryoid.size() - getIndexPrefixLength();
                        int[] index = new int[ newSuffixLength ];
                        for(int i = 0; i < index.length ; i++) {
                            index[i] = tryoid.get(i + getIndexPrefixLength());
                        }
                        return index;
                    }
                }
                if(found) {
                    setSuffixLength(newSuffixLength);
                }
                else  {
                    log(Level.ERROR, "index for %s not found", indexKey);
                }
            } catch (IOException e) {
                log(Level.ERROR, e, "index for %s not found because of %s", indexKey, e);
            }
        }
        return new int[0];
    }

    /**
     * This method check if the tried value match the index
     * @param index the index value 
     * @param key the found key tried
     * @return
     */
    public boolean matchIndex(Object readKey) {
        return key.equals(readKey);
    }

    /**
     * @see jrds.probe.snmp.SnmpProbe#getOidSet()
     */
    public Set<OID> getOidSet() {
        Set<OID> retValue = null;
        int[] indexArray = setIndexValue();
        if(indexArray != null)
            retValue = makeIndexed(getOidNameMap().keySet(), indexArray);
        return retValue;
    }

    /**
     * @return the indexKey
     */
    public String getIndex() {
        return indexKey;
    }

    /**
     * @param indexKey the indexKey to set
     */
    public void setIndex(String indexKey) {
        this.indexKey = indexKey;
        this.key = indexKey;
    }

    /**
     * @return the indexOid
     */
    public OID getOid() {
        return indexOid;
    }

    /**
     * @param indexOid the indexOid to set
     */
    public void setOid(OID indexOid) {
        this.indexOid = indexOid;
    }

}
