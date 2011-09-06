package jrds.configuration;

import javax.xml.xpath.XPathExpression;

import jrds.factories.xml.CompiledXPath;

import org.w3c.dom.Node;

public enum ConfigType {
    FILTER {
        final XPathExpression xpath = CompiledXPath.get("/filter/name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "filter");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    HOSTS {
        final XPathExpression xpath = CompiledXPath.get("/host/@name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "host");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    SUM {
        final XPathExpression xpath = CompiledXPath.get("/sum/@name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "sum");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    TAB {
        final XPathExpression xpath = CompiledXPath.get("/tab/@name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "tab");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    MACRODEF {
        final XPathExpression xpath = CompiledXPath.get("/macrodef/@name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "macrodef");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    GRAPH {
        final XPathExpression xpath = CompiledXPath.get("/graph/name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "graph");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    GRAPHDESC {
        final XPathExpression xpath = CompiledXPath.get("/graphdesc/name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "graphdesc");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    },
    PROBEDESC {
        final XPathExpression xpath = CompiledXPath.get("/probedesc/name");
        public boolean memberof(Node d) {
            return matchDocElement(d, "probedesc");
        }
        public XPathExpression getNameXpath() {
            return xpath;
        }
    };

    public abstract boolean memberof(Node d);
    public abstract XPathExpression getNameXpath();

    private static boolean matchDocElement(Node d, String rootElement) {
        String root = d.getFirstChild().getNodeName();
        return rootElement.equals(root);
    }
}
