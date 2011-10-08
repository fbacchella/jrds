package jrds.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import jrds.Tab;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.apache.log4j.Logger;

public class TabBuilder extends ConfigObjectBuilder<Tab>  {
    static final private XPathExpression TABFILTER = CompiledXPath.get("/tab/filter");
    static final private XPathExpression TABGRAPH = CompiledXPath.get("/tab/graph|/tab/cgraph");

    static final private Logger logger = Logger.getLogger(TabBuilder.class);

    public TabBuilder() {
        super(ConfigType.TAB);
    }

    @Override
    Tab build(JrdsDocument n) {
        JrdsElement root = n.getRootElement();
        String name = root.getAttribute("name");
        if(name == null || "".equals(name)) {
            logger.error("Invalid tab file");
            return null;
        }
        Tab tab;
        if(n.checkPath(TABFILTER)) {
            tab = new Tab.Filters(name);
            for(JrdsElement elemNode: root.getChildElementsByName("filter")) {
                String elemName = elemNode.getTextContent();
                tab.add(elemName);
            }
        }
        else {
            tab = new Tab.DynamicTree(name);
            for(JrdsElement elemNode: n.iterate(TABGRAPH, JrdsElement.class)) {
                String id = elemNode.getAttribute("id");
                if("cgraph".equals(elemNode.getNodeName()))
                    id = "/" + id;
                List<String> path = new ArrayList<String>();
                for(JrdsElement pathNode: elemNode.getChildElementsByName("path")) {
                    path.add(pathNode.getTextContent());
                }
                tab.add(id, path);
            }
        }
        return tab;
    }
}
