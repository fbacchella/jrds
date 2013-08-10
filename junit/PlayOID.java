import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.OID;


public class PlayOID {

    @Test
    public void findMap() {
        Map<OID, Number> map = new HashMap<OID, Number>();

        OID key = new OID("1.1");
        map.put(key, 1);
        key.removeLast();
        Assert.assertTrue(new OID("1").equals(key));
        for(OID k: map.keySet()) {
            Assert.assertTrue(map.containsKey(k));
            Assert.assertEquals(map.get(k), 1);			
        }
    }

    @Test
    public void SubIndex() {
        OID key = new OID("1.2.3.4.5");
        key.set(3, 100);
        System.out.println(key);
        System.out.println(key.toSubIndex(false));
    }
}
