package jrds.store;

import java.util.Properties;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Util;

public class GraphiteStoreFactory extends AbstractStoreFactory<GraphiteStore> {
    GraphiteConnection cnx;

    @Override
    public GraphiteStore create(Probe<?, ?> p) {
        return new GraphiteStore(p, cnx);
    }

    /* (non-Javadoc)
     * @see jrds.store.AbstractStoreFactory#configureStore(jrds.PropertiesManager, java.util.Properties)
     */
    @Override
    public void configureStore(PropertiesManager pm, Properties props) {
        super.configureStore(pm, props);
        String host = props.getProperty("host", "localhost");
        String portStr = props.getProperty("port", "2003");
        int port = Util.parseStringNumber(portStr, new Integer(2003));
        String prefix = props.getProperty("prefix", "");
        
        cnx = new GraphiteConnection(host, port, prefix);

    }

}
