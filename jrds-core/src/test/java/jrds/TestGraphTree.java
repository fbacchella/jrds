package jrds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import jrds.mockobjects.MockGraph;

public class TestGraphTree {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }
    
    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.GraphTree");
    }


    private List<String> doList(String... pathelems) {
        return new ArrayList<String>(Arrays.asList(pathelems));
    }

    @Test
    public void test1() {
        GraphTree gt1 = GraphTree.makeGraph("root");

        GraphNode gn = new MockGraph();
        gt1.addGraphByPath(doList("a", "b", gn.getName()), gn);

        Assert.assertNotNull("Graph node not found", gt1.getByPath("root", "a", "b"));
    }

    @Test
    public void test2() {
        GraphTree gt1 = GraphTree.makeGraph("root");

        gt1.addPath("a", "b");

        Assert.assertNotNull("Graph node not found", gt1.getByPath("root", "a", "b"));
    }

}
