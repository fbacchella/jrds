/*
 * Created on 7 févr. 2005
 */
package jrds;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jrds.probe.IndexedProbe;
import jrds.probe.JdbcProbe;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;


/**
 * @author bacchell
 *
 * TODO 
 */
public final class GraphDesc implements Cloneable {
	static final private Logger logger = JrdsLogger.getLogger(GraphDesc.class);
	
	private static final class ConsFunc  {
		private String id;
		private ConsFunc(String id) { this.id = id ;}
		public String toString() { return id;}
		static public final ConsFunc AVERAGE = new ConsFunc("AVERAGE");
		static public final ConsFunc MIN = new ConsFunc("MIN");			
		static public final ConsFunc MAX = new ConsFunc("MAX");			
		static public final ConsFunc LAST = new ConsFunc("LAST");			
	};
	
	static public final ConsFunc AVERAGE = ConsFunc.AVERAGE;
	static public final ConsFunc MIN = ConsFunc.MIN;
	static public final ConsFunc MAX = ConsFunc.MAX;
	static public final ConsFunc LAST = ConsFunc.LAST;
	
	private interface GraphType{
		public void draw(RrdGraphDef rgd, String sn, Color color, String legend) throws RrdException;
		static public final GraphType NONE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {};
		};
		static public final GraphType LINE = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color, String legend) throws RrdException {
				rgd.line(sn, color, legend);
			};
		};
		static public final GraphType AREA = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color, String legend) throws RrdException {
				rgd.area(sn, color, legend);
			};
		};
		static public final GraphType STACK = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color, String legend) throws RrdException {
				rgd.stack(sn, color, legend);
			};
		};
		static public final GraphType COMMENT = new GraphType() {
			public void draw(RrdGraphDef rgd, String sn, Color color, String legend) throws RrdException {
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
				return ((IndexedProbe)graph.getProbe()).getIndexName();
			}
		};
		static public final PathElement SUBTITLE = new PathElement() {
			public String resolve(RdsGraph graph) {
				return graph.getGraphSubTitle();
			}
		};
		static public final PathElement JDBC = new PathElement() {
			public String resolve(RdsGraph graph) {
				return ((JdbcProbe)graph.getProbe()).getJdbcurl();
			}
		};
	}
	
	static final public PathElement HOST = PathElement.HOST;
	static final public PathElement TITLE = PathElement.TITLE;
	static final public PathElement INDEX = PathElement.INDEX;
	static final public PathElement SUBTITLE = PathElement.SUBTITLE;
	static final public PathElement JDBC= PathElement.JDBC;
	static final public String DISK = "Disques";
	static final public String NETWORK = "Réseau";
	static final public String SERVICES = "Services";
	static final public String SYSTEM = "Système";

	static final public String LOAD = "Charge";
	static final public String DISKACTIVITY = "Activité disques";
	static final public String WEB = "Web";
	static final public String INTERFACES = "Interfaces";
	static final public String IP = "IP";
	static final public String MEMORY = "Mémoire";
	static final public String DATABASE = "Base de données";
	
	static final public Object[] HSLT = new Object[] { HOST, SYSTEM, LOAD, TITLE };
	static final public Object[] SLHT = new Object[] { SYSTEM, LOAD, TITLE, HOST };

	static final public Object[] DAHIT = new Object[] { DISK, DISKACTIVITY, HOST, SUBTITLE, TITLE };
	static final public Object[] HDAIT = new Object[] { HOST, DISK, DISKACTIVITY, INDEX, TITLE };
	
	static final public Object[] NIHIT = new Object[] { NETWORK, INTERFACES, HOST, INDEX, TITLE };
	static final public Object[] HNIIT = new Object[] { HOST, NETWORK, INTERFACES, INDEX, TITLE };

	static final public Object[] HNT = new Object[] { GraphDesc.HOST, GraphDesc.NETWORK , GraphDesc.TITLE};
	static final public Object[] NTH = new Object[] { GraphDesc.NETWORK, GraphDesc.TITLE, GraphDesc.HOST };

	static final public Object[] HSMT = new Object[] { HOST, SYSTEM, MEMORY, TITLE};
	static final public Object[] SMHT = new Object[] { GraphDesc.SYSTEM, GraphDesc.MEMORY, GraphDesc.TITLE, GraphDesc.HOST};
	
	static final public Object[] HSDJT = new Object[] { HOST, SERVICES, DATABASE, JDBC, TITLE};
	static final public Object[] SDJT = new Object[] { SERVICES, DATABASE, JDBC, TITLE };

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
	
	static final public Color COLOR1 = colors[0];
	static final public Color COLOR2 = colors[1];
	static final public Color COLOR3 = colors[2];
	static final public Color COLOR4 = colors[3];
	static final public Color COLOR5 = colors[4];
	static final public Color COLOR6 = colors[5];
	static final public Color COLOR7 = colors[6];
	
	private Map dsMap;
	private String filename;
	private boolean filenameRO = false;
	private String graphTitle;
	private boolean graphTitleRO = false;
	private int width = 578;
	private int height = 206;
	private double upperLimit = Double.NaN;
	private double lowerLimit = 0;
	private boolean cloned = false;
	private String verticalLabel = null; 
	private int lastColor = 0;
	private List viewTree = null;
	private List hostTree = null;
	private String subTitle = null;
	//We don't want this to be copied during a clone
	private final class  Dimension {
		public int width = 0;
		public int height = 0;		
	};
	private Dimension dimension = new Dimension();
	
	static private class DsDesc {
		public String name;
		public String dsName;
		public String rpn;
		public GraphType graphType;
		public Color color;
		public String legend;
		public ConsFunc cf;
		public DsDesc(String name, String dsName, String rpn, GraphType graphType, Color color, String legend, ConsFunc cf)
		{
			this.name = name;
			this.dsName = dsName;
			this.rpn = rpn;
			this.graphType = graphType;
			this.color = color;
			this.legend = legend;
			this.cf = cf;
		}
	}

	/**
	 * 
	 */
	public GraphDesc(int size) {
		dsMap = new LinkedHashMap(size);
	}

	public GraphDesc() {
		dsMap = new LinkedHashMap();
	}
	
	public void add(String name , GraphType graphType, Color color)
	{
		dsMap.put(name, new DsDesc(name, name, null, graphType, color, name, ConsFunc.AVERAGE));
	}

	public void add(String name , GraphType graphType, Color color, String legend)
	{
		dsMap.put(name, new DsDesc(name, name, null, graphType, color, legend, ConsFunc.AVERAGE));
	}

	public void add(String name , GraphType graphType, String legend)
	{
		dsMap.put(name, new DsDesc(name, name, null, graphType, colors[(lastColor++) % colors.length], legend, ConsFunc.AVERAGE));
	}

	public void add(String name , GraphType graphType)
	{
		dsMap.put(name, new DsDesc(name, name, null, graphType, colors[(lastColor++) % colors.length], name, ConsFunc.AVERAGE));
	}

	public void add(GraphType graphType, String legend)
	{
		dsMap.put(legend, new DsDesc(null, null, null, graphType, null, legend, null));
	}

	public void add(String name , String rpn, GraphType graphType, Color color, String legend)
	{
		dsMap.put(name, new DsDesc(name, null, rpn, graphType, color, legend, ConsFunc.AVERAGE));
	}

	public void add(String name , String rpn, GraphType graphType, Color color)
	{
		dsMap.put(name, new DsDesc(name, null, rpn, graphType, color, name, ConsFunc.AVERAGE));
	}

	public void add(String name , String rpn, GraphType graphType, String legend)
	{
		dsMap.put(name, new DsDesc(name, null, rpn, graphType, colors[lastColor++ % colors.length], legend, ConsFunc.AVERAGE));
	}

	public void add(String name)
	{
		dsMap.put(name, new DsDesc(name, name, null, NONE, null, null, ConsFunc.AVERAGE));
	}

	public void add(String name, String rpn)
	{
		dsMap.put(name, new DsDesc(name, null, rpn, NONE, null, null, ConsFunc.AVERAGE));
	}

	public RrdGraphDef getGraphDef(Probe probe) throws IOException, RrdException
	{
		RrdGraphDef retValue = new RrdGraphDef();
		boolean firstStack = true;
		String rrdName = probe.getRrdName();
		RrdDb rrddb = probe.getRrdDb();
		for(Iterator i = dsMap.values().iterator() ; i.hasNext() ;) {
			DsDesc ds = (DsDesc) i.next();
			if(ds.dsName == null && ds.rpn == null)
				ds.graphType.draw(retValue, ds.name, ds.color, ds.legend + "@l");
			else if( ds.rpn == null) {
				if(rrddb.getDatasource(ds.dsName) != null) {
					retValue.datasource(ds.name, rrdName, ds.dsName, ds.cf.toString());
					ds.graphType.draw(retValue, ds.name, ds.color, ds.legend + "@l");
				}
			}
			else {
				retValue.datasource(ds.name , ds.rpn);
				ds.graphType.draw(retValue, ds.name, ds.color, ds.legend + "@l");
			}
		}
		retValue.setTitle(getGraphTitle() + " on " + probe.getHost().getName());
		retValue.setShowLegend(true);
		retValue.setGridRange(lowerLimit, upperLimit, false);
		if(verticalLabel != null)
			retValue.setVerticalLabel(verticalLabel);
		return retValue;
	}
	
	/**
	 * @return Returns the filename.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename The filename to set.
	 */
	public void setFilename(String filename) {
		if(  cloned || ! filenameRO ) {
			this.filename = filename;
			filenameRO = ! cloned;
		}
		else 
			logger.error("filename tried to be set twice");
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
		if( cloned || ! graphTitleRO ) {
			this.graphTitle = graphTitle;
			graphTitleRO = ! cloned;
		}
		else 
			logger.error("graph title tried to be set twice");
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
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		Object o = null;
		try {
			o = super.clone();
			((GraphDesc) o).cloned = true;
			((GraphDesc) o).filenameRO = false;
			((GraphDesc) o).graphTitleRO = false;
		} catch (CloneNotSupportedException e) {
			logger.error("Clone not suported for this object");
		}
		return o;
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
	 * @return Returns the cloned.
	 */
	public boolean isCloned() {
		return cloned;
	}
	/**
	 * @return Returns the viewTree.
	 */
	public LinkedList getViewTree(RdsGraph graph) {
		LinkedList tree = new LinkedList();
		for(Iterator i = viewTree.iterator(); i.hasNext() ; ) {
			Object o = i.next();
			if (o instanceof String)
				tree.add(o);
			else if (o instanceof PathElement)
				tree.add(((PathElement)o).resolve(graph));
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
		for(Iterator i = hostTree.iterator(); i.hasNext() ; ) {
			Object o = i.next();
			if (o instanceof String)
				tree.add(o);
			else if (o instanceof PathElement)
				tree.add(((PathElement)o).resolve(graph));
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
	 * @return Returns the subTitle.
	 */
	public String getSubTitle() {
		return subTitle;
	}
	/**
	 * @param subTitle The subTitle to set.
	 */
	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}
}
