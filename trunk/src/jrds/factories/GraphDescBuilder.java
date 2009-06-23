package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import jrds.GraphDesc;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

public class GraphDescBuilder extends ObjectBuilder {

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
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
		}
	}
	public GraphDesc makeGraphDesc(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		GraphDesc gd = new GraphDesc();
		
		JrdsNode subnode = n.getChild(CompiledXPath.get("/graphdesc | /graph"));

		subnode.setMethod(gd, CompiledXPath.get("name"), "setName");
		subnode.setMethod(gd, CompiledXPath.get("graphName"), "setGraphName");
		subnode.setMethod(gd, CompiledXPath.get("verticalLabel"), "setVerticalLabel");
		subnode.setMethod(gd, CompiledXPath.get("graphTitle"), "setGraphTitle");
		subnode.setMethod(gd, CompiledXPath.get("upperLimit"), "setUpperLimit");
		subnode.setMethod(gd, CompiledXPath.get("lowerLimit"), "setLowerLimit");
		subnode.setMethod(gd, CompiledXPath.get("unit/base"), "setUnitExponent");
		
		//Vertical label should never be empty
		if(gd.getVerticalLabel() == null)
			gd.setVerticalLabel("");

		if(subnode.checkPath(CompiledXPath.get("unit/binary"))) {
			gd.setSiUnit(false);
		}
		if(subnode.checkPath(CompiledXPath.get("unit/SI"))) {
			gd.setSiUnit(true);
		}

		for(Node addnode: subnode.iterate(CompiledXPath.get("add"))) {
			Map<String, String> elements = new HashMap<String, String>(10);
			for(JrdsNode child: new NodeListIterator(addnode.getChildNodes())) {
				if("path".equals(child.getNodeName())) {
					for(JrdsNode hostchild: new NodeListIterator(child.getChildNodes())) {
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
			String host = elements.get("pathhost");
			String probe = elements.get("pathprobe");
			String dsName = elements.get("pathname");

			gd.add(addName, addrpn, addgraphType, addColor, addLegend, consFunc, reversed, host, probe, dsName);
		}

		JrdsNode.FilterNode viewFilter = new JrdsNode.FilterNode() {
			@Override
			public Object filter(Node input) {
				Object value = input.getTextContent();
				if("pathelement".equals(input.getNodeName()))
					value = GraphDesc.resolvPathElement((String)value);
				return value;
			}
		};

		gd.setHostTree(subnode.doTreeList(CompiledXPath.get("hosttree/*"),viewFilter));
		gd.setViewTree(subnode.doTreeList(CompiledXPath.get("viewtree/*"),viewFilter));
		return gd;
	}

}
