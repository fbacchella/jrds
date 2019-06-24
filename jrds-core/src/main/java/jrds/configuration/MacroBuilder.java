package jrds.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

import jrds.Macro;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class MacroBuilder extends ConfigObjectBuilder<Macro> {
    static final private Logger logger = LoggerFactory.getLogger(MacroBuilder.class);

    public MacroBuilder() {
        super(ConfigType.MACRODEF);
    }

    @Override
    Macro build(JrdsDocument n) {
        return makeMacro(n);
    }

    public Macro makeMacro(JrdsDocument n) {
        Macro m = new Macro();
        String name = n.getRootElement().getAttribute("name");
        logger.debug("Building macro {}", name);
        if(name != null && !"".equals(name)) {
            m.setName(name);
        }

        JrdsElement macrodefnode = n.getRootElement();
        DocumentFragment df = n.createDocumentFragment();
        df.appendChild(macrodefnode.getParentNode().removeChild(macrodefnode.getParent()));
        m.setDf(df);

        return m;
    }

}
