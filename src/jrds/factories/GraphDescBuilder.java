package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import jrds.GraphDesc;
import jrds.factories.xml.JrdsNode;

public class GraphDescBuilder extends ObjectBuilder {

	@Override
	Object build(JrdsNode n) {
		try {
			return makeGraphDesc(n);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public GraphDesc makeGraphDesc(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		GraphDesc gd = new GraphDesc();

		n.setMethod(gd, CompiledXPath.get("/graphdesc/name"), "setName");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/graphName"), "setGraphName");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/verticalLabel"), "setVerticalLabel");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/graphTitle"), "setGraphTitle");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/upperLimit"), "setUpperLimit");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/lowerLimit"), "setLowerLimit");
		n.setMethod(gd, CompiledXPath.get("/graphdesc/unit/base"), "setUnitExponent");
		
		//Vertical label should never be empty
		if(gd.getVerticalLabel() == null)
			gd.setVerticalLabel("");

		if(n.checkPath(CompiledXPath.get("/graphdesc/unit/binary"))) {
			gd.setSiUnit(false);
		}
		if(n.checkPath(CompiledXPath.get("/graphdesc/unit/SI"))) {
			gd.setSiUnit(true);
		}

		for(Node addnode: n.iterate(CompiledXPath.get("/graphdesc/add"))) {
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
			String addName = elements.get("name"); //xpather.evaluate("//name", addnode).trim();
			String addgraphType = elements.get("graphType"); //xpather.evaluate("/graphType", addnode).trim();
			String addColor = elements.get("color"); //xpather.evaluate("/color", addnode).trim();
			String addLegend = elements.get("legend"); //xpather.evaluate("/legend", addnode).trim();
			String addrpn = elements.get("rpn"); //xpather.evaluate("/rpn", addnode).trim();
			String consFunc = elements.get("cf"); //xpather.evaluate("/cf", addnode).trim();
			String reversed = elements.get("reversed"); //xpather.evaluate("/reversed", addnode).trim();
			String host = elements.get("pathhost"); //xpather.evaluate("/path/host", addnode).trim();
			String probe = elements.get("pathprobe"); //xpather.evaluate("/path/probe", addnode).trim();
			String dsName = elements.get("pathname"); //xpather.evaluate("/path/name", addnode).trim();

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

		gd.setHostTree(n.doTreeList(CompiledXPath.get("/graphdesc/hosttree/*"),viewFilter));
		gd.setViewTree(n.doTreeList(CompiledXPath.get("/graphdesc/viewtree/*"),viewFilter));
		return gd;
	}

}
