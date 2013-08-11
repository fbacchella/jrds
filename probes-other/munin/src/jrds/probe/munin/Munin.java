/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munin;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.ProbeConnected;
import jrds.factories.ProbeMeta;

import org.apache.log4j.Level;

/**
 * @author Fabrice Bacchella
 *
 */
@ProbeMeta(discoverAgent = MuninDiscoverAgent.class)
public class Munin extends ProbeConnected<String, Number, MuninConnection> {

    public Munin() {
        super(MuninConnection.class.getName());
    }

    @Override
    public Map<String, Number> getNewSampleValuesConnected(MuninConnection cnx) {
        MuninConnection.SocketChannels muninSocket = cnx.getConnection();

        Map<String, Number> retValue = new HashMap<String, Number>();
        String fetchList = getPd().getSpecific("fetch");
        if(fetchList == null) {
            log(Level.ERROR, "no fetch list defined");
            return Collections.emptyMap();
        }

        try {
            for(String currentFetch: fetchList.split(",")) {
                muninSocket.out.println("fetch " + jrds.Util.parseTemplate(currentFetch.trim(), this));
                String lastLine;
                boolean dotFound = false;
                while(! dotFound && ( lastLine = muninSocket.in.readLine()) != null ) {
                    lastLine.replaceFirst("#.*", "");
                    if(".".equals(lastLine)) {
                        dotFound = true;
                    }
                    else {
                        String[] kvp = lastLine.split(" ");
                        if(kvp.length == 2) {
                            String name = kvp[0];
                            Number value = jrds.Util.parseStringNumber(kvp[1], Double.NaN);
                            if(name != null && value != null)
                                retValue.put(name.replace(".value", ""), value);
                        }
                    }
                }
                if(! dotFound) {
                    log(Level.WARN, "Munin connection finished early");
                }
            }
        }
        catch (IOException e) {
            log(Level.ERROR, e, "Munin communication error: %s", e);
            return Collections.emptyMap();
        }
        return retValue;
    }

    @Override
    public String getSourceType() {
        return "Munin";
    }

}
