package jrds.configuration;

import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.w3c.dom.Node;

public enum ConfigType {
    FILTER {
        public boolean memberof(Node d) {
            return matchDocElement(d, "filter");
        }
        public String getName(JrdsDocument d) {
            return getNameByElement(d);
        }
    },
    HOSTS {
        public boolean memberof(Node d) {
            return matchDocElement(d, "host");
        }
        public String getName(JrdsDocument d) {
            return getNameByAttribute(d);
        }
    },
    SUM {
        public boolean memberof(Node d) {
            return matchDocElement(d, "sum");
        }
        public String getName(JrdsDocument d) {
            return getNameByAttribute(d);
        }
    },
    TAB {
        public boolean memberof(Node d) {
            return matchDocElement(d, "tab");
        }
        public String getName(JrdsDocument d) {
            return d.getRootElement().getAttribute("name");
        }
    },
    MACRODEF {
        public boolean memberof(Node d) {
            return matchDocElement(d, "macrodef");
        }
        public String getName(JrdsDocument d) {
            return d.getRootElement().getAttribute("name");
        }
    },
    GRAPH {
        public boolean memberof(Node d) {
            return matchDocElement(d, "graph");
        }
        public String getName(JrdsDocument d) {
            return getNameByElement(d);
        }
    },
    GRAPHDESC {
        public boolean memberof(Node d) {
            return matchDocElement(d, "graphdesc");
        }
        public String getName(JrdsDocument d) {
            return getNameByElement(d);
        }
    },
    PROBEDESC {
        public boolean memberof(Node d) {
            return matchDocElement(d, "probedesc");
        }
        public String getName(JrdsDocument d) {
            return getNameByElement(d);
        }
    };

    public abstract boolean memberof(Node d);
    public abstract String getName(JrdsDocument d);
    
    private static String getNameByAttribute(JrdsDocument d) {
        String name = d.getRootElement().getAttribute("name");
        if(name != null) {
            return name.trim();
        }
        return null;
    }

    private static String getNameByElement(JrdsDocument d) {
        JrdsElement nameElement = d.getRootElement().getElementbyName("name");
        if(nameElement != null) {
            return nameElement.getTextContent() != null ? nameElement.getTextContent().trim() : null;
        }
        return null;
    }

    private static boolean matchDocElement(Node d, String rootElement) {
        String root = d.getFirstChild().getNodeName();
        return rootElement.equals(root);
    }
}
