package jrds;

import jrds.objects.probe.ContainerProbe;
import jrds.webapp.WithACL;

public class AutonomousGraphNode extends GraphNode implements WithACL  {

    private String name;

    protected AutonomousGraphNode(String name) {
        this.name = name;
        setProbe(new ContainerProbe(name));
    }

    protected AutonomousGraphNode(String name, GraphDesc gd) {
        this.name = name;
        setGraphDesc(gd);
        setProbe(new ContainerProbe(name));
    }
    public AutonomousGraphNode(GraphDesc gd) {
        this.name = gd.getName();
        setGraphDesc(gd);
        setProbe(new ContainerProbe(name));
    }

    /* (non-Javadoc)
     * @see jrds.GraphNode#getQualifieName()
     */
    @Override
    public String getQualifieName() {
        return "/" + name;
    }

    public void configure(HostsList hl) {
        getProbe().setParent(hl);
    }
}
