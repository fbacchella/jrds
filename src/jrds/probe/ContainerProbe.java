package jrds.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.HostInfo;
import jrds.JrdsSample;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.store.EmptyExtractor;
import jrds.store.Extractor;
import jrds.store.Store;

import org.apache.log4j.Level;

public class ContainerProbe extends Probe<Object, Number> {
    static private final HostInfo ContainerHost = new HostInfo("ContainerHost");

    static final ProbeDesc pd = new ProbeDesc(0) {
        @Override
        public String getName() {
            return "ContainerProbeDesc";
        }
        @Override
        public Class<? extends Probe<?,?>> getProbeClass() {
            return ContainerProbe.class;
        }
        public String getProbeName() {
            return "ContainerProbeDesc";
        }	
    };

    public ContainerProbe(String name) {
        super(pd);
        setName(name);
        this.monitoredHost = ContainerHost;
    }

    public ContainerProbe(String name, HostInfo monitoredHost) {
        super(pd);
        setName(name);
        this.monitoredHost = monitoredHost;
    }

    //An array list is needed, the introspection is picky
    public void configure(String name, ArrayList<String> graphList) {
        log(Level.DEBUG, "new container: %s", name);
        setName(name);
    }

    @Override
    public Date getLastUpdate() {
        return new Date();
    }

    @Override
    public String getSourceType() {
        return "container";
    }

    public Map<Object, Number> getNewSampleValues() {
        return java.util.Collections.emptyMap();
    }

    public boolean checkStore() {
        return true;
    }

    /**
     * This method does nothing for a virtual probe
     * @see jrds.Probe#collect()
     */
    public void collect() {
    }

    /* (non-Javadoc)
     * @see jrds.Probe#getQualifiedName()
     */
    @Override
    public String getQualifiedName() {
        return "/"  + getName();
    }

    /* (non-Javadoc)
     * @see jrds.Probe#getMainStore()
     */
    @Override
    public Store getMainStore() {
        return new Store(){

            @Override
            public void commit(JrdsSample sample) {
            }

            @Override
            public Map<String, Number> getLastValues() {
                return Collections.emptyMap();
            }

            @Override
            public boolean checkStoreFile(ArchivesSet archives) {
                return true;
            }

            @Override
            public Date getLastUpdate() {
                return new Date();
            }

            @Override
            public Object getStoreObject() {
                return null;
            }

            @Override
            public void closeStoreObject(Object object) {                
            }

            @Override
            public Extractor getExtractor() {
                return new EmptyExtractor();
            }

            @Override
            public String getPath() {
                return "";
            }

        };
    }

}
