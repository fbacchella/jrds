package jrds.webapp;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class DiscoverAgent {
    protected enum DojoType { ToggleButton {
        @Override
        public void doNode(Element parent, FieldInfo fi) {
            Element button = parent.getOwnerDocument().createElement("button");
            button.setAttribute("id", fi.id);
            button.setAttribute("name", fi.id);
            button.setAttribute("iconClass", "dijitCheckBoxIcon");
            button.setAttribute("dojoType", "dijit.form.ToggleButton");
            button.setTextContent(fi.label);
            parent.appendChild(button);
        }
    }, TextBox {
        @Override
        public void doNode(Element parent, FieldInfo fi) {
            Element label = parent.getOwnerDocument().createElement("label");
            label.setAttribute("for", fi.id);
            label.setTextContent(fi.label);
            parent.appendChild(label);

            Element input = parent.getOwnerDocument().createElement("input");
            input.setAttribute("dojoType", "dijit.form.TextBox");
            input.setAttribute("trim", "true");
            input.setAttribute("id", fi.id);
            input.setAttribute("name", fi.id);
            parent.appendChild(input);
        }
    };
    public abstract void  doNode(Element parent, FieldInfo fi);
    };
    public static final class FieldInfo {
        public String id;
        public String label;
        public DojoType dojoType;
    };

    Logger namedLogger = null;

    protected DiscoverAgent(String name) {
        namedLogger = Logger.getLogger("jrds.DiscoverAgent." + name);
    }

    public abstract void discover(String hostname, Element hostElement, Map<String, JrdsNode> probdescs, HttpServletRequest request);

    public abstract List<FieldInfo> getFields();

    public void doHtmlDiscoverFields(Document document) {
        try {
            List<FieldInfo> fields = getFields();
            log(Level.DEBUG, "Fields: %s", fields);

            Element localRoot = 
                (Element) document.createElement("div");
            document.getDocumentElement().appendChild(localRoot);
            for(FieldInfo f: fields) {
                f.dojoType.doNode(localRoot, f);
            }
        } catch (DOMException e) {
            log(Level.ERROR, e, "Invalid DOM: %s", e);
        }
    }

    /**
     * This method add a probe to the current host document
     * @param hostDom the host document
     * @param probe the Name of the probe
     * @param argsTypes a list of type for the argument
     * @param argsValues a list of value for the argument
     * @return the generated element for this probe
     */
    public Element addProbe(Element hostElem, String probe, List<String> argsTypes, List<String> argsValues) {
        Document hostDoc = hostElem.getOwnerDocument();
        Element rrdElem = hostDoc.createElement("probe");
        rrdElem.setAttribute("type", probe);
        hostElem.appendChild(rrdElem);
        addArgsList(hostDoc, rrdElem, argsTypes, argsValues);
        return rrdElem;
    }
    
    protected Element addConnexion(Element hostElem, String connexionClass, List<String> argsTypes, List<String> argsValues) {
        Document hostDoc = hostElem.getOwnerDocument();
        Element cnxElement = hostDoc.createElement("connection");
        cnxElement.setAttribute("type", connexionClass);
        hostDoc.getDocumentElement().appendChild(cnxElement);
        addArgsList(hostDoc, cnxElement, argsTypes, argsValues);
        return cnxElement;
    }
    
    private void addArgsList(Document hostDoc, Element e, List<String> argsTypes, List<String> argsValues) {
        if(argsTypes != null && argsTypes.size() > 0 && argsTypes.size() == argsValues.size()) {
            for(int i=0; i < argsTypes.size(); i++) {
                Element arg = hostDoc.createElement("arg");
                arg.setAttribute("type", argsTypes.get(i));
                arg.setAttribute("value", argsValues.get(i));
                e.appendChild(arg);
            }
        }
    }

    protected void log(Level l, Throwable e, String format, Object... elements) {
        jrds.Util.log(this, namedLogger, l, e, format, elements);
    }

    protected void log(Level l, String format, Object... elements) {
        jrds.Util.log(this, namedLogger, l, null, format, elements);
    }

}
