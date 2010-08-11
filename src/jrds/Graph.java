package jrds;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import jrds.webapp.ACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class Graph implements WithACL {
	static final private Logger logger = Logger.getLogger(Graph.class);

	static final private SimpleDateFormat lastUpdateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	static private final SimpleDateFormat exportDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private GraphNode node;
	private Date start;
	private Date end;
	private double max = Double.NaN;
	private double min = Double.NaN;
	private ACL acl = ACL.ALLOWEDACL;

	public Graph(GraphNode node) {
		super();
		this.node = node;
		addACL(node.getACL());
	}

	public Graph(GraphNode node, Date begin, Date start) {
		this.node = node;
		addACL(node.getACL());
		setStart(start);
		setEnd(end);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((end == null) ? 0 : end.hashCode());
		result = PRIME * result + ((start == null) ? 0 : start.hashCode());
		long temp;
		temp = Double.doubleToLongBits(max);
		result = PRIME * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(min);
		result = PRIME * result + (int) (temp ^ (temp >>> 32));
		result = PRIME * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Graph other = (Graph) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max))
			return false;
		if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	/**
	 * Add the graph formating information to a RrdGraphDef
	 * @param graphDef
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws RrdException
	 */
	private RrdGraphDef graphFormat(RrdGraphDef graphDef) {
		Date lastUpdate = node.getProbe().getLastUpdate();
		graphDef.setAntiAliasing(true);
		graphDef.setTextAntiAliasing(true);
		graphDef.setTitle(node.getGraphTitle());
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.comment("Last update: " + 
				lastUpdateFormat.format(lastUpdate) + "\\L");
		String unit = "SI";
		if(! node.getGraphDesc().isSiUnit()) 
			unit = "binary";
		graphDef.comment("Unit type: " + unit + "\\r");
		graphDef.comment("Period from " + lastUpdateFormat.format(start) +
				" to " + lastUpdateFormat.format(end) + "\\L");
		graphDef.comment("Source type: " + node.getProbe().getSourceType() + "\\r");
		graphDef.setImageFormat("PNG");
		graphDef.setFilename("-");
		return graphDef;		
	}

	public RrdGraphDef getRrdGraphDef() throws IOException {
		return node.getGraphDesc().getGraphDef(node.getProbe());
	}

	public RrdGraph getRrdGraph() throws
	IOException {
		GraphDesc gd = node.getGraphDesc();
		RrdGraphDef tempGraphDef = getRrdGraphDef();
		tempGraphDef = graphFormat(tempGraphDef);

		try {
			tempGraphDef.setStartTime(start.getTime()/1000);
			tempGraphDef.setEndTime(Util.endDate(node.getProbe(), end).getTime()/1000);
		} catch (IllegalArgumentException e) {
			logger.error("Impossible to create graph definition, invalid date definition from " + start + " to " + end + " : " + e);
		}
		if( ! Double.isNaN(max) && ! Double.isNaN(min) ) {
			tempGraphDef.setMaxValue(max);
			tempGraphDef.setMinValue(min);
			tempGraphDef.setRigid(true);
		}
		tempGraphDef.setWidth(gd.getWidth());
		tempGraphDef.setHeight(gd.getHeight());
		RrdGraph graph;
		graph = new RrdGraph(tempGraphDef);
		return graph;
	}

	public void writePng(OutputStream out) throws IOException {
		byte[] buffer = getRrdGraph().getRrdGraphInfo().getBytes();
		out.write(buffer);
	}

	public void writeCsv(OutputStream out){
		try {
			DataProcessor dp = node.getGraphDesc().getPlottedDatas(node.getProbe(), null, start.getTime() / 1000, end.getTime() / 1000);
			dp.processData();
			String sources[] = dp.getSourceNames();
			StringBuilder sourcesline = new StringBuilder();
			sourcesline.append("Date,");
			for(String name: sources) {
				if(! name.startsWith("rev_"))
					sourcesline.append(name + ",");
			}
			sourcesline.deleteCharAt(sourcesline.length() - 1);
			sourcesline.append("\r\n");
			out.write(sourcesline.toString().getBytes());
			double[][] values = dp.getValues();
			long[] ts = dp.getTimestamps();
			for(int i=0; i < ts.length; i++) {
				sourcesline.setLength(0);
				sourcesline.append(exportDateFormat.format(org.rrd4j.core.Util.getDate(ts[i])) + ",");
				for(int j = 0; j < sources.length; j++) {
					if(! sources[j].startsWith("rev_"))
						sourcesline.append(values[j][i]+",");
				}
				sourcesline.deleteCharAt(sourcesline.length() - 1);
				sourcesline.append("\r\n");
				out.write(sourcesline.toString().getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getPngName() {
		return node.getName().replaceAll("/","_").replaceAll(" ","_") + ".png";
	}

	/**
	 * Return a uniq name for the graph
	 * @return
	 */
	public String getQualifieName() {
		return node.getQualifieName();
	}

	/**
	 * @return the node
	 */
	public GraphNode getNode() {
		return node;
	}

	/**
	 * @return the max
	 */
	public double getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(double max) {
		this.max = max;
	}

	/**
	 * @return the min
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(double min) {
		this.min = min;
	}

	/**
	 * @return the start
	 */
	public Date getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(Date start) {
		long step = getNode().getProbe().getStep();
		this.start = new Date(org.rrd4j.core.Util.normalize(start.getTime() / 1000L, step) * 1000L);
	}

	/**
	 * @return the end
	 */
	public Date getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(Date end) {
		//We normalize the last update time, it can't be used directly
		this.end = Util.endDate(node.getProbe(), end);
	}

	public void setPeriod(Period p) {
		logger.trace("Period for graph:" + p);
		setStart(p.getBegin());
		setEnd(p.getEnd());
	}

	public void addACL(ACL acl) {
		this.acl = this.acl.join(acl);
	}

	public ACL getACL() {
		return acl;
	}
}
