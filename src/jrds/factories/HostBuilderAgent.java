package jrds.factories;

import jrds.HostInfo;
import jrds.factories.xml.JrdsElement;
import jrds.starter.StarterNode;

public abstract class HostBuilderAgent {
    public abstract void buildStarters(JrdsElement fragment, StarterNode s, HostInfo host);
}
