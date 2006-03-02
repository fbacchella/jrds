//----------------------------------------------------------------------------
//$Id$

package jrds;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.probe.jdbc.JdbcProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdException;
import org.jrobin.graph.Plottable;
import org.jrobin.graph.RrdGraphDef;

/**
 * A classed used to store the static description of a graph
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public final class GraphDesc
implements Cloneable {
	static final private Logger logger = Logger.getLogger(GraphDesc.class);
	
	public static final class ConsFunc {
		private String id;
		private ConsFunc(String id) {
			this.id = id;
		}
		
		public String toString() {
			return id;
		}
		
		static public final ConsFunc AVERAGE = new ConsFunc("AVERAGE");
		static public final ConsFunc MIN = new ConsFunc("MIN");
		static public final ConsFunc MAX = new ConsFunc("MAX");
		static public final ConsFunc LAST = new ConsFunc("LAST");
	};
	
	static public final ConsFunc AVERAGE = ConsFunc.AVERAGE;
	static public final ConsFunc MIN = ConsFunc.MIN;
	static public final ConsFunc MAX = ConsFunc.MAX;
	static public final ConsFunc LAST = ConsFunc.LAST;
	static public final ConsFunc DEFAULTCF = AVERAGE;
	
	/**
	 * Find the value of field in the class, the field is is upper case
	 *
	 * @param clazz Class the class to search in
	 * @param name String the looked for field
	 * @return Object the value of the field or null
	 */
	private static final Object resolv(Class clazz, String name) {
		Object gt = null;
		Field gtfield;
		try {
			gtfield = clazz.getField(name.toUpperCase());
			if (gtfield != null)
				gt = gtfield.get(clazz);
		}
		catch (Exception e) {
			logger.error(name + " is a invalid constant name for type " + clazz);
		}
		return gt;
	}
	
	public static final PathElement resolvPathElement(String name) {
		return (PathElement) resolv(PathElement.class, name);
	}
	
	public interface GraphType {
		public abstract void draw(RrdGraphDef rgd, String sn, Color color,
				String legend) throws
				RrdException;
		
		public static final GraphType NONE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color,
					String legend) {};
		};
		
		public static final GraphType LINE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color,
					String legend) throws RrdException {
				rgd.line(sn, color, legend);
			};
		};
		static public final GraphType AREA = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color,
					String legend) throws RrdException {
				rgd.area(sn, color, legend);
			};
		};
		static public final GraphType STACK = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color,
					String legend) throws RrdException {
				rgd.stack(sn, color, legend);
			};
		};
		static public final GraphType COMMENT = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color,
					String legend) throws RrdException {
				rgd.comment(legend);
			};
		};
	};
	
	static final public GraphType NONE = GraphType.NONE;
	static final public GraphType LINE = GraphType.LINE;
	static final public GraphType AREA = GraphType.AREA;
	static final public GraphType STACK = GraphType.STACK;
	static final public GraphType COMMENT = GraphType.COMMENT;
	
	private interface PathElement {
		public String resolve(RdsGraph graph);
		
		static public final PathElement HOST = new PathElement() {
			public String resolve(RdsGraph graph) {
				return graph.getProbe().getHost().getName();
			}
		};
		static public final PathElement TITLE = new PathElement() {
			public String resolve(RdsGraph graph) {
				return graph.getGraphTitle();
			}
		};
		static public final PathElement INDEX = new PathElement() {
			public String resolve(RdsGraph graph) {
				return ( (IndexedProbe) graph.getProbe()).getIndexName();
			}
		};
		static public final PathElement URL = new PathElement() {
			public String resolve(RdsGraph graph) {
				String url = "";
				Probe probe = graph.getProbe();
				if( probe instanceof UrlProbe) {
					url =((UrlProbe) probe).getUrlAsString();
				}
				return url;
			}
		};
		static public final PathElement JDBC = new PathElement() {
			public String resolve(RdsGraph graph) {
				return ( (JdbcProbe) graph.getProbe()).getJdbcurl();
			}
		};
		static public final PathElement DISK = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Disk";
			}
		};
		static public final PathElement NETWORK = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Network";
			}
		};
		static public final PathElement TCP = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "TCP";
			}
		};
		static public final PathElement SERVICES = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Services";
			}
		};
		static public final PathElement SYSTEM = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "System";
			}
		};
		static public final PathElement LOAD = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Load";
			}
		};
		static public final PathElement DISKACTIVITY = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Disk activity";
			}
		};
		static public final PathElement WEB = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Web";
			}
		};
		static public final PathElement INTERFACES = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Interfaces";
			}
		};
		static public final PathElement IP = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "IP";
			}
		};
		static public final PathElement MEMORY = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Memory";
			}
		};
		static public final PathElement DATABASE = new PathElement() {
			public String resolve(RdsGraph graph) {
				return "Databases";
			}
		};
		static public final PathElement DBISNTANCE = new PathElement() {
			public String resolve(RdsGraph graph) {
				JdbcProbe dbprobe = (JdbcProbe) graph.getProbe();
				return dbprobe.getJdbcInstanceUrl();
			}
		};
	}
	
	static final public PathElement HOST = PathElement.HOST;
	static final public PathElement SERVICES = PathElement.SERVICES;
	static final public PathElement NETWORK = PathElement.NETWORK;
	static final public PathElement IP = PathElement.IP;
	static final public PathElement TITLE = PathElement.TITLE;
	static final public PathElement INDEX = PathElement.INDEX;
	static final public PathElement URL = PathElement.URL;
	static final public PathElement JDBC = PathElement.JDBC;
	static final public PathElement WEB = PathElement.WEB;
	static final public PathElement SYSTEM = PathElement.SYSTEM;
	static final public PathElement DISK = PathElement.DISK;
	static final public PathElement DISKACTIVITY = PathElement.DISKACTIVITY;
	static final public PathElement MEMORY = PathElement.MEMORY;
	static final public PathElement TCP = PathElement.TCP;
	static final public PathElement LOAD = PathElement.LOAD;
	static final public PathElement INTERFACES = PathElement.INTERFACES;
	static final public PathElement DATABASE = PathElement.DATABASE;
	
	static final public Color[] colors = new Color[] {
			Color.BLUE,
			Color.GREEN,
			Color.RED,
			Color.CYAN,
			Color.BLACK,
			Color.ORANGE,
			Color.YELLOW,
			Color.PINK,
			Color.MAGENTA
	};
	
	static final private Map COLORMAP = new HashMap(colors.length);
	static {
		COLORMAP.put("BLUE", Color.BLUE);
		COLORMAP.put("GREEN", Color.GREEN);
		COLORMAP.put("RED", Color.RED);
		COLORMAP.put("CYAN", Color.CYAN);
		COLORMAP.put("BLACK", Color.BLACK);
		COLORMAP.put("ORANGE", Color.ORANGE);
		COLORMAP.put("YELLOW", Color.YELLOW);
		COLORMAP.put("PINK", Color.PINK);
		COLORMAP.put("MAGENTA", Color.MAGENTA);
		COLORMAP.put("WHITE", Color.WHITE);
		COLORMAP.put("DARK_GRAY", Color.DARK_GRAY);
		COLORMAP.put("GRAY", Color.GRAY);
		COLORMAP.put("LIGHT_GRAY", Color.LIGHT_GRAY);
		COLORMAP.put("PINK", Color.PINK);
	}
	
	static final public Color COLOR1 = colors[0];
	static final public Color COLOR2 = colors[1];
	static final public Color COLOR3 = colors[2];
	static final public Color COLOR4 = colors[3];
	static final public Color COLOR5 = colors[4];
	static final public Color COLOR6 = colors[5];
	static final public Color COLOR7 = colors[6];
	
	private Map dsMap;
	private int width = 578;
	private int height = 206;
	private double upperLimit = Double.NaN;
	private double lowerLimit = 0;
	private String verticalLabel = null;
	private int lastColor = 0;
	private List viewTree = new ArrayList();
	private List hostTree = new ArrayList();
	private String graphName;
	private String graphTitle ="{0} on {1}";
	
	private final class Dimension {
		public int width = 0;
		public int height = 0;
	};
	private Dimension dimension = new Dimension();
	
	static private final class DsDesc {
		public String name;
		public String dsName;
		public String rpn;
		public GraphType graphType;
		public Color color;
		public String legend;
		public ConsFunc cf;
		public DsDesc(String name, String dsName, String rpn,
				GraphType graphType, Color color, String legend,
				ConsFunc cf) {
			this.name = name;
			this.dsName = dsName;
			this.rpn = rpn;
			this.graphType = graphType;
			this.color = color;
			this.legend = legend;
			this.cf = cf;
		}
		public String toString() {
			return "DsDesc(" + name + "," + dsName + ",\"" + rpn + "\"," + graphType + "," + color + ",\"" + legend + "\"," + cf + ")";
		}
	}
	
	/**
	 * A constructor wich pre allocate the desired size
	 * @param size the estimated number of graph that will be created
	 */
	public GraphDesc(int size) {
		dsMap = new LinkedHashMap(size);
	}
	
	public GraphDesc() {
		dsMap = new LinkedHashMap();
	}
	
	/**
	 * add a graph element
	 *
	 * @param name String
	 * @param graphType GraphType
	 * @param color Color
	 */
	public void add(String name, GraphType graphType, Color color) {
		dsMap.put(name,
				new DsDesc(name, name, null, graphType, color, name,
						DEFAULTCF));
	}
	
	public void add(String name, GraphType graphType, Color color,
			String legend) {
		dsMap.put(name,
				new DsDesc(name, name, null, graphType, color, legend,
						DEFAULTCF));
	}
	
	public void add(String name, GraphType graphType, String legend) {
		dsMap.put(name,
				new DsDesc(name, name, null, graphType,
						colors[ (lastColor++) % colors.length], legend,
						DEFAULTCF));
	}
	
	public void add(String name, GraphType graphType) {
		dsMap.put(name,
				new DsDesc(name, name, null, graphType,
						colors[ (lastColor++) % colors.length], name,
						DEFAULTCF));
	}
	
	/**
	 * Used to add a lign in the legend
	 *
	 * @param graphType GraphType
	 * @param legend String
	 */
	public void add(GraphType graphType, String legend) {
		dsMap.put(legend, new DsDesc(null, null, null, graphType, null, legend, null));
	}
	
	public void add(String name, String rpn, GraphType graphType, Color color,
			String legend) {
		dsMap.put(name,
				new DsDesc(name, null, rpn, graphType, color, legend,
						DEFAULTCF));
	}
	
	public void add(String name, String rpn, GraphType graphType, Color color) {
		dsMap.put(name,
				new DsDesc(name, null, rpn, graphType, color, name,
						DEFAULTCF));
	}
	
	public void add(String name, String rpn, GraphType graphType, String legend) {
		dsMap.put(name,
				new DsDesc(name, null, rpn, graphType,
						colors[lastColor++ % colors.length], legend,
						DEFAULTCF));
	}
	
	/**
	 * Add a datastore that will not generate a graph
	 *
	 * @param name String
	 */
	public void add(String name) {
		dsMap.put(name,
				new DsDesc(name, name, null, NONE, null, null, DEFAULTCF));
	}
	
	public void add(String name, String rpn) {
		dsMap.put(name,
				new DsDesc(name, null, rpn, NONE, null, null, DEFAULTCF));
	}
	
	public void add(String name, String dsName, String rpn,
			GraphType graphType, Color color, String legend,
			ConsFunc cf) {
		dsMap.put(name,
				new DsDesc(name, dsName, rpn, graphType, color, legend, cf));
		
	}
	
	/**
	 * Add a plot, but only uses String as parameters, for the GraphFactory
	 * @param name Name of the plot
	 * @param dsName the datastore to use
	 * @param rpn The RPN, used instead of the datastore
	 * @param graphType
	 * @param color
	 * @param legend
	 * @param consFunc
	 */
	public void add(String name, String dsName, String rpn,
			String graphType, String color, String legend,
			String consFunc) {
		if (dsName == null && rpn == null)
			dsName = name;
		GraphType gt = (GraphType) resolv(GraphType.class, graphType);
		ConsFunc cf = DEFAULTCF;
		if (consFunc != null)
			cf = (ConsFunc) resolv(ConsFunc.class, consFunc);
		Color c = Color.WHITE;
		if (color != null) {
			c = (Color) COLORMAP.get(color.toUpperCase());
			if( c == null)
				c = Color.getColor(color);
			if (c == null) {
				logger.error("Cannot read color " + color);
				c = Color.white;
			}
		}
		else
			c = colors[lastColor++ % colors.length];
		if (legend == null)
			legend = name;
		dsMap.put(name,
				new DsDesc(name, dsName, rpn, gt, c, legend, cf));
		
	}
	
	/**
	 * return the RrdGraphDef for this graph, used the indicated probe
	 * any data can be overined of a provided map of Plottable
	 * @param probe
	 * @param ownData data used to overied probe's own values
	 * @return
	 * @throws IOException
	 * @throws RrdException
	 */
	public RrdGraphDef getGraphDef(Probe probe, Map ownData) throws IOException,
	RrdException {
		RrdGraphDef retValue = new RrdGraphDef();
		String rrdName = probe.getRrdName();
		for (Iterator i = dsMap.values().iterator(); i.hasNext(); ) {
			DsDesc ds = (DsDesc) i.next();
			if (ds.dsName == null && ds.rpn == null)
				ds.graphType.draw(retValue, ds.name, ds.color, ds.legend + "@l");
			else if (ds.rpn == null) {
				boolean exist = false; // Used to check it the data source one way or another
				//Does the datas existe in the provided values
				if(ownData != null && ownData.containsKey(ds.dsName)) {
					exist = true;
					retValue.datasource(ds.name, (Plottable) ownData.get(ds.dsName));
				}
				//Or they might be on the associated rrd
				else if(probe.dsExist(ds.dsName)) {
					exist = true;
					retValue.datasource(ds.name, rrdName, ds.dsName,
							ds.cf.toString());				
				}
				if (exist) {
					if(ds.graphType != null)
						ds.graphType.draw(retValue, ds.name, ds.color,
								ds.legend + "@l");
					else {
						logger.warn("graph type is null for " + ds.dsName + " on probe " + probe);
					}
				}
			}
			else {
				retValue.datasource(ds.name, ds.rpn);
				ds.graphType.draw(retValue, ds.name, ds.color, ds.legend + "@l");
			}
		}
		retValue.setShowLegend(true);
		retValue.setGridRange(lowerLimit, upperLimit, false);
		if (verticalLabel != null)
			retValue.setVerticalLabel(verticalLabel);
		return retValue;
	}
	
	/**
	 * return the RrdGraphDef for this graph, used the indicated probe
	 *
	 * @param probe Probe
	 * @return RrdGraphDef
	 * @throws IOException
	 * @throws RrdException
	 */
	public RrdGraphDef getGraphDef(Probe probe) throws IOException,
	RrdException {
		return getGraphDef(probe, null);
	}
	
	/**
	 * @return Returns the graphTitle.
	 */
	public String getGraphName() {
		return graphName;
	}
	
	/**
	 * @param graphTitle The graphTitle to set.
	 */
	public void setGraphName(String graphTitle) {
		this.graphName = graphTitle;
	}
	
	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * @param height The height to set.
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getRealHeight() {
		return dimension.height;
	}
	
	public void setRealHeight(int height) {
		dimension.height = height;
	}
	
	public int getRealWidth() {
		return dimension.width;
	}
	
	public void setRealWidth(int width) {
		dimension.width = width;
	}
	
	/**
	 * @return Returns the lowerLimit.
	 */
	public double getLowerLimit() {
		return lowerLimit;
	}
	
	/**
	 * @param lowerLimit The lowerLimit to set.
	 */
	public void setLowerLimit(double lowerLimit) {
		this.lowerLimit = lowerLimit;
	}
	
	/**
	 * @return Returns the upperLimit.
	 */
	public double getUpperLimit() {
		return upperLimit;
	}
	
	/**
	 * @param upperLimit The upperLimit to set.
	 */
	public void setUpperLimit(double upperLimit) {
		this.upperLimit = upperLimit;
	}
	
	/**
	 * @param upperLimit The upperLimit to set.
	 */
	public void setUpperLimit(String upperLimit) {
		this.upperLimit = Double.parseDouble(upperLimit);
	}
	
	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * @param width The width to set.
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return Returns the verticalLabel.
	 */
	public String getVerticalLabel() {
		return verticalLabel;
	}
	
	/**
	 * @param verticalLabel The verticalLabel to set.
	 */
	public void setVerticalLabel(String verticalLabel) {
		this.verticalLabel = verticalLabel;
	}
	
	public void colorsReset() {
		lastColor = 0;
	}

	/**
	 * @return Returns the viewTree.
	 */
	public LinkedList getViewTree(RdsGraph graph) {
		LinkedList tree = new LinkedList();
		for (Iterator i = viewTree.iterator(); i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof String)
				tree.add(o);
			else if (o instanceof PathElement)
				tree.add( ( (PathElement) o).resolve(graph));
		}
		return tree;
	}
	
	/**
	 * @param viewTree The viewTree to set.
	 */
	public void setViewTree(List viewTree) {
		this.viewTree = viewTree;
	}
	
	/**
	 * @param viewTree The viewTree to set.
	 */
	public void setViewTree(Object[] viewTree) {
		this.viewTree = Arrays.asList(viewTree);
	}
	
	/**
	 * @return Returns the hostTree.
	 */
	public LinkedList getHostTree(RdsGraph graph) {
		LinkedList tree = new LinkedList();
		for (Iterator i = hostTree.iterator(); i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof String)
				tree.add(o);
			else if (o instanceof PathElement)
				tree.add( ( (PathElement) o).resolve(graph));
		}
		return tree;
	}
	
	/**
	 * @param hostTree The hostTree to set.
	 */
	public void setHostTree(List hostTree) {
		this.hostTree = hostTree;
	}
	
	/**
	 * @param hostTree The hostTree to set.
	 */
	public void setHostTree(Object[] hostTree) {
		this.hostTree = Arrays.asList(hostTree);
	}

	/**
	 * @return Returns the graphTitle.
	 */
	public String getGraphTitle() {
		return graphTitle;
	}
	/**
	 * @param graphTitle The graphTitle to set.
	 */
	public void setGraphTitle(String graphTitle) {
		this.graphTitle = graphTitle;
	}
}
