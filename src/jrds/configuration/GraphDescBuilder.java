package jrds.configuration;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.GraphDesc;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.AbstractJrdsNode;

import org.w3c.dom.Node;

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
        }
	}
	public GraphDesc makeGraphDesc(JrdsDocument n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		GraphDesc gd = new GraphDesc();

		JrdsElement subnode = n.getRootElement();
		setMethod(subnode.getElementbyName("name"), gd, "setName");
		setMethod(subnode.getElementbyName("graphName"), gd, "setGraphName");
		setMethod(subnode.getElementbyName("verticalLabel"), gd, "setVerticalLabel");
		setMethod(subnode.getElementbyName("graphTitle"), gd, "setGraphTitle");
		setMethod(subnode.getElementbyName("upperLimit"), gd, "setUpperLimit", Double.TYPE);
		setMethod(subnode.getElementbyName("lowerLimit"), gd, "setLowerLimit", Double.TYPE);
		setMethod(subnode, gd, "setUnitExponent", "unit", "base");
		setMethod(subnode.getElementbyName("height"), gd, "setHeight", Integer.TYPE);
		setMethod(subnode.getElementbyName("width"), gd, "setWidth", Integer.TYPE);

		doACL(gd, n, CompiledXPath.get("/graph/role"));

		//Vertical label should never be empty
		if(gd.getVerticalLabel() == null)
			gd.setVerticalLabel("");

        subnode.callIfExist(gd, CompiledXPath.get("nolegend"), "setWithLegend", Boolean.TYPE, false);
        subnode.callIfExist(gd, CompiledXPath.get("novalues"), "setWithSummary", Boolean.TYPE, false);
        subnode.callIfExist(gd, CompiledXPath.get("unit/binary"), "setSiUnit", Boolean.TYPE, false);
        subnode.callIfExist(gd, CompiledXPath.get("unit/SI"), "setSiUnit", Boolean.TYPE, true);
        subnode.callIfExist(gd, CompiledXPath.get("logarithmic"), "setLogarithmic", Boolean.TYPE, true);

		for(JrdsElement addnode: subnode.iterate(CompiledXPath.get("add|addpath"), JrdsElement.class)) {
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

		AbstractJrdsNode.FilterNode<Object> viewFilter = new AbstractJrdsNode.FilterNode<Object>() {
			@Override
			public Object filter(Node input) {
				Object value = input.getTextContent();
				if("pathelement".equals(input.getNodeName()))
					value = GraphDesc.resolvPathElement((String)value);
				return value;
			}
		};

		List<?> tree;

		tree = subnode.doTreeList(CompiledXPath.get("hosttree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setHostTree(tree);
		tree = subnode.doTreeList(CompiledXPath.get("viewtree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setViewTree(tree);
		tree = subnode.doTreeList(CompiledXPath.get("tree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setHostTree(tree);

		gd.initializeLimits(g2d);
		return gd;
	}

}
