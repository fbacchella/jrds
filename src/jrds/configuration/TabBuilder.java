package jrds.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.apache.log4j.Logger;

import jrds.Tab;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

public class TabBuilder extends ConfigObjectBuilder<Tab>  {
    static final private XPathExpression TABNAME = CompiledXPath.get("/tab/@name");
    static final private XPathExpression TABFILTER = CompiledXPath.get("/tab/filter");
    static final private XPathExpression TABGRAPH = CompiledXPath.get("/tab/graph|/tab/cgraph");
    static final private XPathExpression PATH = CompiledXPath.get("path");

    static final private Logger logger = Logger.getLogger(TabBuilder.class);

    public TabBuilder() {
        super(ConfigType.TAB);
    }

    @Override
    Tab build(JrdsNode n) {
        String name = n.evaluate(TABNAME);
        if(name == null || "".equals(name)) {
            logger.error("Invalid tab file");
            return null;
        }
        Tab tab;
        if(n.checkPath(TABFILTER)) {
            tab = new Tab.Filters(name);
            for(JrdsNode elemNode: n.iterate(TABFILTER)) {
                String elemName = elemNode.getTextContent();
                tab.add(elemName);
            }
        }
        else {
            tab = new Tab.DynamicTree(name);
            for(JrdsNode elemNode: n.iterate(TABGRAPH)) {
                String id = elemNode.attrMap().get("id");
                if("cgraph".equals(elemNode.getNodeName()))
                    id = "/" + id;
                List<String> path = new ArrayList<String>();
                for(JrdsNode pathNode: elemNode.iterate(PATH)) {
                    path.add(pathNode.getTextContent());
                }
                tab.add(id, path);
            }
        }
        return tab;
    }
}
