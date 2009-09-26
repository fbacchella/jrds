package jrds.factories;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.graph.RrdGraphConstants;
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

		JrdsNode subnode = n.getChild(CompiledXPath.get("(/graphdesc|/graph)"));

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

		for(Node addnode: subnode.iterate(CompiledXPath.get("add|addpath"))) {
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

		List<Object> tree;

		tree = subnode.doTreeList(CompiledXPath.get("hosttree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setHostTree(tree);
		tree = subnode.doTreeList(CompiledXPath.get("viewtree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setViewTree(tree);
		tree = subnode.doTreeList(CompiledXPath.get("tree/*"),viewFilter);
		if(tree != null && ! tree.isEmpty())
			gd.setHostTree(tree);

		initializeLimits(gd);
		return gd;
	}
	
	private class ImageParameters {
		int xsize;
		int ysize;
		int unitslength;
		int xorigin;
		int yorigin;
		
		int xgif, ygif;
	}
	
	private final double LEGEND_LEADING_SMALL = 0.7; // chars
	private final int PADDING_LEFT = 10; // pix
	private final int PADDING_TOP = 12; // pix
	private final int PADDING_TITLE = 6; // pix
	private final int PADDING_RIGHT = 16; // pix
	private final int PADDING_PLOT = 2; //chars
	private final int PADDING_BOTTOM = 6; //pix

	private final int DEFAULT_UNITS_LENGTH = 9;

	private static final String DUMMY_TEXT = "Dummy";
	private final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	private final Graphics2D gd = img.createGraphics();
	private final Font smallFont = RrdGraphConstants.DEFAULT_SMALL_FONT; // ok
	private final Font largeFont = RrdGraphConstants.DEFAULT_LARGE_FONT; // ok

	private double getFontHeight(Font font) {
		LineMetrics lm = font.getLineMetrics(DUMMY_TEXT, gd.getFontRenderContext());
		return lm.getAscent() + lm.getDescent();
	}

	private double getSmallFontHeight() {
		return getFontHeight(smallFont);
	}

	private double getLargeFontHeight() {
		return getFontHeight(largeFont);
	}

	private double getStringWidth(String text, Font font) {
		return font.getStringBounds(text, 0, text.length(), gd.getFontRenderContext()).getBounds().getWidth();
	}
	
	private double getSmallFontCharWidth() {
		return getStringWidth("a", smallFont);
	}

	private double getSmallLeading() {
		return getSmallFontHeight() * LEGEND_LEADING_SMALL;
	}

	private void initializeLimits(GraphDesc gd) {
		ImageParameters im = new ImageParameters();

		im.xsize = gd.getWidth();
		im.ysize = gd.getHeight();
		im.unitslength = DEFAULT_UNITS_LENGTH;
		//gdef.onlyGraph
		if (false) {
			if (im.ysize > 64) {
				throw new IllegalArgumentException("Cannot create graph only, height too big: " + im.ysize);
			}
			im.xorigin = 0;
		}
		else {
			im.xorigin = (int) (PADDING_LEFT + im.unitslength * getSmallFontCharWidth());
		}
		//gdef.verticalLabel != null
		if (true) {
			im.xorigin += getSmallFontHeight();
		}
		//gdef.onlyGraph
		if (false) {
			im.yorigin = im.ysize;
		}
		else {
			im.yorigin = PADDING_TOP + im.ysize;
		}
		//gdef.title != null
		if (true) {
			im.yorigin += getLargeFontHeight() + PADDING_TITLE;
		}
		//gdef.onlyGraph
		if (false) {
			im.xgif = im.xsize;
			im.ygif = im.yorigin;
		}
		else {
			im.xgif = PADDING_RIGHT + im.xsize + im.xorigin;
			im.ygif = im.yorigin + (int) (PADDING_PLOT * getSmallFontHeight());
		}
		im.ygif += ( (int) getSmallLeading() + 5 ) * ( gd.getLegendLines() + 5);
		im.ygif += PADDING_BOTTOM;
		gd.setDimension(im.ygif, im.xgif);
	}

}
