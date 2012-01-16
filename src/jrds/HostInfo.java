package jrds;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class HostInfo {
    private String name = null;
    private String dnsName = null;
    private Set<String> tags = null;
    private File hostdir = null;
    private boolean hidden = false;

    public HostInfo(String name) {
        super();
        this.name = name;
    }

    public HostInfo(String name, String dnsName) {
        super();
        this.name = name;
        this.dnsName = dnsName;
    }

    /**
     * @param hostdir the hostdir to set
     */
    public void setHostDir(File hostdir) {
        this.hostdir = hostdir;
    }

    public File getHostDir() {
        return hostdir;
    }

    public void addTag(String tag) {
        if(tags == null)
            tags = new HashSet<String>();
        tags.add(tag);
    }

    public Set<String> getTags() {
        Set<String> temptags = tags;
        if(tags == null)
            temptags = new HashSet<String>();
        return temptags;
    }

    /**
     * @return the dnsName
     */
    public String getDnsName() {
        if(dnsName != null)
            return dnsName;
        else
            return name;
    }

    /**
     * @param dnsName the dnsName to set
     */
    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    /**
     * Is the host to be shown in the host list ?
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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

}
