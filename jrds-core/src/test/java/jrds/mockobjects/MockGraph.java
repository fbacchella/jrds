package jrds.mockobjects;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;

public class MockGraph extends GraphNode {

    public MockGraph() {
        super(new MokeProbe.SelfDescMokeProbe<String, Number>(), getGd());
    }

    private static GraphDesc getGd() {
        GraphDesc gd = new GraphDesc();
        gd.setGraphName("graphname");
        return gd;
    }

    public MockGraph(Probe<?, ?> theStore) {
        super(theStore, new GraphDesc());
    }

}
