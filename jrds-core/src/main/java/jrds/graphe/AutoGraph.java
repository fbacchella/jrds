package jrds.graphe;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;
import jrds.PropertiesManager;

public class AutoGraph extends GraphNode {
    public enum Operation {
        SUM, MIN, MAX, AVERAGE
    }

    static final private Logger logger = LoggerFactory.getLogger(AutoGraph.class);
    static int i;
    Operation op;

    public AutoGraph(Probe<?, ?> theStore, Operation op) {
        super(theStore, new GraphDesc() {
            final String name = "autograph" + i++;

            /*
             * (non-Javadoc)
             * 
             * @see jrds.GraphDesc#getGraphName()
             */
            @Override
            public String getGraphName() {
                return name;
            }

        });
        GraphDesc gd = this.getGraphDesc();
        gd.setGraphName(theStore.getName());
        gd.setGraphTitle(theStore.getName());
        gd.setName(theStore.getName());
        gd.setTree(PropertiesManager.HOSTSTAB, List.<Object>of(GraphDesc.TITLE));
        logger.debug(this.getQualifiedName());
        this.op = op;

    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.GraphNode#getGraph()
     */
    @Override
    public Graph getGraph() {
        logger.debug("Wants to graph a AutoGraph");
        return null;
    }
}
