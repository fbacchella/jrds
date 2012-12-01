package jrds.configuration;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.GraphDesc;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class GraphDescBuilder extends ConfigObjectBuilder<GraphDesc> {
    private final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    private final Graphics2D g2d = img.createGraphics();

    public GraphDescBuilder() {
        super(ConfigType.GRAPHDESC);
    }

    @Override
    GraphDesc build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeGraphDesc(n);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e, GraphDescBuilder.class.getName());
        }
    }
    public GraphDesc makeGraphDesc(JrdsDocument n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        JrdsElement subnode = n.getRootElement();

        GraphDesc gd;
        
        //Identify the optionnal custom GraphDesc class
        JrdsElement gdClass = subnode.getElementbyName("descClass");
        if(gdClass != null) {
            String className = gdClass.getTextContent().trim();
            if(! "".equals(className)) {
                @SuppressWarnings("unchecked")
                Class<GraphDesc> clazz = (Class<GraphDesc>) pm.extensionClassLoader.loadClass(className);
                Constructor<GraphDesc> c  = clazz .getConstructor();
                gd = c.newInstance();
            }
            else {
                throw new IllegalArgumentException("Empty descClass");
            }
        }
        else {
            gd = new GraphDesc();
        }

        setMethod(subnode.getElementbyName("name"), gd, "setName");
        setMethod(subnode.getElementbyName("graphName"), gd, "setGraphName");
        setMethod(subnode.getElementbyName("verticalLabel"), gd, "setVerticalLabel");
        setMethod(subnode.getElementbyName("graphTitle"), gd, "setGraphTitle");
        setMethod(subnode.getElementbyName("upperLimit"), gd, "setUpperLimit", Double.TYPE);
        setMethod(subnode.getElementbyName("lowerLimit"), gd, "setLowerLimit", Double.TYPE);
        setMethod(subnode.getElementbyName("height"), gd, "setHeight", Integer.TYPE);
        setMethod(subnode.getElementbyName("width"), gd, "setWidth", Integer.TYPE);

        doACL(gd, n, subnode);

        //Vertical label should never be empty
        if(gd.getVerticalLabel() == null)
            gd.setVerticalLabel("");

        gd.setWithLegend(subnode.getElementbyName("nolegend") == null);
        gd.setWithSummary(subnode.getElementbyName("novalues") == null);
        gd.setLogarithmic(subnode.getElementbyName("logarithmic") != null);

        JrdsElement unitElem = subnode.getElementbyName("unit");
        if(unitElem != null) {
            setMethod(unitElem.getElementbyName("base"), gd, "setUnitExponent");
            if(unitElem.getElementbyName("binary") != null)
                gd.setSiUnit(false);
            else if(unitElem.getElementbyName("SI") != null)
                gd.setSiUnit(true);
        }

        for(JrdsElement addnode: subnode.getChildElements()) {
            if(! "add".equals(addnode.getNodeName()) && ! "addpath".equals(addnode.getNodeName()) )
                continue;
            Map<String, String> elements = new HashMap<String, String>(10);
            boolean withPath = false;
            for(JrdsElement child: addnode.getChildElements()) {
                if("path".equals(child.getNodeName())) {
                    withPath = true;
                    for(JrdsElement hostchild: child.getChildElements()) {
                        String key = hostchild.getNodeName();
                        String value = hostchild.getTextContent();
                        if(value != null) {
                            value = value.trim();
                        }
                        elements.put("path" + key, value);
                    }
                }
                else {
                    String key = child.getNodeName();
                    String value = child.getTextContent();
                    if(value != null) {
                        value = value.trim();
                    }
                    elements.put(key, value);
                }
            }
            String addName = elements.get("name");
            String addgraphType = elements.get("graphType");
            String addColor = elements.get("color");
            String addLegend = elements.get("legend");
            String addrpn = elements.get("rpn");
            String consFunc = elements.get("cf");
            String reversed = elements.get("reversed");	
            String percentile = elements.get("percentile"); 
            if(elements.containsKey("percentile") && "".equals(percentile))
                percentile="95";
            String host = null;
            String probe = null;
            String dsName = null;
            if(withPath) {
                host = elements.get("pathhost");
                probe = elements.get("pathprobe");
                dsName = elements.get("pathdsName");
            }
            else 
                dsName = elements.get("dsName");

            gd.add(addName, addrpn, addgraphType, addColor, addLegend, consFunc, reversed, percentile, host, probe, dsName);
        }

        gd.setHostTree(enumerateTree(subnode.getElementbyName("hosttree")));
        gd.setViewTree(enumerateTree(subnode.getElementbyName("viewtree")));

        gd.initializeLimits(g2d);
        return gd;
    }

    List<Object> enumerateTree(JrdsElement roottree) {
        if(roottree == null)
            return Collections.emptyList();
        List<JrdsElement> path = roottree.getChildElements();
        if(path.isEmpty())
            return Collections.emptyList();

        List<Object> pathString = new ArrayList<Object>(path.size());
        for(JrdsElement te: path) {
            Object value;
            if("pathelement".equals(te.getNodeName()))
                value = GraphDesc.resolvPathElement(te.getTextContent());
            else 
                value = te.getTextContent();
            pathString.add(value);
        }
        return pathString;
    }

}
