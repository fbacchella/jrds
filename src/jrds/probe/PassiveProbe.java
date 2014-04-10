package jrds.probe;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import jrds.JrdsSample;
import jrds.Probe;
import jrds.starter.Listener;
import jrds.starter.StarterNode;

import org.apache.log4j.Level;

public class PassiveProbe<KeyType> extends Probe<KeyType, Number> {

    private Listener<?, KeyType> listener = null;

    public void configure() {
        setName(jrds.Util.parseTemplate(getPd().getProbeName(), this));
        if(listener != null) {
            listener.register(this);                
        }
    }

    /**
     * Used by the listener starter to store value
     * @param rawValues
     */
    public void store(Date time, Map<KeyType, Number> rawValues) {
        JrdsSample sample = newSample();
        sample.setTime(time);
        injectSample(sample, rawValues);
        storeSample(sample);
    }

    /* (non-Javadoc)
     * @see jrds.Probe#collect()
     */
    @Override
    public void collect() {
    }

    @Override
    public Map<KeyType, Number> getNewSampleValues() {
        return Collections.emptyMap();
    }

    @Override
    public String getSourceType() {
        return listener.getSourceType();
    }

    /* (non-Javadoc)
     * @see jrds.starter.StarterNode#setParent(jrds.starter.StarterNode)
     */
    @Override
    public void setParent(StarterNode parent) {
        super.setParent(parent);
        if(listener == null) {
            String listenerClassName = this.getPd().getSpecific("listener");
            try {
                @SuppressWarnings("unchecked")
                Class<Listener<?, KeyType>> listenerClass = (Class<Listener<?, KeyType>>) this.getClass().getClassLoader().loadClass(listenerClassName);
                listener = parent.find(listenerClass);
            } catch (ClassNotFoundException e) {
                log(Level.ERROR, e, "Can't find listener class: %s", e.getMessage());
            }            
        }
    }

    /**
     * @return the listener
     */
    public Listener<?, KeyType> getListener() {
        return listener;
    }

    /**
     * @param l the listener to set
     */
    @SuppressWarnings("unchecked")
    public void setListener(Listener<?, ?> l) {
        this.listener = (Listener<?, KeyType>) l;
    }

}
