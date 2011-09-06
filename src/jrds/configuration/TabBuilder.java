package jrds.configuration;

import java.util.ArrayList;
import java.util.List;

import jrds.Tab;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

public class TabBuilder extends ConfigObjectBuilder<Tab>  {

    public TabBuilder() {
        super(ConfigType.TAB);
    }

    @Override
	Tab build(JrdsNode n) {
        String name = n.evaluate(CompiledXPath.get("/tab/@name"));
        if(name != null && ! "".equals(name)) {
            Tab tab = new Tab(name);
            for(JrdsNode elemNode: n.iterate(CompiledXPath.get("/tab/filter"))) {
                String elemName = elemNode.getTextContent();
                tab.add(elemName);
            }
            for(JrdsNode elemNode: n.iterate(CompiledXPath.get("/tab/graph"))) {
                String id = elemNode.attrMap().get("id");
                List<String> path = new ArrayList<String>();
                for(JrdsNode pathNode: elemNode.iterate(CompiledXPath.get("path"))) {
                    path.add(pathNode.getTextContent());
                }
                tab.add(id, path);
            }
            return tab;
        }
        return null;
	}

}
