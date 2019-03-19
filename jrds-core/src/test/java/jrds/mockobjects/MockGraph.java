package jrds.mockobjects;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;

public class MockGraph extends GraphNode {

    public MockGraph() {
        super(new MokeProbe.SelfDescMokeProbe<String, Number>(), new GraphDesc());
    }

    public MockGraph(Probe<?, ?> theStore) {
        super(theStore, new GraphDesc());
    }

}
