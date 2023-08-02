package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.graphe.Sum;

public class SumBuilder extends ConfigObjectBuilder<Sum> {

    public SumBuilder() {
        super(ConfigType.SUM);
    }

    @Override
    Sum build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeSum(n);
        } catch (SecurityException | IllegalArgumentException e) {
            throw new InvocationTargetException(e, FilterBuilder.class.getName());
        }
    }

    public Sum makeSum(JrdsDocument n) {
        JrdsElement root = n.getRootElement();
        String name = root.getAttribute("name");
        if(name != null && !"".equals(name)) {
            ArrayList<String> elements = new ArrayList<>();
            for(JrdsElement elemNode: root.getChildElementsByName("element")) {
                String elemName = elemNode.getAttribute("name");
                elements.add(elemName);
            }
            Sum sp = new Sum(name, elements);
            doACL(sp, n, root);
            return sp;
        }
        return null;
    }
}
