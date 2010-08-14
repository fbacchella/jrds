package jrds.mockobjects;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;

public class MockGraph extends GraphNode {

	public MockGraph(Probe<?, ?> theStore) {
		super(theStore, new GraphDesc());
	}

	/* (non-Javadoc)
	 * @see jrds.GraphNode#getName()
	 */
	@Override
	public String getName() {
		return "MockGraph";
	}

}
