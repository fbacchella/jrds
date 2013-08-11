package jrds.probe;

import java.util.HashMap;
import java.util.Map;

import jrds.factories.ProbeBean;

@ProbeBean({"name", "device", "instance"})
public class KstatProbeIndexed extends KstatProbe implements IndexedProbe {
    private String index;
    private int instance = -1;
    private String device = null;
    private String name =  null;
    private String module =  null;

    public KstatProbeIndexed() {
        super();
    }

    private Boolean setup(int instance, Map<String, String> vars) {
        String module = jrds.Util.parseTemplate(getPd().getSpecific("module"), vars);
        String name = jrds.Util.parseTemplate(getPd().getSpecific("name"), vars);
        index = jrds.Util.parseTemplate(getPd().getSpecific("index"), vars);
        return setup(module, instance, name);
    }

    public Boolean configure(String name) {
        this.name = name;
        return configure();
    }

    public Boolean configure(String device, Integer instance) {
        this.device = device;
        this.instance = instance;
        return configure();
    }

    public Boolean configure(Integer instance) {
        this.instance = instance;
        return configure();
    }

    public Boolean configure() {
        Map<String, String> vars = new HashMap<String, String>(3);
        if(instance >= 0)
            vars.put("instance", Integer.toString(instance));
        if(device != null)
            vars.put("device", device);
        if(name != null)
            vars.put("name", name);

        return setup(instance, vars) && super.configure();
    }

    public String getIndexName() {
        return index;
    }

    /**
     * @return the instance
     */
    public Integer getInstance() {
        return instance;
    }

    /**
     * @param instance the instance to set
     */
    public void setInstance(Integer instance) {
        this.instance = instance;
    }

    /**
     * @return the device
     */
    public String getDevice() {
        return device;
    }

    /**
     * @param device the device to set
     */
    public void setDevice(String device) {
        this.device = device;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
    }

}
