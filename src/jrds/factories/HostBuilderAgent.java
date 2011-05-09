package jrds.factories;

import jrds.RdsHost;
import jrds.factories.xml.JrdsNode;
import jrds.starter.StarterNode;

public abstract class HostBuilderAgent {
    public abstract void buildStarters(JrdsNode fragment, StarterNode s, RdsHost host);
    public void buildStarters(JrdsNode fragment, RdsHost host) {
        buildStarters(fragment, host, host);
    }

}
