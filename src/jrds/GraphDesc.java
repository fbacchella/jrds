package jrds;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Util.SiPrefix;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.probe.jdbc.JdbcProbe;
import jrds.webapp.ACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Plottable;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A classed used to store the static description of a graph
 * @author Fabrice Bacchella
 * @version $Id$
 */
public class GraphDesc
implements Cloneable, WithACL {
    static final private Logger logger = Logger.getLogger(GraphDesc.class);

    static public final ConsolFun DEFAULTCF = ConsolFun.AVERAGE;

    public enum GraphType {
        NONE  {
            public void draw(RrdGraphDef rgd, String sn, Color color) {}
            public String toString() {
                return "none";
            }
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return false;
            }
            public boolean legend() {
                return false;
            }
        },
        LEGEND {
            public void draw(RrdGraphDef rgd, String sn, Color color) {}
            public String toString() {
                return "void";
            }
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return false;
            }
            public boolean legend() {
                return true;
            }
        },
        PERCENTILELEGEND {
            public void draw(RrdGraphDef rgd, String sn, Color color) {};
            @Override
            public String toString() {
                return "percentile legend";
            };
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return false;
            };
            public boolean legend() {
                return true;
            };
        },
        COMMENT {
            public void draw(RrdGraphDef rgd, String sn, Color color) {};
            public String toString() {
                return "comment";
            }
            public boolean datasource() {
                return false;
            }
            public boolean toPlot() {
                return false;
            }
            public boolean legend() {
                return true;
            }
        },
        LINE {
            public void draw(RrdGraphDef rgd, String sn, Color color) {
                rgd.line(sn, color, " \\g");
            };
            @Override
            public String toString() {
                return "line";
            };
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return true;
            };
            public boolean legend() {
                return true;
            };
        },
        AREA {
            public void draw(RrdGraphDef rgd, String sn, Color color) {
                rgd.area(sn, color, " \\g");
            };
            @Override
            public String toString() {
                return "area";
            };
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return true;
            };
            public boolean legend() {
                return true;
            };
        },
        STACK {
            public void draw(RrdGraphDef rgd, String sn, Color color) {
                rgd.stack(sn, color, " \\g");
            };
            @Override
            public String toString() {
                return "stack";
            };
            public boolean datasource() {
                return true;
            }
            public boolean toPlot() {
                return true;
            };
            public boolean legend() {
                return true;
            };
        };

        public abstract void draw(RrdGraphDef rgd, String sn, Color color);
        public abstract boolean toPlot();
        public abstract boolean datasource();
        public abstract boolean legend();
    };

    //Old name kept
    static final public GraphType NONE = GraphType.NONE;
    static final public GraphType DATASOURCE = GraphType.NONE;
    static final public GraphType LINE = GraphType.LINE;
    static final public GraphType AREA = GraphType.AREA;
    static final public GraphType STACK = GraphType.STACK;
    static final public GraphType COMMENT = GraphType.COMMENT;

    private enum PathElement {
        HOST {
            public String resolve(GraphNode graph) {
                return graph.getProbe().getHost().getName();
            }
            public String toString() {
                return "HOST";
            }
        },
        TITLE {
            public String resolve(GraphNode graph) {
                return graph.getGraphTitle();
            }
            public String toString() {
                return "TITLE";
            }
        },
        INDEX {
            public String resolve(GraphNode graph) {
                StringBuffer retValue = new StringBuffer("empty");
                if(graph.getProbe() instanceof IndexedProbe) {
                    retValue.setLength(0);
                    IndexedProbe ip = (IndexedProbe) graph.getProbe();
                    retValue.append(ip.getIndexName());
                    //Check to see if a label is defined and needed to add
                    String label = graph.getProbe().getLabel();
                    if(label != null) {
                        retValue.append(" (" + label + ")");
                    }
                }
                else {
                    logger.debug("Bad graph definition for " + graph);
                }
                return retValue.toString();
            }
            public String toString() {
                return "INDEX";
            }
        },
        URL {
            public String resolve(GraphNode graph) {
                String url = "";
                Probe<?,?> probe = graph.getProbe();
                if( probe instanceof UrlProbe) {
                    url =((UrlProbe) probe).getUrlAsString();
                }
                return url;
            }
            public String toString() {
                return "URL";
            }
        },
        JDBC {
            public String resolve(GraphNode graph) {
                return ( (JdbcProbe) graph.getProbe()).getUrlAsString();
            }
            public String toString() {
                return "JDBC";
            }
        },
        DISK {
            public String resolve(GraphNode graph) {
                return "Disk";
            }
        },
        NETWORK {
            public String resolve(GraphNode graph) {
                return "Network";
            }
        },
        TCP {
            public String resolve(GraphNode graph) {
                return "TCP";
            }
        },
        SERVICES {
            public String resolve(GraphNode graph) {
                return "Services";
            }
        },
        SYSTEM {
            public String resolve(GraphNode graph) {
                return "System";
            }
        },
        LOAD {
            public String resolve(GraphNode graph) {
                return "Load";
            }
        },
        DISKACTIVITY {
            public String resolve(GraphNode graph) {
                return "Disk activity";
            }
            public String toString() {return "DISKACTIVITY";}
        },
        WEB {
            public String resolve(GraphNode graph) {
                return "Web";
            }
        },
        INTERFACES {
            public String resolve(GraphNode graph) {
                return "Interfaces";
            }
        },
        IP {
            public String resolve(GraphNode graph) {
                return "IP";
            }
        },
        MEMORY {
            public String resolve(GraphNode graph) {
                return "Memory";
            }
        },
        DATABASE{
            public String resolve(GraphNode graph) {
                return "Databases";
            }
        },
        DBINSTANCE {
            public String resolve(GraphNode graph) {
                JdbcProbe dbprobe = (JdbcProbe) graph.getProbe();
                return dbprobe.getUrlAsString();
            }
            @Override
            public String toString() {
                return "DBINSTANCE";
            }
        };
        public abstract String resolve(GraphNode graph);
        public String toString() {
            return resolve(null).toUpperCase();
        }
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

    public enum Colors {
        //240 ° 
        BLUE {
            @Override
            public Color getColor() {
                return Color.BLUE;
            }
        },
        //120°
        GREEN {
            @Override
            public Color getColor() {
                return Color.GREEN;
            }
        },
        //0°
        RED {
            @Override
            public Color getColor() {
                return Color.RED;
            }
        },
        //180°
        CYAN {
            @Override
            public Color getColor() {
                return Color.CYAN;
            }
        },
        // 47°
        ORANGE {
            @Override
            public Color getColor() {
                return Color.ORANGE;
            }
        },
        //180°
        TEAL {
            @Override
            public Color getColor() {
                return new Color(0,128,128);
            }
        },
        //60°
        YELLOW {
            @Override
            public Color getColor() {
                return Color.YELLOW;
            }
        },
        //300°
        MAGENTA {
            @Override
            public Color getColor() {
                return Color.MAGENTA;
            }
        },
        //0°
        PINK {
            @Override
            public Color getColor() {
                return Color.PINK;
            }
        },
        //0°
        BLACK {
            @Override
            public Color getColor() {
                return Color.BLACK;
            }
        },
        NAVY {
            @Override
            public Color getColor() {
                return new Color(0,0,128);
            }
        },
        //0°
        GRAY {
            @Override
            public Color getColor() {
                return Color.GRAY;
            }
        },
        //0°
        LIGHT_GRAY {
            @Override
            public Color getColor() {
                return Color.LIGHT_GRAY;
            }
        },
        DARK_GRAY {
            @Override
            public Color getColor() {
                return Color.DARK_GRAY;
            }
        },
        FUCHSIA {
            @Override
            public Color getColor() {
                return new Color(255,0,255);
            }
        },
        //Netscape alias for cyan
        AQUA {
            @Override
            public Color getColor() {
                return Color.CYAN;
            }
        },
        LIME {
            @Override
            public Color getColor() {
                return new Color(204,255,0);
            }
        },
        MAROON {
            @Override
            public Color getColor() {
                return new Color(128,0,0);
            }
        },
        OLIVE {
            @Override
            public Color getColor() {
                return new Color(128,128,0);
            }
        },
        PURPLE {
            @Override
            public Color getColor() {
                return new Color(128,0,128);
            }
        },
        SILVER {
            @Override
            public Color getColor() {
                return new Color(192,192,192);
            }
        },
        WHITE {
            @Override
            public Color getColor() {
                return Color.WHITE;
            }
        };

        public abstract Color getColor();
        public static final int length = Colors.values().length;
        public static final Color resolveIndex(int i) {
            return Colors.values()[ i % Colors.length].getColor();
        }
    };

    static private final class DsDesc {
        public String name;
        public String dsName;
        public String rpn;
        public GraphType graphType;
        public Color color;
        public String legend;
        public ConsolFun cf;
        public Integer percentile;
        public class DsPath  {
            String host;
            String probe;
        };
        public DsPath dspath = null;
        public DsDesc(String name, String dsName,
                String rpn, Integer percentile,
                GraphType graphType, Color color, String legend,
                ConsolFun cf, String host, String probe) {
            this.name = name;
            this.dsName = dsName;
            this.rpn = rpn;
            this.percentile = percentile;
            this.graphType = graphType;
            this.color = color;
            this.legend = legend;
            this.cf = cf;
            if(host != null && probe != null) {
                this.dspath = new DsPath();
                dspath.host = host;
                dspath.probe = probe;
            }
        }
        public String toString() {
            return "DsDesc(" + name + "," + dsName + ",\"" + rpn + "\"," + graphType + "," + color + ",\"" + legend + "\"," + cf + ")";
        }
    }

    //	static final private String manySpace = "123456798ABCDEF0123465798ABCDEF0123456798ABCDEF0123465798ABCDEF0123456798ABCDEF0123465798ABCDEF0";
    static final private String manySpace = "                                                                      ";
    //	private Map<Object, DsDesc> dsMap;
    private List<DsDesc> allds;
    private int width = 578;
    private int height = 206;
    private double upperLimit = Double.NaN;
    private double lowerLimit = 0;
    private String verticalLabel = null;
    private int lastColor = 0;
    private List<?> viewTree = new ArrayList<Object>();
    private List<?> hostTree = new ArrayList<Object>();
    private String graphName;
    private String name;
    private String graphTitle ="{0} on {1}";
    private int maxLengthLegend = 0;
    private boolean siUnit = true;
    private Integer unitExponent = null;
    private ACL acl = ACL.ALLOWEDACL;

    public final class Dimension {
        public int width = 0;
        public int height = 0;
    };
    private Dimension dimension = null;


    /**
     * A constructor wich pre allocate the desired size
     * @param size the estimated number of graph that will be created
     */
    public GraphDesc(int size) {
        allds = new ArrayList<DsDesc>(size);
    }

    public GraphDesc() {
        allds = new ArrayList<DsDesc>();
    }

    /**
     * add a graph element
     *
     * @param name String
     * @param graphType GraphType
     * @param color Color
     */
    public void add(String name, GraphType graphType, Color color) {
        add(name, name, null, graphType, color, name, DEFAULTCF, false, null, null, null);
    }

    public void add(String name, GraphType graphType, Color color,
            String legend) {
        add(name, name, null, graphType, color, legend, DEFAULTCF, false, null, null, null);
    }

    public void add(String name, GraphType graphType, String legend) {
        add(name, name, null, graphType,
                Colors.resolveIndex(lastColor), legend,
                DEFAULTCF, false, null, null, null);
        if(graphType.toPlot())
            lastColor++;
    }

    public void add(String name, GraphType graphType) {
        add(name, name, null, graphType,
                Colors.resolveIndex(lastColor), name,
                DEFAULTCF, false, null, null, null);
        if(graphType.toPlot())
            lastColor++;
    }

    /**
     * Used to add a line in the legend
     *
     * @param graphType GraphType
     * @param legend String
     */
    public void add(GraphType graphType, String legend) {
        add(null, null, null, graphType, null, legend, null, false, null, null, null);
    }

    public void add(String name, String rpn, GraphType graphType, Color color,
            String legend) {
        add(name, null, rpn, graphType, color, legend,
                DEFAULTCF, false, null, null, null);
    }

    public void add(String name, String rpn, GraphType graphType, Color color) {
        add(name, null, rpn, graphType, color, name,
                DEFAULTCF, false, null, null, null);
    }

    public void add(String name, String rpn, GraphType graphType, String legend) {
        add(name, null, rpn, graphType,
                Colors.resolveIndex(lastColor), legend,
                DEFAULTCF, false, null, null, null);
        if(graphType.toPlot())
            lastColor++;
    }

    /**
     * Add a datastore that will not generate a graph
     *
     * @param name String
     */
    public void add(String name) {
        add(name, name, null, GraphType.NONE, null, null, DEFAULTCF, false, null, null, null);
    }

    public void add(String name, String rpn) {
        add(name, null, rpn, GraphType.NONE, null, null, DEFAULTCF, false, null, null, null);
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
     * @param host
     * @param probe
     * @param subDsName
     */
    public void add(String name, String rpn,
            String graphType, String color, String legend,
            String consFunc, String reversed, String percentile,
            //The path to an external datastore
            String host, String probe, String dsName) {
        if(logger.isTraceEnabled())
            logger.trace("Adding " + name + ", " + rpn + ", " + graphType + ", " + color + ", " + legend + ", " + consFunc + ", " + reversed + ", " + host + ", " + probe);
        GraphType gt = null;
        if(graphType == null || "".equals(graphType)) {
            if(legend != null)
                gt = GraphType.COMMENT;
            else
                gt = GraphType.NONE;
        }
        else
            gt = GraphType.valueOf(graphType.toUpperCase());

        ConsolFun cf  = null;
        if(gt != GraphType.COMMENT) {
            cf = DEFAULTCF;
            if (consFunc != null && ! "".equals(consFunc))
                cf = ConsolFun.valueOf(consFunc.toUpperCase());
        }

        Color c = null;
        if(gt.toPlot()) {
            c = Color.WHITE;
            if(color != null && color.toUpperCase().matches("^#[0-9A-F]{6}")) {
                int r = Integer.parseInt(color.substring(1, 3), 16);
                int g = Integer.parseInt(color.substring(3, 5), 16);
                int b = Integer.parseInt(color.substring(5, 7), 16);
                c = new Color(r,g,b);
            }
            else if (color != null && ! "".equals(color)) {
                c = Colors.valueOf(color.toUpperCase()).getColor();
                if( c == null)
                    c = Color.getColor(color);
                if (c == null) {
                    logger.error("Cannot read color " + color);
                    c = Color.white;
                }
            }
            else {
                c = Colors.resolveIndex(lastColor);
                if(gt.toPlot())
                    lastColor++;

            }
        }
        if(name != null) {
            // If not a rpn, it must be a datastore
            if(gt.datasource() && rpn == null && dsName == null) {
                dsName = name;
            }
        }
        //If the name is missing, where do we find it ?
        else {
            if(rpn != null)
                name = rpn;
            else if(legend != null) {
                name = legend;
            }
            else if(host != null) {
                name = host + "/" + probe + "/" + dsName;
            }
        }
        //Auto generated legend
        if(legend == null && name != null && gt.legend())
            legend = name;

        Integer valPercentile = null;
        if(percentile != null && ! "".equals(percentile)) {
            valPercentile = jrds.Util.parseStringNumber(percentile, new Integer(0));
        }
        add(name, dsName, rpn, gt, c, legend, cf, reversed != null, valPercentile, host, probe);
    }

    public void add(String name, String dsName, String rpn,
            GraphType graphType, Color color, String legend,
            ConsolFun cf, boolean reversed, Integer percentile,
            //The path to an external datastore
            String host, String probe) {
        String finalName = name;
        if(reversed) {
            allds.add(
                    new DsDesc(name, dsName, rpn, null, GraphType.NONE, null, legend, cf, host, probe));
            String revRpn = "0, " + name + ", -";
            finalName = "rev_" + name;
            allds.add(
                    new DsDesc(finalName, finalName, revRpn, null, graphType, color, null, cf, host, probe));
            String legendName = "legend_" + name;
            allds.add(
                    new DsDesc(legendName, dsName, rpn, null, GraphType.LEGEND, null, legend, cf, host, probe));
            if(percentile != null) {
                allds.add(
                        new DsDesc("percentile" + percentile + "_" + name, finalName, null, percentile, GraphType.LINE, color.darker(), null, ConsolFun.MAX, host, probe));
                allds.add(
                        new DsDesc("percentile" + percentile + "_" + legendName, name, null, percentile, GraphType.PERCENTILELEGEND, null, percentile + "th percentile", ConsolFun.MAX, host, probe));
            }
        }
        else {
            allds.add(
                    new DsDesc(name, dsName, rpn, null, graphType, color, legend, cf, host, probe));
            if(percentile != null) {
                allds.add(
                        new DsDesc("percentile" + percentile + "_" + name, name, null, percentile, GraphType.LINE, color.darker(), null, ConsolFun.MAX, host, probe));
                allds.add(
                        new DsDesc("percentile" + percentile + "_legend_" +  name, name, null, percentile, GraphType.PERCENTILELEGEND, null, percentile + "th percentile", ConsolFun.MAX, host, probe));
            }
        }
        if(legend != null) {
            maxLengthLegend = Math.max(maxLengthLegend, legend.length());
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
    public RrdGraphDef getGraphDef(Probe<?,?> probe) throws IOException {
        return getGraphDef(probe, null);
    }

    public RrdGraphDef getEmptyGraphDef() {
        RrdGraphDef retValue = new RrdGraphDef();
        if( ! Double.isNaN(lowerLimit))
            retValue.setMinValue(lowerLimit);
        if( ! Double.isNaN(upperLimit))
            retValue.setMaxValue(upperLimit);
        if (verticalLabel != null)
            retValue.setVerticalLabel(verticalLabel);
        if(this.siUnit)
            retValue.setBase(1000);
        else    
            retValue.setBase(1024);
        if(unitExponent != null) {
            retValue.setUnitsExponent(unitExponent);
        }
        retValue.setPoolUsed(true);
        retValue.setAntiAliasing(true);
        retValue.setTextAntiAliasing(true);
        retValue.setImageFormat("PNG");
        retValue.setWidth(getWidth());
        retValue.setHeight(getHeight());
        return retValue;
    }

    /**
     * Fill a GraphDef with value as defined by the graph desc
     * @param graphDef the GraphDef to configure
     * @param defProbe The probe to get values from
     * @param customData some custom data
     */
    public void fillGraphDef(RrdGraphDef graphDef, Probe<?, ?> defProbe,
            Map<String, ? extends Plottable> customData) {
        String defRrdName = defProbe.getRrdName();
        HostsList hl = defProbe.getHostList();
        List<DsDesc> toDo = new ArrayList<DsDesc>();
        Set<String> datasources = new HashSet<String>();

        for(DsDesc ds: allds) {
            String rrdName = defRrdName;
            Probe<?,?> probe = defProbe;
            if(ds.dspath != null) {
                if(logger.isTraceEnabled())
                    logger.trace("External probe path: " + ds.dspath.host + "/" + ds.dspath.probe + "/" + ds.dsName);
                probe = hl.getProbeByPath(ds.dspath.host, ds.dspath.probe);
                if(probe == null) {
                    logger.error("Invalide probe: " + ds.dspath.host + "/" + ds.dspath.probe);
                    continue;
                }
                else 
                    rrdName = probe.getRrdName();
            }
            boolean complete = false;
            if(ds.percentile != null) {
                complete = true;
                graphDef.percentile(ds.name, ds.dsName, ds.percentile);
                datasources.add(ds.name);
            }
            else if (ds.graphType.datasource() && ds.rpn == null) {
                // Used to check if the data source is provided one way or another
                //Does the datas existe in the provided values
                if(customData != null && customData.containsKey(ds.dsName)) {
                    complete = true;
                    if( ! datasources.contains(ds.name)) {
                        graphDef.datasource(ds.name, customData.get(ds.dsName));
                        datasources.add(ds.name);
                    }
                }
                //Or they might be on the associated rrd
                else if(probe.dsExist(ds.dsName)) {
                    complete = true;
                    if( ! datasources.contains(ds.name)) {
                        graphDef.datasource(ds.name, rrdName, ds.dsName, ds.cf);                
                        datasources.add(ds.name);
                    }
                }
            }
            //A rpn datasource
            else if (ds.graphType.datasource()) {
                complete = true;
                if(! datasources.contains(ds.name)) {
                    graphDef.datasource(ds.name, ds.rpn);
                    datasources.add(ds.name);
                }
            }
            //No data source, so it's complete
            else {
                complete = true;
            }
            if (complete) {
                toDo.add(ds);
            }
            else {
                logger.debug("Error for " + ds);
                logger.error("No way to plot " + ds.name + " in " + name + " found");
            }
        }
        /*The title line*/
        graphDef.comment(""); //We simulate the color box
        graphDef.comment(manySpace.substring(0, Math.min(maxLengthLegend, manySpace.length()) + 2));
        graphDef.comment("Current");
        graphDef.comment("  Average");
        graphDef.comment("  Minimum");
        graphDef.comment("  Maximum");
        graphDef.comment("\\l");

        if(logger.isTraceEnabled()) {
            logger.trace("Datasource: " + datasources);
            logger.trace("Todo: " + toDo);
        }

        for(DsDesc ds: toDo) {
            ds.graphType.draw(graphDef, ds.name, ds.color);
            if(ds.graphType.legend())
                addLegend(graphDef, ds.name, ds.graphType, ds.legend);
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
    public RrdGraphDef getGraphDef(Probe<?,?> defProbe, Map<String, ? extends Plottable> ownData) throws IOException {
        RrdGraphDef retValue = getEmptyGraphDef();
        fillGraphDef(retValue, defProbe, ownData);
        return retValue;
    }

    /**
     * return the RrdGraphDef for this graph, used the indicated probe
     * any data can be overridden of a provided map of Plottable
     * @param probe
     * @param ownData data used to override probe's own values
     * @return
     * @throws IOException
     * @throws RrdException
     */
    public DataProcessor getPlottedDatas(Probe<?,?> probe, Map<?, ?> ownData, long start, long end) throws IOException {
        DataProcessor retValue = new DataProcessor(start, end);
        String rrdName = probe.getRrdName();

        String lastName = null;
        for(DsDesc ds: allds) {
            boolean stack = ds.graphType == GraphType.STACK;
            boolean plotted = stack || ds.graphType == GraphType.LINE  || ds.graphType == GraphType.AREA;
            if (ds.rpn == null && ds.dsName != null) {
                //Does the datas existe in the provided values
                if(ownData != null && ownData.containsKey(ds.dsName) && ds.graphType == GraphType.LINE) {
                    retValue.addDatasource(ds.name, (Plottable) ownData.get(ds.dsName));
                }
                //Or they might be on the associated rrd
                else if(probe.dsExist(ds.dsName)) {
                    retValue.addDatasource(ds.name, rrdName, ds.dsName, ds.cf);                             
                }
            }
            else if(ds.rpn != null){
                retValue.addDatasource(ds.name, ds.rpn);
            }
            if(plotted && stack) {
                retValue.addDatasource("Plotted" + ds.name, lastName + ", " +  ds.name + ", +");
            }
            else if(plotted) {
                retValue.addDatasource("Plotted" + ds.name, ds.name);
            }
            lastName = ds.name; 
        }
        if(logger.isTraceEnabled()) {
            logger.trace("Datastore for " + getName());
            for(String s: retValue.getSourceNames())
                logger.trace("\t" + s);
        }
        return retValue;
    }

    private void addLegend(RrdGraphDef def, String ds, GraphType gt, String legend) {
        if(legend == null)
            return;
        if(gt == GraphType.PERCENTILELEGEND) {
            def.comment(legend + "\\g");
            int missingLength = Math.min(maxLengthLegend - legend.length(), manySpace.length()) + 2;
            if(missingLength > 0)
                def.comment(manySpace.substring(0, missingLength));
            def.gprint(ds, ConsolFun.MAX, "%6.2f%s");
            def.comment("\\l");
        }
        else if(gt == GraphType.COMMENT) {
            def.comment(legend + "\\l");
        }
        else if(gt != GraphType.NONE) {
            def.comment(legend + "\\g");
            int missingLength = Math.min(maxLengthLegend - legend.length(), manySpace.length()) + 2;
            if(missingLength > 0)
                def.comment(manySpace.substring(0, missingLength));
            def.gprint(ds, ConsolFun.LAST, "%6.2f%s");
            def.gprint(ds, ConsolFun.AVERAGE, "%8.2f%s");
            def.gprint(ds, ConsolFun.MIN, "%8.2f%s");
            def.gprint(ds, ConsolFun.MAX, "%8.2f%s");
            def.comment("\\l");
        }
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
     * @return Returns the height of the graphic zone.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height The height of the graphic zone to set.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return Returns the width of the graphic zone.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width The width of the graphic zone to set.
     */
    public void setWidth(int width) {
        this.width = width;
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
    public LinkedList<String> getViewTree(GraphNode graph) {
        return getTree(graph, viewTree);
    }

    /**
     * @param viewTree The viewTree to set.
     */
    public void setViewTree(List<?> viewTree) {
        this.viewTree = viewTree;
        logger.trace("Adding view tree: " + viewTree);
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
    public LinkedList<String> getHostTree(GraphNode graph) {
        return getTree(graph, hostTree);
    }

    private LinkedList<String> getTree(GraphNode graph, List<?> ElementsTree) {
        LinkedList<String> tree = new LinkedList<String>();
        for (Object o: ElementsTree) {
            if (o instanceof String) {
                String pathElem = jrds.Util.parseTemplate((String) o, graph.getProbe(), this, graph.getProbe().getHost());
                tree.add(pathElem);
            }
            else if (o instanceof PathElement)
                tree.add( ( (PathElement) o).resolve(graph));
        }
        return tree;
    }

    /**
     * @param hostTree The hostTree to set.
     */
    public void setHostTree(List<?> hostTree) {
        this.hostTree = hostTree;
        logger.trace("Adding host tree: " + hostTree);
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

    public static final PathElement resolvPathElement(String name) {
        return PathElement.valueOf(name.toUpperCase());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSiUnit() {
        return siUnit;
    }

    public void setSiUnit(boolean siUnit) {
        this.siUnit = siUnit;
    }

    public void setUnitExponent(String exponent) {
        if("".equals(exponent))
            exponent = SiPrefix.FIXED.name();
        try {
            unitExponent = SiPrefix.valueOf(exponent).getExponent();
        } catch (IllegalArgumentException e1) {
        }
        if(unitExponent == null) {
            try {
                unitExponent = new Integer(exponent);
            } catch (NumberFormatException e) {
                logger.debug("Base unit not identified: " + exponent);
            }
        }
    }

    /**
     * @return the dimension of the graphic object
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * @param dimension the dimension of the graphic object to set
     */
    public void setDimension(int height, int width) {
        dimension = new Dimension();
        dimension.height = height;
        dimension.width = width;
    }

    public int getLegendLines() {
        int numlegend = 0;
        for(DsDesc dd: allds) {
            if (dd.graphType.legend() && dd.legend != null)
                numlegend++;
        }
        return numlegend;
    }

    private class ImageParameters {
        int xsize;
        int ysize;
        int unitslength;
        int xorigin;
        int yorigin;
        int xgif, ygif;
    }

    static private final double LEGEND_LEADING_SMALL = 0.7; // chars
    static private final int PADDING_LEFT = 10; // pix
    static private final int PADDING_TOP = 12; // pix
    static private final int PADDING_TITLE = 6; // pix
    static private final int PADDING_RIGHT = 16; // pix
    static private final int PADDING_PLOT = 2; //chars
    static private final int PADDING_BOTTOM = 6; //pix

    static private final int DEFAULT_UNITS_LENGTH = 9;

    private static final String DUMMY_TEXT = "Dummy";
    private static final Font smallFont = RrdGraphConstants.DEFAULT_SMALL_FONT; // ok
    private static final Font largeFont = RrdGraphConstants.DEFAULT_LARGE_FONT; // ok

    private double getFontHeight(FontRenderContext frc, Font font) {
        LineMetrics lm = font.getLineMetrics(DUMMY_TEXT, frc);
        return lm.getAscent() + lm.getDescent();
    }

    private double getSmallFontHeight(FontRenderContext frc) {
        return getFontHeight(frc, smallFont);
    }

    private double getLargeFontHeight(FontRenderContext frc) {
        return getFontHeight(frc, largeFont);
    }

    private double getStringWidth(Font font, FontRenderContext frc) {
        return font.getStringBounds("a", 0, 1, frc).getBounds().getWidth();
    }

    private double getSmallFontCharWidth(FontRenderContext frc) {
        return getStringWidth(smallFont, frc);
    }

    private double getSmallLeading(FontRenderContext frc) {
        return getSmallFontHeight(frc) * LEGEND_LEADING_SMALL;
    }

    public void initializeLimits(Graphics2D g2d) {
        FontRenderContext frc  = g2d.getFontRenderContext();
        ImageParameters im = new ImageParameters();

        im.xsize = getWidth();
        im.ysize = getHeight();
        im.unitslength = DEFAULT_UNITS_LENGTH;
        im.xorigin = (int) (PADDING_LEFT + im.unitslength * getSmallFontCharWidth(frc));
        im.xorigin += getSmallFontHeight(frc);
        im.yorigin = PADDING_TOP + im.ysize;
        im.yorigin += getLargeFontHeight(frc) + PADDING_TITLE;
        im.xgif = PADDING_RIGHT + im.xsize + im.xorigin;
        im.ygif = im.yorigin + (int) (PADDING_PLOT * getSmallFontHeight(frc));
        im.ygif += ( (int) getSmallLeading(frc) + 5 ) * ( getLegendLines() + 5);
        im.ygif += PADDING_BOTTOM;
        setDimension(im.ygif, im.xgif);
    }

    public void addACL(ACL acl) {
        this.acl = this.acl.join(acl);
    }

    public ACL getACL() {
        return acl;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        GraphDesc newgd =  (GraphDesc) super.clone();
        return newgd;
    }

    public Document dumpAsXml() throws ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = 
            (Element) document.createElement("graphdesc"); 
        document.appendChild(root);
        root.appendChild(document.createElement("name")).setTextContent(name);
        if(graphName != null)
            root.appendChild(document.createElement("graphName")).setTextContent(graphName);
        if(graphTitle != null)
            root.appendChild(document.createElement("graphTitle")).setTextContent(graphTitle);
        if(verticalLabel != null)
            root.appendChild(document.createElement("verticalLabel")).setTextContent(verticalLabel);
        if(! (lowerLimit == 0))
            root.appendChild(document.createElement("lowerLimit")).setTextContent(Double.toString(lowerLimit));
        if(! Double.isNaN(upperLimit))
            root.appendChild(document.createElement("upperLimit")).setTextContent(Double.toString(upperLimit));

        // private List<DsDesc> allds;

        for(DsDesc e: allds) {
            Element specElement = (Element) root.appendChild(document.createElement("add"));
            specElement.appendChild(document.createElement("name")).setTextContent(e.name);
            if(! e.name.equals(e.dsName))
                specElement.appendChild(document.createElement("dsName")).setTextContent(e.dsName);
            specElement.appendChild(document.createElement("graphType")).setTextContent(e.graphType.toString());
            if(e.legend != null)
                specElement.appendChild(document.createElement("legend")).setTextContent(e.legend);

        }
        Element hostTreeElement =  (Element) root.appendChild(document.createElement("hosttree"));
        for(Object o: hostTree) {
            Element pe = document.createElement("pathstring");
            pe.setTextContent(o.toString());
            hostTreeElement.appendChild(pe);
        }
        Element viewTreeElement =  (Element) root.appendChild(document.createElement("viewtree"));
        for(Object o: viewTree) {
            Element pe = document.createElement("pathstring");
            pe.setTextContent(o.toString());
            viewTreeElement.appendChild(pe);
        }
        return document;
    }

}
