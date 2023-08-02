package jrds.configuration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Tab;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class TabBuilder extends ConfigObjectBuilder<Tab> {

    static final private Logger logger = LoggerFactory.getLogger(TabBuilder.class);

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
        if(root.getElementbyName("filter") != null) {
            tab = new Tab.Filters(name);
            for(JrdsElement elemNode: root.getChildElementsByName("filter")) {
                String elemName = elemNode.getTextContent();
                tab.add(elemName);
            }
        } else {
            tab = new Tab.DynamicTree(name);
            for(JrdsElement elemNode: root.getChildElements()) {
                if(!"graph".equals(elemNode.getNodeName()) && !"cgraph".equals(elemNode.getNodeName()))
                    continue;
                String id = elemNode.getAttribute("id");
                if("cgraph".equals(elemNode.getNodeName()))
                    id = "/" + id;
                List<String> path = new ArrayList<>();
                for(JrdsElement pathNode: elemNode.getChildElementsByName("path")) {
                    path.add(pathNode.getTextContent());
                }
                tab.add(id, path);
            }
        }
        return tab;
    }
}
