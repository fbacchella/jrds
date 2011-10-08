package jrds.factories;

import jrds.RdsHost;
import jrds.factories.xml.JrdsElement;
import jrds.starter.StarterNode;

public abstract class HostBuilderAgent {
    public abstract void buildStarters(JrdsElement fragment, StarterNode s, RdsHost host);
    public void buildStarters(JrdsElement fragment, RdsHost host) {
        buildStarters(fragment, host, host);
    }

}
