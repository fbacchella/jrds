//----------------------------------------------------------------------------
//$Id$

package jrds;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.probe.jdbc.JdbcProbe;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jrobin.core.RrdException;
import org.jrobin.graph.Plottable;
import org.jrobin.graph.RrdGraphDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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


	public interface GraphType {
		public abstract void draw(RrdGraphDef rgd, String sn, Color color) throws
		RrdException;

		public static final GraphType NONE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) {}
			@Override
			public String toString() {
				return "none";
			};
		};

		public static final GraphType VOID = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) {};
			@Override
			public String toString() {
				return "void";
			};
		};

		public static final GraphType LINE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) throws RrdException {
				rgd.line(sn, color, " @g");
			};
			@Override
			public String toString() {
				return "line";
			};
		};
		static public final GraphType AREA = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) throws RrdException {
				rgd.area(sn, color, " @g");
			};
			@Override
			public String toString() {
				return "area";
			};
		};
		static public final GraphType STACK = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) throws RrdException {
				rgd.stack(sn, color, " @g");
			};
			@Override
			public String toString() {
				return "stack";
			};
		};
		static public final GraphType COMMENT = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color) throws RrdException {};
			@Override
			public String toString() {
				return "comment";
			};
		};
	};

	static final public GraphType NONE = GraphType.NONE;
	static final public GraphType LINE = GraphType.LINE;
	static final public GraphType AREA = GraphType.AREA;
	static final public GraphType STACK = GraphType.STACK;
	static final public GraphType COMMENT = GraphType.COMMENT;

	private static abstract class PathElement {
		public abstract String resolve(RdsGraph graph);
		public String toString() {
			return this.resolve(null).toUpperCase();
		}

		static public final PathElement HOST = new PathElement() {
			public String resolve(RdsGraph graph) {
				return graph.getProbe().getHost().getName();
			}
			public String toString() {
				return "HOST";
			}
		};
		static public final PathElement TITLE = new PathElement() {
			public String resolve(RdsGraph graph) {
				return graph.getGraphTitle();
			}
			public String toString() {
				return "TITLE";
			}
		};
		static public final PathElement INDEX = new PathElement() {
			public String resolve(RdsGraph graph) {
				return ( (IndexedProbe) graph.getProbe()).getIndexName();
			}
			public String toString() {
				return "INDEX";
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
			public String toString() {
				return "URL";
			}
		};
		static public final PathElement JDBC = new PathElement() {
			public String resolve(RdsGraph graph) {
				return ( (JdbcProbe) graph.getProbe()).getUrlAsString();
			}
			public String toString() {
				return "JDBC";
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
			public String toString() {return "DISKACTIVITY";}
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
				return dbprobe.getUrlAsString();
			}
			@Override
			public String toString() {
				return "DBINSTANCE";
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


	static final private Map<String, Color> COLORMAP = new HashMap<String, Color>();
	static {
		//Netscape alias for cyan
		COLORMAP.put("AQUA",  new Color(0,255,225) {
			public String toString() { return "AQUA"; }
		});
		COLORMAP.put("BLACK",  new Color(Color.BLACK.getRGB()) {
			public String toString() { return "BLACK"; }
		});
		COLORMAP.put("BLUE", new Color(Color.BLUE.getRGB()) {
			public String toString() { return "BLUE"; }
		});
		COLORMAP.put("CYAN",  new Color(Color.CYAN.getRGB()) {
			public String toString() { return "CYAN"; }
		});
		COLORMAP.put("FUCHSIA",  new Color(255,0,255) {
			public String toString() { return "FUCHSIA"; }
		});
		COLORMAP.put("GRAY",  new Color(Color.GRAY.getRGB()) {
			public String toString() { return "GRAY"; }
		});
		COLORMAP.put("GREEN", new Color(Color.GREEN.getRGB()) {
			public String toString() { return "GREEN"; }
		});
		COLORMAP.put("LIME",  new Color(204,255,0) {
			public String toString() { return "LIME"; }
		});
		COLORMAP.put("MAGENTA",  new Color(Color.MAGENTA.getRGB()) {
			public String toString() { return "MAGENTA"; }
		});
		COLORMAP.put("MAROON",  new Color(128,0,0) {
			public String toString() { return "MAROON"; }
		});
		COLORMAP.put("NAVY",  new Color(0,0,128) {
			public String toString() { return "NAVY"; }
		});
		COLORMAP.put("OLIVE",  new Color(128,128,0) {
			public String toString() { return "OLIVE"; }
		});
		COLORMAP.put("PURPLE",  new Color(128,0,128) {
			public String toString() { return "PURPLE"; }
		});
		COLORMAP.put("RED",  new Color(Color.RED.getRGB()) {
			public String toString() { return "RED"; }
		});
		COLORMAP.put("SILVER",  new Color(192,192,192) {
			public String toString() { return "SILVER"; }
		});
		COLORMAP.put("TEAL",  new Color(0,128,128) {
			public String toString() { return "TEAL"; }
		});
		COLORMAP.put("WHITE",  new Color(Color.WHITE.getRGB()) {
			public String toString() { return "WHITE"; }
		});
		COLORMAP.put("YELLOW",  new Color(Color.YELLOW.getRGB()) {
			public String toString() { return "YELLOW"; }
		});
		COLORMAP.put("ORANGE",  new Color(Color.ORANGE.getRGB()) {
			public String toString() { return "ORANGE"; }
		});
		COLORMAP.put("PINK",  new Color(Color.PINK.getRGB()) {
			public String toString() { return "PINK"; }
		});
		COLORMAP.put("DARK_GRAY",  new Color(Color.DARK_GRAY.getRGB()) {
			public String toString() { return "DARK_GRAY"; }
		});
		COLORMAP.put("GRAY",  new Color(Color.GRAY.getRGB()) {
			public String toString() { return "GRAY"; }
		});
		COLORMAP.put("LIGHT_GRAY",  new Color(Color.LIGHT_GRAY.getRGB()) {
			public String toString() { return "LIGHT_GRAY"; }
		});
		COLORMAP.put("PINK",  new Color(Color.GREEN.getRGB()) {
			public String toString() { return "GREEN"; }
		});
	}
	static final public Color[] colors = new Color[] {
		COLORMAP.get("BLUE"),
		COLORMAP.get("GREEN"),
		COLORMAP.get("RED"),
		COLORMAP.get("CYAN"),
		COLORMAP.get("ORANGE"),
		COLORMAP.get("TEAL"),
		COLORMAP.get("YELLOW"),
		COLORMAP.get("PINK"),
		COLORMAP.get("MAGENTA"),
		COLORMAP.get("BLACK"),
		COLORMAP.get("NAVY")
	};

	/*static final public Color BLUE = colors[0];
	static final public Color GREEN = colors[1];
	static final public Color RED = colors[2];
	static final public Color CYAN = colors[3];
	static final public Color BLACK = colors[4];
	static final public Color ORANGE = colors[5];
	static final public Color YELLOW = colors[6];
	static final public Color PINK = colors[7];
	static final public Color MAGENTA = colors[8];*/

	private final class Dimension {
		public int width = 0;
		public int height = 0;
	};

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

	static final String manySpace = "                                                                  ";
	private Map<Object, DsDesc> dsMap;
	private int width = 578;
	private int height = 206;
	private double upperLimit = Double.NaN;
	private double lowerLimit = 0;
	private String verticalLabel = null;
	private int lastColor = 0;
	private List viewTree = new ArrayList();
	private List hostTree = new ArrayList();
	private String graphName;
	private String name;
	private String graphTitle ="{0} on {1}";
	private Dimension dimension = new Dimension();
	private int maxLengthLegend = 0;
	private boolean siUnit = true;

	/**
	 * A constructor wich pre allocate the desired size
	 * @param size the estimated number of graph that will be created
	 */
	public GraphDesc(int size) {
		dsMap = new LinkedHashMap<Object, DsDesc>(size);
	}

	public GraphDesc() {
		dsMap = new LinkedHashMap<Object, DsDesc>();
	}

	/**
	 * add a graph element
	 *
	 * @param name String
	 * @param graphType GraphType
	 * @param color Color
	 */
	public void add(String name, GraphType graphType, Color color) {
		add(name, name, null, graphType, color, name, DEFAULTCF, false);
	}

	public void add(String name, GraphType graphType, Color color,
			String legend) {
		add(name, name, null, graphType, color, legend, DEFAULTCF, false);
	}

	public void add(String name, GraphType graphType, String legend) {
		add(name, name, null, graphType,
				colors[ (lastColor) % colors.length], legend,
				DEFAULTCF, false);
		if(graphType != GraphType.COMMENT && graphType != GraphType.NONE && graphType != GraphType.VOID)
			lastColor++;
	}

	public void add(String name, GraphType graphType) {
		add(name, name, null, graphType,
				colors[ (lastColor) % colors.length], name,
				DEFAULTCF, false);
		if(graphType != GraphType.COMMENT && graphType != GraphType.NONE && graphType != GraphType.VOID)
			lastColor++;
	}

	/**
	 * Used to add a lign in the legend
	 *
	 * @param graphType GraphType
	 * @param legend String
	 */
	public void add(GraphType graphType, String legend) {
		add(null, null, null, graphType, null, legend, null, false);
	}

	public void add(String name, String rpn, GraphType graphType, Color color,
			String legend) {
		add(name, null, rpn, graphType, color, legend,
				DEFAULTCF, false);
	}

	public void add(String name, String rpn, GraphType graphType, Color color) {
		add(name, null, rpn, graphType, color, name,
				DEFAULTCF, false);
	}

	public void add(String name, String rpn, GraphType graphType, String legend) {
		add(name, null, rpn, graphType,
				colors[lastColor % colors.length], legend,
				DEFAULTCF, false);
		if(graphType != GraphType.COMMENT && graphType != GraphType.NONE && graphType != GraphType.VOID)
			lastColor++;
	}

	/**
	 * Add a datastore that will not generate a graph
	 *
	 * @param name String
	 */
	public void add(String name) {
		add(name, name, null, NONE, null, null, DEFAULTCF, false);
	}

	public void add(String name, String rpn) {
		add(name, null, rpn, NONE, null, null, DEFAULTCF, false);
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
	 * @param reversed
	 */
	public void add(String name, String dsName, String rpn,
			String graphType, String color, String legend,
			String consFunc, String reversed) {
		if (dsName == null && rpn == null)
			dsName = name;
		GraphType gt = null;
		if(graphType == null) {
			if(legend != null)
				gt = GraphType.COMMENT;
			else if(name != null)
				gt = GraphType.NONE;
		}
		else
			gt = (GraphType) resolv(GraphType.class, graphType);
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
		else {
			c = colors[lastColor % colors.length];
			if(gt != GraphType.COMMENT && gt != GraphType.NONE && gt != GraphType.VOID)
				lastColor++;

		}
		if(legend == null && name != null)
			legend = name;
		add(name, dsName, rpn, gt, c, legend, cf, reversed != null);

	}

	public void add(String name, String dsName, String rpn,
			GraphType graphType, Color color, String legend,
			ConsFunc cf, boolean reversed) {
		String key = name;
		if(key == null && legend != null)
			key = legend;
		if(reversed) {
			dsMap.put(key,
					new DsDesc(name, dsName, rpn, GraphType.NONE, null, null, cf));
			String revRpn = "0, " + name + ", -";
			dsMap.put("rev_" + key,
					new DsDesc("rev_" + name, "rev_" + name, revRpn, graphType, color, null, cf));
			dsMap.put("legend_" + key,
					new DsDesc(name, dsName, null, GraphType.VOID, null, legend, cf));
		}
		else
			dsMap.put(key,
					new DsDesc(name, dsName, rpn, graphType, color, legend, cf));
		if(legend != null) {
			maxLengthLegend = Math.max(maxLengthLegend, legend.length());
		}

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

		/*The title line*/
		retValue.comment("   @G"); //We simulate the color box
		retValue.comment(manySpace.substring(0, Math.min(maxLengthLegend, manySpace.length())) + "@G");
		retValue.comment("    Current");
		retValue.comment("  Average");
		retValue.comment("  Minimum");
		retValue.comment("  Maximum");
		retValue.comment("@l");

		for(DsDesc ds: dsMap.values()) {
			if (ds.dsName == null && ds.rpn == null) {
				ds.graphType.draw(retValue, ds.name, ds.color);
				addLegend(retValue, ds.name, ds.graphType, ds.legend);
			}
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
					if(ds.graphType != null) {
						ds.graphType.draw(retValue, ds.name, ds.color);
						addLegend(retValue, ds.name, ds.graphType, ds.legend);
					}
					else {
						logger.warn("graph type is null for " + ds.dsName + " on probe " + probe);
					}
				}
			}
			else {
				retValue.datasource(ds.name, ds.rpn);
				ds.graphType.draw(retValue, ds.name, ds.color);
				addLegend(retValue, ds.name, ds.graphType, ds.legend);
			}
		}
		retValue.setShowLegend(true);
		retValue.setGridRange(lowerLimit, upperLimit, false);
		if (verticalLabel != null)
			retValue.setVerticalLabel(verticalLabel);
		if(this.siUnit)
			retValue.setBaseValue(1000);
		else	
			retValue.setBaseValue(1024);
		
			
		return retValue;
	}

	private void addLegend(RrdGraphDef def, String ds, GraphType gt, String legend) throws RrdException {
		if(gt == GraphType.COMMENT) {
			def.comment(legend + "@l");
		}
		else if(gt != GraphType.NONE && legend != null) {
			def.comment(legend);
			int missingLength = Math.min(maxLengthLegend - legend.length(), manySpace.length());
			if(missingLength > 0)
				def.comment(manySpace.substring(0, missingLength) + "@G");
			def.gprint(ds, ConsFunc.LAST.toString(), "@8.2@s");
			def.gprint(ds, ConsFunc.AVERAGE.toString(), "@8.2@s");
			def.gprint(ds, ConsFunc.MIN.toString(), "@8.2@s");
			def.gprint(ds, ConsFunc.MAX.toString(), "@8.2@s");
			def.comment("@l");
		}
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
	 * @param upperLimit The upperLimit to set.
	 */
	public void setLowerLimit(String lowerLimit) {
		this.lowerLimit = Double.parseDouble(lowerLimit);
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
	public LinkedList<String> getViewTree(RdsGraph graph) {
		LinkedList<String> tree = new LinkedList<String>();
		for (Object o: viewTree) {
			if (o instanceof String)
				tree.add((String)o);
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
	public LinkedList<String> getHostTree(RdsGraph graph) {
		LinkedList<String> tree = new LinkedList<String>();
		for (Object o: hostTree) {
			if (o instanceof String)
				tree.add((String)o);
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public void dumpAsXml(Class c) throws ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();  // Create from whole cloth
		Element root = 
			(Element) document.createElement("graphdesc"); 
		document.appendChild(root);

		Element nameElement = document.createElement("name");
		nameElement.appendChild(document.createTextNode(c.getSimpleName()));
		root.appendChild(nameElement);

		Element graphNameElement = document.createElement("graphName");
		graphNameElement.appendChild(document.createTextNode(graphName));
		root.appendChild(graphNameElement);

		Element graphTitleElement = document.createElement("graphTitle");
		graphTitleElement.appendChild(document.createTextNode(graphTitle));
		root.appendChild(graphTitleElement);

		Element verticalLabelElement = document.createElement("verticalLabel");
		verticalLabelElement.appendChild(document.createTextNode(verticalLabel));
		root.appendChild(verticalLabelElement);

		if(upperLimit != Double.NaN) {
			Element upperLimitElement = document.createElement("upperLimit");
			upperLimitElement.appendChild(document.createTextNode(Double.toString(upperLimit)));
			root.appendChild(upperLimitElement);
		}

		if(lowerLimit != 0) {
			Element lowerLimitElement = document.createElement("lowerLimit");
			lowerLimitElement.appendChild(document.createTextNode(Double.toString(lowerLimit)));
			root.appendChild(lowerLimitElement);
		}

		for(Map.Entry<Object, DsDesc> e: dsMap.entrySet()) {
			DsDesc ds = e.getValue();
			Element dsElement = document.createElement("add");
			root.appendChild(dsElement);

			Element dsNameElement = document.createElement("name");
			dsElement.appendChild(dsNameElement);
			dsNameElement.appendChild(document.createTextNode(ds.name));

			if(ds.rpn != null) {
				Element rpnElement = document.createElement("rpn");
				rpnElement.appendChild(document.createTextNode(ds.rpn));
				dsElement.appendChild(rpnElement);
			}			

			if(ds.graphType != null) {
				Element dsTypeElement = document.createElement("graphType");
				dsTypeElement.appendChild(document.createTextNode(ds.graphType.toString().toLowerCase()));
				dsElement.appendChild(dsTypeElement);
			}			

			if(ds.color != null && ds.graphType != GraphDesc.COMMENT && ds.graphType != GraphDesc.NONE) {
				Element colorElement = document.createElement("color");
				colorElement.appendChild(document.createTextNode(ds.color.toString()));
				dsElement.appendChild(colorElement);
			}			

			if(ds.legend != null) {
				Element legendElement = document.createElement("legend");
				legendElement.appendChild(document.createTextNode(ds.legend));
				dsElement.appendChild(legendElement);
			}			
			if(ds.cf != GraphDesc.AVERAGE) {
				Element cfElement = document.createElement("cf");
				cfElement.appendChild(document.createTextNode(ds.cf.id));
				dsElement.appendChild(cfElement);
			}			
		}

		doTree(root, "hosttree", hostTree);
		doTree(root, "viewtree", viewTree);

		FileOutputStream fos = new FileOutputStream("desc/autograph/" + c.getSimpleName().toLowerCase() + ".xml");
//		XERCES 1 or 2 additionnal classes.
		OutputFormat of = new OutputFormat("XML","UTF-8",true);
		of.setIndent(1);
		of.setIndenting(true);
		of.setDoctype("-//jrds//DTD Graph Description//EN","urn:jrds:graphdesc");
		XMLSerializer serializer = new XMLSerializer(fos,of);
//		As a DOM Serializer
		serializer.asDOMSerializer();
		serializer.serialize( document.getDocumentElement() );
	}

	private void doTree(Element root, String name, Collection tree) {
		Document document = root.getOwnerDocument();
		Element hosttreeElement = document.createElement(name);
		root.appendChild(hosttreeElement);
		for(Object o: tree) {
			String pathName = null;
			String pathType = null;
			if(o instanceof String) {
				pathName = o.toString();
				pathType = "pathstring";
			}
			else if(o instanceof PathElement) {
				pathName = ((PathElement) o).toString();
				pathType = "pathelement";
			}
			if(pathName != null) {
				Element graphPathElement = document.createElement(pathType);
				hosttreeElement.appendChild(graphPathElement);
				graphPathElement.appendChild(document.createTextNode(pathName));
			}
		}

	}

	public boolean isSiUnit() {
		return siUnit;
	}

	public void setSiUnit(boolean siUnit) {
		this.siUnit = siUnit;
	}

}
