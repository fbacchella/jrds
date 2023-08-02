package jrds;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.IPlottable;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jrds.Util.SiPrefix;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;
import jrds.store.ExtractInfo;
import jrds.webapp.ACL;
import jrds.webapp.WithACL;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * A classed used to store the static description of a graph
 * 
 * @author Fabrice Bacchella
 */
public class GraphDesc implements WithACL {
    static final private Logger logger = LoggerFactory.getLogger(GraphDesc.class);

    static public final ConsolFun DEFAULTCF = ConsolFun.AVERAGE;

    // static final private String manySpace =
    // "123456798ABCDEF0123465798ABCDEF0123456798ABCDEF0123465798ABCDEF0123456798ABCDEF0123465798ABCDEF0
    // ";
    static final private String MANYSPACE = "                                                                      ";

    public enum GraphType {
        NONE {
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
        PERCENTILE {
            public String toString() {
                return "percentile";
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
            public String toString() {
                return "legend";
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
            @Override
            public String toString() {
                return "percentile legend";
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
        COMMENT {
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
            public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {
                rgd.line(sn, color, legend);
            }

            @Override
            public String toString() {
                return "line";
            }

            public boolean datasource() {
                return true;
            }

            public boolean toPlot() {
                return true;
            }

            public boolean legend() {
                return true;
            }
        },
        LINENOLEGEND {
            public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {
                rgd.line(sn, color, legend);
            }

            @Override
            public String toString() {
                return "line";
            }

            public boolean datasource() {
                return true;
            }

            public boolean toPlot() {
                return true;
            }

            public boolean legend() {
                return false;
            }
        },
        AREA {
            public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {
                rgd.area(sn, color, legend);
            }

            @Override
            public String toString() {
                return "area";
            }

            public boolean datasource() {
                return true;
            }

            public boolean toPlot() {
                return true;
            }

            public boolean legend() {
                return true;
            }
        },
        STACK {
            public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {
                rgd.stack(sn, color, legend);
            }

            @Override
            public String toString() {
                return "stack";
            }

            public boolean datasource() {
                return true;
            }

            public boolean toPlot() {
                return true;
            }

            public boolean legend() {
                return true;
            }
        };

        public void draw(RrdGraphDef rgd, String sn, Color color, String legend) {
        }

        /**
         * To check if it will generate a plot, for color calculation
         * 
         * @return
         */
        public abstract boolean toPlot();

        public abstract boolean datasource();

        /**
         * To check if it will generate a line in the legend block
         * 
         * @return
         */
        public abstract boolean legend();
    }

    // Old name kept
    @Deprecated
    static final public GraphType NONE = GraphType.NONE;
    @Deprecated
    static final public GraphType DATASOURCE = GraphType.NONE;
    @Deprecated
    static final public GraphType LINE = GraphType.LINE;
    @Deprecated
    static final public GraphType AREA = GraphType.AREA;
    @Deprecated
    static final public GraphType STACK = GraphType.STACK;
    @Deprecated
    static final public GraphType COMMENT = GraphType.COMMENT;

    private enum PathElement {
        HOST {
            public String resolve(GraphNode graph) {
                return graph.getProbe().getHost().getName();
            }
        },
        TITLE {
            public String resolve(GraphNode graph) {
                return graph.getGraphTitle();
            }
        },
        INDEX {
            public String resolve(GraphNode graph) {
                StringBuilder retValue = new StringBuilder("empty");
                if(graph.getProbe() instanceof IndexedProbe) {
                    retValue.setLength(0);
                    IndexedProbe ip = (IndexedProbe) graph.getProbe();
                    retValue.append(ip.getIndexName());
                    // Check to see if a label is defined and needed to add
                    String label = graph.getProbe().getLabel();
                    if(label != null) {
                        retValue.append(" (").append(label).append(")");
                    }
                } else {
                    logger.debug("Bad graph definition for {}", graph);
                }
                return retValue.toString();
            }
        },
        URL {
            public String resolve(GraphNode graph) {
                String url = "";
                Probe<?, ?> probe = graph.getProbe();
                if(probe instanceof UrlProbe) {
                    url = ((UrlProbe) probe).getUrlAsString();
                }
                return url;
            }
        },
        JDBC {
            public String resolve(GraphNode graph) {
                return ((UrlProbe) graph.getProbe()).getUrlAsString();
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
        DATABASE {
            public String resolve(GraphNode graph) {
                return "Databases";
            }
        },
        DBINSTANCE {
            public String resolve(GraphNode graph) {
                UrlProbe dbprobe = (UrlProbe) graph.getProbe();
                return dbprobe.getUrlAsString();
            }
        };
        public abstract String resolve(GraphNode graph);
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
        // 240°
        BLUE {
            @Override
            public Color getColor() {
                return Color.BLUE;
            }
        },
        // 120°
        GREEN {
            @Override
            public Color getColor() {
                return Color.GREEN;
            }
        },
        // 0°
        RED {
            @Override
            public Color getColor() {
                return Color.RED;
            }
        },
        // 180°
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
        // 180°
        TEAL {
            @Override
            public Color getColor() {
                return new Color(0, 128, 128);
            }
        },
        // 60°
        YELLOW {
            @Override
            public Color getColor() {
                return Color.YELLOW;
            }
        },
        // 300°
        MAGENTA {
            @Override
            public Color getColor() {
                return Color.MAGENTA;
            }
        },
        // 0°
        PINK {
            @Override
            public Color getColor() {
                return Color.PINK;
            }
        },
        // 0°
        BLACK {
            @Override
            public Color getColor() {
                return Color.BLACK;
            }
        },
        NAVY {
            @Override
            public Color getColor() {
                return new Color(0, 0, 128);
            }
        },
        // 0°
        GRAY {
            @Override
            public Color getColor() {
                return Color.GRAY;
            }
        },
        // 0°
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
                return new Color(255, 0, 255);
            }
        },
        // Netscape alias for cyan
        AQUA {
            @Override
            public Color getColor() {
                return Color.CYAN;
            }
        },
        LIME {
            @Override
            public Color getColor() {
                return new Color(204, 255, 0);
            }
        },
        MAROON {
            @Override
            public Color getColor() {
                return new Color(128, 0, 0);
            }
        },
        OLIVE {
            @Override
            public Color getColor() {
                return new Color(128, 128, 0);
            }
        },
        PURPLE {
            @Override
            public Color getColor() {
                return new Color(128, 0, 128);
            }
        },
        SILVER {
            @Override
            public Color getColor() {
                return new Color(192, 192, 192);
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

        public static Color resolveIndex(int i) {
            return Colors.values()[i % Colors.length].getColor();
        }
    }

    private static final Map<Color, String> colornames;
    static {
        Map<Color, String> _colornames = new HashMap<>(Colors.length);
        for (Colors i: Colors.values()) {
            _colornames.put(i.getColor(), i.name().toLowerCase());
        }
        colornames = Collections.unmodifiableMap(_colornames);
    }

    @ToString @EqualsAndHashCode
    static class DsPath {
        public final String host;
        public final String probe;

        DsPath(String host, String probe) {
            this.host = host;
            this.probe = probe;
        }

    }

    public static DsDescBuilder getDsDescBuilder() {
        return new DsDescBuilder();
    }

    private static final Pattern COLORSTRINGPATTERN = Pattern.compile("^#[0-9A-F]{6,8}");
    public static class DsDescBuilder {
        @Setter @Accessors(chain=true)
        private String name;
        @Setter @Accessors(chain=true)
        private String dsName;
        @Setter @Accessors(chain=true)
        private String rpn;
        @Setter @Accessors(chain=true)
        private GraphType graphType;
        @Setter @Accessors(chain=true)
        private Color color;
        @Setter @Accessors(chain=true)
        private String legend;
        @Setter @Accessors(chain=true)
        private ConsolFun cf;
        @Setter @Accessors(chain=true)
        private Integer percentile;
        @Setter @Accessors(chain=true)
        private DsPath dspath;
        @Setter @Accessors(chain=true)
        private boolean reversed;

        private DsDescBuilder() {
        }

        public DsDescBuilder setPath(String host, String probe) {
            this.dspath = new DsPath(host, probe);
            return this;
        }

        public DsDescBuilder setColorString(String colorName) {
            if(colorName != null && COLORSTRINGPATTERN.matcher(colorName).matches()) {
                int r = Integer.parseInt(colorName.substring(1, 3), 16);
                int g = Integer.parseInt(colorName.substring(3, 5), 16);
                int b = Integer.parseInt(colorName.substring(5, 7), 16);
                int alpha = 255;
                if (colorName.length() == 9) {
                    alpha = Integer.parseInt(colorName.substring(7, 9), 16);
                }
                color = new Color(r, g, b, alpha);
            } else if(colorName != null && !"".equals(colorName)) {
                color = Colors.valueOf(colorName.toUpperCase()).getColor();
                if(color == null) {
                    color = Color.getColor(colorName);
                }
                if(color == null) {
                    logger.error("Cannot read color {}", colorName);
                    color = Color.white;
                }
            }
            return this;
        }
    }

    public static class DsDesc {
        public final String name;
        public final String dsName;
        public final String rpn;
        public final GraphType graphType;
        public final Color color;
        public final String legend;
        public final ConsolFun cf;
        public final Integer percentile;
        public final DsPath dspath;

        private DsDesc(String name, String dsName, String rpn,
               GraphType graphType, Color color, String legend,
               ConsolFun cf, DsPath dspath) {
            this.name = name;
            this.dsName = dsName;
            this.rpn = rpn;
            this.percentile = null;
            this.graphType = graphType;
            this.color = color;
            this.legend = legend;
            this.cf = cf;
            this.dspath = dspath;
        }

        private DsDesc(String name, String rpn, GraphType graphType, Color color, String legend) {
            this.name = name;
            this.rpn = rpn;
            this.graphType = graphType;
            this.color = color;
            this.legend = legend;
            this.dsName = null;
            this.cf = null;
            this.percentile = null;
            this.dspath = null;
        }

        private DsDesc(String name, String dsName, Integer percentile, GraphType graphType, Color color) {
            this.name = name;
            this.dsName = dsName;
            this.percentile = percentile;
            this.graphType = graphType;
            this.color = color;
            this.rpn = null;
            this.legend = null;
            this.cf = null;
            this.dspath = null;
        }

        private  DsDesc(String dsName, GraphType graphType, String legend, ConsolFun cf) {
            this.name = dsName;
            this.dsName = dsName;
            this.graphType = graphType;
            this.legend = legend;
            this.cf = cf;
            this.rpn = null;
            this.color = null;
            this.percentile = null;
            this.dspath = null;
        }

        public String toString() {
            String colorString;
            if (color instanceof Color) {
                Color c = (Color) color;
                colorString = String.format("Color[%d, %d, %d]", c.getRed(), c.getGreen(), c.getBlue());
            } else if (color != null) {
                colorString = color.toString();
            } else {
                colorString = "None";
            }
            return String.format("DsDesc(%s,[%s/%s/%s],\"%s\",%s,%s,\"%s\",%s)",
                                 name,
                                 (dspath == null ? "" : dspath.host),
                                 (dspath == null ? "" : dspath.probe),
                                 (dsName == null ? "" : dsName),
                                 (rpn == null ? "" : rpn),
                                 graphType,
                                 colorString,
                                 (legend == null ? "" : legend),
                                 cf
                            );
        }
    }

    public static class Builder {
        @Setter @Accessors(chain=true)
        private List<DsDesc> allds = new ArrayList<>();
        @Setter @Accessors(chain=true)
        private List<DsDescBuilder> descbuilders = null;
        @Setter @Accessors(chain=true)
        private int width = 578;
        @Setter @Accessors(chain=true)
        private int height = 206;
        @Setter @Accessors(chain=true)
        private double upperLimit = Double.NaN;
        @Setter @Accessors(chain=true)
        private double lowerLimit = 0;
        @Setter @Accessors(chain=true)
        private String verticalLabel = null;
        @Setter @Accessors(chain=true)
        private Map<String, List<?>> trees = new HashMap<>(2);
        @Setter @Accessors(chain=true)
        private String graphName;
        @Setter @Accessors(chain=true)
        private String name;
        @Setter @Accessors(chain=true)
        private String graphTitle = "${graphdesc.name} on ${host}";
        @Setter @Accessors(chain=true)
        private boolean siUnit = true;
        @Setter @Accessors(chain=true)
        private boolean logarithmic = false;
        @Setter @Accessors(chain=true)
        private Integer unitExponent = null;
        @Setter @Accessors(chain=true)
        private boolean withLegend = true;
        @Setter @Accessors(chain=true)
        private boolean withSummary = true;
        @Setter @Accessors(chain=true)
        private ACL acl = ACL.ALLOWEDACL;
        @Setter @Accessors(chain=true)
        private Class<Graph> graphClass = Graph.class;
        private int maxLengthLegend = 0;

        public Builder fromGraphDesc(GraphDesc parent) {
            allds = new ArrayList<>(parent.allds);
            width = parent.width;
            height = parent.height;
            upperLimit = parent.upperLimit;
            lowerLimit = parent.lowerLimit;
            verticalLabel = parent.verticalLabel;
            trees = new HashMap<>(parent.trees.size());
            parent.trees.forEach((k, v) -> trees.put(k, new ArrayList<Object>(v)));
            graphName = parent.graphName;
            name = parent.name;
            maxLengthLegend = parent.maxLengthLegend;
            graphTitle = parent.graphTitle;
            siUnit = parent.siUnit;
            logarithmic = parent.logarithmic;
            unitExponent = parent.unitExponent;
            withLegend = parent.withLegend;
            withSummary = parent.withSummary;
            acl = parent.acl;
            graphClass =parent.graphClass;

            return this;
        }

        public Builder emptyDs() {
            allds = null;
            return this;
        }

        public Builder emptyTrees() {
            trees = new HashMap<>();
            return this;
        }

        public Builder addTree(String name, List<?> hierarchy) {
            if(trees == null) {
                emptyTrees();
            }
            trees.put(name, hierarchy);
            return this;
        }

        public Builder addTree(String name, String... hierarchy) {
            if(trees == null) {
                emptyTrees();
            }
            trees.put(name, Arrays.asList(hierarchy));
            return this;
        }

        public Builder addDsDesc(DsDescBuilder builder) {
            if(descbuilders == null) {
                descbuilders = new ArrayList<>();
            }
            descbuilders.add(builder);
            return this;
        }

        public GraphDesc build() {
            return new GraphDesc(this);
        }
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    private List<DsDesc> allds = null;
    private int width = 578;
    private int height = 206;
    private double upperLimit = Double.NaN;
    private double lowerLimit = 0;
    private String verticalLabel = null;
    private int lastColor = 0;
    private Map<String, List<?>> trees = new HashMap<>(2);
    private String graphName;
    private String name;
    private String graphTitle = "${graphdesc.name} on ${host}";
    private int maxLengthLegend = 0;
    private boolean siUnit = true;
    private boolean logarithmic = false;
    private Integer unitExponent = null;
    private boolean withLegend = true; // To show the values block under the
    // graph
    private boolean withSummary = true; // To show the summary with last update,
    // period, etc. information block
    private ACL acl = ACL.ALLOWEDACL;
    private Class<Graph> graphClass = Graph.class;

    public static class Dimension {
        public int width = 0;
        public int height = 0;
    }

    private Dimension dimension = null;

    public GraphDesc() {
        allds = new ArrayList<>();
    }

    public GraphDesc(Builder builder) {
        allds = new ArrayList<>(builder.allds);
        width = builder.width;
        height = builder.height;
        upperLimit = builder.upperLimit;
        lowerLimit = builder.lowerLimit;
        verticalLabel = builder.verticalLabel;
        if(builder.trees != null) {
            trees = new HashMap<>(builder.trees.size());
            builder.trees.forEach((k,v) -> trees.put(k, new ArrayList<>(v)));
        }
        graphName = builder.graphName;
        maxLengthLegend = builder.maxLengthLegend;
        name = builder.name;
        graphTitle = builder.graphTitle;
        siUnit = builder.siUnit;
        logarithmic = builder.logarithmic;
        unitExponent = builder.unitExponent;
        withLegend = builder.withLegend;
        withSummary = builder.withSummary;
        acl = builder.acl;
        graphClass =builder.graphClass;
        if(builder.descbuilders != null) {
            builder.descbuilders.forEach(this::add);
        }
    }

    public void add(DsDescBuilder builder) {
        Color bcolor = builder.color;
        GraphType bgt = builder.graphType;
        if(bgt == null) {
            if(builder.legend != null) {
                bgt = GraphType.COMMENT;
            }
            else {
                bgt = GraphType.NONE;
            }
        }
        if(bgt.toPlot() && bcolor == null) {
            bcolor = Colors.resolveIndex(lastColor++);
        }
        String bname = builder.name;
        String bdsname = builder.dsName;
        // dsName unknown, try to extract from name
        if (bdsname == null && bgt.datasource() && builder.rpn == null && bname != null) {
            bdsname = bname;
        }
        // Unknown name, where to get it from ?
        if (bname == null) {
            // If the name is missing, generate one ?
            if (builder.rpn != null){
                bname = Util.stringSignature(builder.rpn);
            } else if (bdsname != null){
                bname = bdsname;
            } else {
                bname = Integer.toHexString(ThreadLocalRandom.current().nextInt());
            }
        }
        String blegend = builder.legend;
        // Auto generated legend
        if(blegend == null && bgt.legend()) {
            blegend = bname;
        }
        if(builder.reversed) {
            String revRpn = "0, " + bname + ", -";
            allds.add(new DsDesc(bname, bdsname, builder.rpn, GraphType.NONE, null, null, builder.cf, builder.dspath));
            allds.add(new DsDesc("rev_" + bname, revRpn, bgt, bcolor, null));
            allds.add(new DsDesc(bname, GraphType.LEGEND, blegend, builder.cf));
        } else {
            allds.add(new DsDesc(bname, bdsname, builder.rpn, bgt, bcolor, blegend, builder.cf, builder.dspath));
        }
        if(builder.percentile != null) {
            String percentileName = "percentile" + builder.percentile + "_" + name;
            String percentileLegend = builder.percentile + "th percentile";
            Color percentilColor = ((Color)bcolor).darker();
            if(!builder.reversed) {
                allds.add(new DsDesc(percentileName, name, builder.percentile, GraphType.LINE, percentilColor));
            } else {
                String revPercentilRpn = "0, " + percentileName + ", -";
                allds.add(new DsDesc(percentileName, bname, builder.percentile, GraphType.NONE, null));
                allds.add(new DsDesc("rev_" + percentileName, revPercentilRpn, GraphType.LINE, percentilColor, null));
            }
            allds.add(new DsDesc(percentileName, GraphType.PERCENTILELEGEND, percentileLegend, builder.cf));
            maxLengthLegend = Math.max(maxLengthLegend, percentileLegend.length());
        }
        if(blegend != null) {
            maxLengthLegend = Math.max(maxLengthLegend, blegend.length());
        }
    }

    public RrdGraphDef getEmptyGraphDef() {
        RrdGraphDef retValue = new RrdGraphDef(1, 2);
        if(!Double.isNaN(lowerLimit))
            retValue.setMinValue(lowerLimit);
        if(!Double.isNaN(upperLimit))
            retValue.setMaxValue(upperLimit);
        if(verticalLabel != null)
            retValue.setVerticalLabel(verticalLabel);
        if(this.siUnit)
            retValue.setBase(1000);
        else
            retValue.setBase(1024);
        if(unitExponent != null) {
            retValue.setUnitsExponent(unitExponent);
        }
        retValue.setLogarithmic(logarithmic);
        retValue.setAntiAliasing(true);
        retValue.setTextAntiAliasing(true);
        retValue.setImageFormat("PNG");
        retValue.setWidth(getWidth());
        retValue.setHeight(getHeight());
        return retValue;
    }

    /**
     * Fill a GraphDef with values as defined by the graph desc
     * 
     * @param graphDef the GraphDef to configure
     * @param defProbe The probe to get values from
     * @param customData some custom data, they override existing values in the
     *            associated probe
     */
    public void fillGraphDef(RrdGraphDef graphDef, Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> customData) {
        List<DsDesc> toDo = DatasourcesPopulator.populate(graphDef, defProbe, ei, customData, allds, name);
        // The title line, only if values block is required
        if(withSummary) {
            graphDef.comment(""); // We simulate the color box
            graphDef.comment(MANYSPACE.substring(0, Math.min(maxLengthLegend, MANYSPACE.length() - 2) + 4));
            graphDef.comment("Current");
            graphDef.comment("  Average");
            graphDef.comment("  Minimum");
            graphDef.comment("  Maximum");
            graphDef.comment("\\l");
        }

        String shortLegend = withSummary ? " \\g" : null;
        for(DsDesc ds: toDo) {
            ds.graphType.draw(graphDef, ds.name, ds.color, shortLegend);
            if(withSummary && ds.graphType.legend())
                addLegend(graphDef, ds.name, ds.graphType, ds.legend);
        }

    }

    /**
     * return the RrdGraphDef for this graph, used the indicated probe any data
     * can be overridden of a provided map of {@link org.rrd4j.data.Plottable}
     * 
     * @param defProbe
     * @param ownData data used to override probe's own values
     * @return
     */
    public RrdGraphDef getGraphDef(Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> ownData) {
        RrdGraphDef retValue = getEmptyGraphDef();
        fillGraphDef(retValue, defProbe, ei, ownData);
        return retValue;
    }

    protected void addLegend(RrdGraphDef def, String ds, GraphType gt, String legend) {
        if(legend == null)
            return;
        if(gt == GraphType.PERCENTILELEGEND) {
            def.comment(legend + "\\g");
            int missingLength = Math.min(maxLengthLegend - legend.length(), MANYSPACE.length()) + 2;
            if(missingLength > 0)
                def.comment(MANYSPACE.substring(0, missingLength));
            def.datasource(ds + "_MAX", ds, new Variable.MAX());
            def.gprint(ds + "_MAX", "%8.2f%s");
            def.comment("\\l");
        } else if(gt == GraphType.COMMENT) {
            def.comment(legend + "\\l");
        } else if(gt != GraphType.NONE) {
            def.comment(legend + "\\g");
            int missingLength = Math.min(maxLengthLegend - legend.length(), MANYSPACE.length()) + 2;
            if(missingLength > 0)
                def.comment(MANYSPACE.substring(0, missingLength));
            def.datasource(ds + "_LAST", ds, new Variable.LAST());
            def.gprint(ds + "_LAST", "%8.2f%s");
            def.datasource(ds + "_AVERAGE", ds, new Variable.AVERAGE());
            def.gprint(ds + "_AVERAGE", "%8.2f%s");
            def.datasource(ds + "_MIN", ds, new Variable.MIN());
            def.gprint(ds + "_MIN", "%8.2f%s");
            def.datasource(ds + "_MAX", ds, new Variable.MAX());
            def.gprint(ds + "_MAX", "%8.2f%s");
            def.comment("\\l");
        }
    }

    /**
     * return the RrdGraphDef for this graph, used the indicated probe any data
     * can be overridden of a provided map of Plottable
     * 
     * @param defProbe
     * @param ei
     * @param customData data used to override probe's own values
     * @return
     */
    public DataProcessor getPlottedDatas(Probe<?, ?> defProbe, ExtractInfo ei, Map<String, IPlottable> customData) {
        return DatasourcesPopulator.populate(defProbe, ei, customData, allds, name);
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
    public List<String> getViewTree(GraphNode graph) {
        return getTree(graph, PropertiesManager.VIEWSTAB);
    }

    /**
     * @return Returns the hostTree.
     */
    public List<String> getHostTree(GraphNode graph) {
        return getTree(graph, PropertiesManager.HOSTSTAB);
    }

    public List<String> getTree(GraphNode graph, String tabname) {
        List<?> elementsTree = trees.get(tabname);
        if(elementsTree == null) {
            return Collections.emptyList();
        }
        List<String> tree = new ArrayList<>(elementsTree.size());
        for(Object o: elementsTree) {
            if(o instanceof String) {
                String pathElem = jrds.Util.parseTemplate((String) o, graph.getProbe(), this, graph.getProbe().getHost());
                tree.add(pathElem);
            } else if(o instanceof PathElement)
                tree.add(((PathElement) o).resolve(graph));
        }
        return tree;
    }

    public void addTree(String tab, List<?> tree) {
        trees.put(tab, tree);
        logger.trace("Adding tree {} to tab {}", tree, tab);
    }

    public void setTree(String tab, List<?> tree) {
        addTree(tab, tree);
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

    public static PathElement resolvPathElement(String name) {
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
            throw new RuntimeException("wrong unit exponent: " + exponent);
        }
        if(unitExponent == null) {
            try {
                unitExponent = Integer.getInteger(exponent);
            } catch (NumberFormatException e) {
                logger.debug("Base unit not identified: {}", exponent);
            }
        }
    }

    public Integer getUnitExponent() {
        return unitExponent;
    }

    /**
     * @return the dimension of the graphic object
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * @param height the height of the graphic object to set
     * @param width the height of the graphic object to set
     */
    public void setDimension(int height, int width) {
        dimension = new Dimension();
        dimension.height = height;
        dimension.width = width;
    }

    public int getLegendLines() {
        int numlegend = 0;
        for(DsDesc dd: allds) {
            if(dd.graphType.legend() && dd.legend != null && withSummary)
                numlegend++;
        }
        return numlegend;
    }

    private static final class ImageParameters {
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
    static private final int PADDING_PLOT = 2; // chars
    static private final int PADDING_BOTTOM = 6; // pix

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
        FontRenderContext frc = g2d.getFontRenderContext();
        ImageParameters im = new ImageParameters();
        int summaryLines = withSummary ? 5 : 0;

        im.xsize = getWidth();
        im.ysize = getHeight();
        im.unitslength = DEFAULT_UNITS_LENGTH;
        im.xorigin = (int) (PADDING_LEFT + im.unitslength * getSmallFontCharWidth(frc));
        im.xorigin += getSmallFontHeight(frc);
        im.yorigin = PADDING_TOP + im.ysize;
        if(graphTitle != null && !"".equals(graphTitle))
            im.yorigin += getLargeFontHeight(frc) + PADDING_TITLE;
        im.xgif = PADDING_RIGHT + im.xsize + im.xorigin;
        im.ygif = im.yorigin + (int) (PADDING_PLOT * getSmallFontHeight(frc));
        im.ygif += ((int) getSmallLeading(frc) + summaryLines) * (getLegendLines() + summaryLines);
        im.ygif += PADDING_BOTTOM;
        setDimension(im.ygif, im.xgif);
    }

    public void addACL(ACL acl) {
        this.acl = this.acl.join(acl);
    }

    public ACL getACL() {
        return acl;
    }

    public Document dumpAsXml() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("graphdesc");
        document.appendChild(root);
        root.appendChild(document.createElement("name")).setTextContent(name);
        if(graphName != null)
            root.appendChild(document.createElement("graphName")).setTextContent(graphName);
        if(graphClass != null) {
            root.appendChild(document.createElement("graphClass")).setTextContent(graphClass.getCanonicalName());
        }
        if(graphTitle != null)
            root.appendChild(document.createElement("graphTitle")).setTextContent(graphTitle);
        Element unit = document.createElement("unit");
        if(siUnit) {
            unit.appendChild(document.createElement("SI"));
        } else {
            unit.appendChild(document.createElement("binary"));
        }
        if(unitExponent != null) {
            for(SiPrefix unity: SiPrefix.values()) {
                if(unitExponent == unity.getExponent()) {
                    String suffix = unity.toString();
                    if(unity == SiPrefix.FIXED) {
                        suffix = "";
                    }
                    unit.appendChild(document.createElement("base")).setTextContent(suffix);
                    break;
                }
            }
        }
        root.appendChild(unit);
        if(verticalLabel != null)
            root.appendChild(document.createElement("verticalLabel")).setTextContent(verticalLabel);
        if(!(lowerLimit == 0))
            root.appendChild(document.createElement("lowerLimit")).setTextContent(Double.toString(lowerLimit));
        if(!Double.isNaN(upperLimit))
            root.appendChild(document.createElement("upperLimit")).setTextContent(Double.toString(upperLimit));
        if(logarithmic)
            root.appendChild(document.createElement("logarithmic"));
        int i = 0;
        // it will contain the number of dsdesc to skip
        int skip = 0;
        for(DsDesc curs: allds) {
            DsDesc e = curs;
            if(skip-- > 0) {
                i++;
                continue;
            }
            boolean reversed = false;
            if(i + 2 <= allds.size() && allds.get(i + 1).name.startsWith("rev_")) {
                reversed = true;
                skip = 2;
                DsDesc rev = allds.get(i + 1);
                DsDesc leg = allds.get(i + 2);
                e = new DsDesc(curs.name, curs.dsName, curs.rpn, rev.graphType, rev.color, leg.legend, curs.cf, null);
            }
            Element specElement = (Element) root.appendChild(document.createElement("add"));
            specElement.appendChild(document.createElement("name")).setTextContent(e.name);
            if(!e.name.equals(e.dsName) && e.dsName != null)
                specElement.appendChild(document.createElement("dsName")).setTextContent(e.dsName);
            if(e.rpn != null) {
                specElement.appendChild(document.createElement("rpn")).setTextContent(e.rpn);
            }
            if(reversed) {
                specElement.appendChild(document.createElement("reversed"));
            }
            specElement.appendChild(document.createElement("graphType")).setTextContent(e.graphType.toString());
            if(e.color != null && e.color instanceof Color) {
                Color c = (Color) e.color;
                String colorString = colornames.get(c);
                if (colorString == null) {
                    colorString = String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
                }
                specElement.appendChild(document.createElement("color")).setTextContent(colorString);
            }
            if(e.legend != null)
                specElement.appendChild(document.createElement("legend")).setTextContent(e.legend);
            i++;
        }
        for(Map.Entry<String, List<?>> e: trees.entrySet()) {
            Element hostTreeElement = (Element) root.appendChild(document.createElement("tree"));
            hostTreeElement.setAttribute("tab", e.getKey());
            for(Object o: e.getValue()) {
                Element pe = document.createElement("pathstring");
                pe.setTextContent(o.toString());
                hostTreeElement.appendChild(pe);
            }
        }
        return document;
    }

    /**
     * @return the logarithmic
     */
    public boolean isLogarithmic() {
        return logarithmic;
    }

    /**
     * @param logarithmic the logarithmic to set
     */
    public void setLogarithmic(boolean logarithmic) {
        this.logarithmic = logarithmic;
    }

    /**
     * @return the withLegend
     */
    public boolean withLegend() {
        return withLegend;
    }

    /**
     * @param withLegend the withLegend to set
     */
    public void setWithLegend(boolean withLegend) {
        this.withLegend = withLegend;
    }

    /**
     * @return the withValues
     */
    public boolean withSummary() {
        return withSummary;
    }

    /**
     * @param withSummary the withValues to set
     */
    public void setWithSummary(boolean withSummary) {
        this.withSummary = withSummary;
    }

    /**
     * @return the graphClass
     */
    public Class<Graph> getGraphClass() {
        return graphClass;
    }

    /**
     * @param graphClass the graphClass to set
     */
    public void setGraphClass(Class<Graph> graphClass) {
        this.graphClass = graphClass;
    }

    /**
     * Return an unmodifiable list of the graph descriptions elements
     * @return the list
     * @since 20190116
     */
    public List<DsDesc> getGraphElements() {
        return Collections.unmodifiableList(allds);
    }

}
