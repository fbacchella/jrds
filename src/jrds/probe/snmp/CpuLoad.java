/*
 * Created on 23 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.CpuLoadGraph;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpVars;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CpuLoad extends RdsSnmpSimple {
    static final private Logger logger = JrdsLogger.getLogger(CpuLoad.class.getPackage().getName());
//	static final private OID LA_STRING = new OID(".1.3.6.1.4.1.2021.10.1.3");
//	static final private OID LA_FLOAT  = new OID(".1.3.6.1.4.1.2021.10.1.6");
//	static final private byte TAG1 = (byte) 0x9f;
//	static final private byte TAG_FLOAT = (byte) 0x78;
//	static final private byte TAG_DOUBLE = (byte) 0x79;

    static final private ProbeDesc pd = new ProbeDesc(3);
    static {
        pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.1")); //new OID(".1.3.6.1.4.1.2021.10.1.3.1"));      	//laLoad.1
        pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.2")); //new OID(".1.3.6.1.4.1.2021.10.1.3.2"));      	//laLoad.2
        pd.add("la15", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.3")); //new OID(".1.3.6.1.4.1.2021.10.1.3.3"));     	//laLoad.3
        //pd.add("la1float",  );	//laLoadFloat.1
        //pd.add("la5float",  );	//laLoadFloat.2
        //pd.add("la15float", );	//laLoadFloat.3
        pd.setRrdName("laverage");
        pd.setGraphClasses(new Class[] {CpuLoadGraph.class});
    }

    /**
     * simple constructor
     *
     * @param monitoredHost RdsHost
     */
    public CpuLoad(RdsHost monitoredHost)
    {
        super(monitoredHost, pd);
    }

    protected SnmpRequester getSnmpRequester() {
        return SnmpRequester.RAW;
    }

    /**
     * No need to play with the OID, so the snmpVars map can be returned has-is
     *
     * @param snmpVars SnmpVars
     * @return unomidified map
     */
    public Map filterValues(SnmpVars snmpVars) {
        return snmpVars;
    }

    /*	public Map prepareValues(SnmpVars snmpVars)
      {
       Map retValue = new HashMap(3);
       Map nameMap = pd.getOidNameMap();
       //double[] laValues = { -1 , -1 , -1 };
       for(Iterator i = snmpVars.keySet().iterator(); i.hasNext();) {
        OID oid = (OID) i.next();
        String name = (String) nameMap.get(oid);
        //if(name != null) {
         double value = -1;
         int[] arrayId = oid.getValue();
				//int index = arrayId[arrayId.length - 1] - 1;
				//if(oid.startsWith(CpuLoad.LA_FLOAT)) {

				try {
					byte[] bytesArray = (byte[]) snmpVars.get(oid);
					ByteBuffer bais = ByteBuffer.wrap(bytesArray);
					BERInputStream beris = new BERInputStream(bais);
					byte t1 = bais.get();
					byte t2 = bais.get();
					int l = BER.decodeLength(beris);
					if(t1 == TAG1 && t2 == TAG_FLOAT && l == 4)
						value = bais.getFloat();
					if(t1 == TAG1 && t2 == TAG_DOUBLE && l == 8)
						value = bais.getDouble();
					if(value >= 0)
						//laValues[index] = value;
						retValue.put(oid, new Double(value));
				} catch (IOException e) {
					logger.error(snmpVars.get(oid).toString(),e);
				}

				//} else {
				//	value = snmpVars.getValAsDouble(oid);
				//	if (laValues[index] < 0)
				//		laValues[index] = value;
				//}
			//}
			//oneSample.setValues(laValues);
		}
		return retValue;
	}
*/
}
