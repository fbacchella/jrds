/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jrds.webapp.ParamsBean;

import org.apache.log4j.Logger;


/**
 *
 * @author Fabrice Bacchella
 * @version $Revision$ $Date$
 */
public class GraphTree {
	static public final int LEAF_GRAPHTITLE = 1;
	static public final int LEAF_HOSTNAME = 2;
	static final private Logger logger = Logger.getLogger(GraphTree.class);

	static final Comparator<String> nodeComparator = new Comparator<String>() {
		public  int compare(String arg0, String arg1) {
			return String.CASE_INSENSITIVE_ORDER.compare(arg0, arg1);
		}
	};

	private GraphTree parent;
	private Map<Integer, GraphTree> pathsMap;
	//The node's name
	private String name;
	//The childs
	private Map<String, GraphTree> childsMap;
	//The graphs in this node
	private Map<String, GraphNode> graphsSet;

	/**
	 *  Private constructor, no one can generate an graph on the fly
	 *  
	 */
	private GraphTree(String name) {
		graphsSet = new TreeMap<String, GraphNode>(String.CASE_INSENSITIVE_ORDER);
		childsMap = new TreeMap<String, GraphTree>(nodeComparator);
		this.name = name;
	}

	/**
	 * The only way to get a new graph
	 * @param root the graph's root name
	 * @return
	 */
	public static GraphTree makeGraph(String root) {
		GraphTree rootNode = new GraphTree(root);
		rootNode.pathsMap = new HashMap<Integer, GraphTree>();
		rootNode.pathsMap.put(rootNode.getPath().hashCode(), rootNode);
		return rootNode;
	}

	public boolean getJavaScriptCode(Writer out, ParamsBean params, String curNode, Filter f) throws IOException {

		StringBuffer thisNode = new StringBuffer(1000);
		thisNode.append(curNode + " = gFld('" + name.replaceAll("'","&#x27;") + "', '");
		//thisNode.append(curNode + " = gFld('" + name.replaceAll("'","&#x27;") + "', 'index.jsp?id=" + getPath().hashCode());
		//thisNode.append("&" + params);
		thisNode.append(params.makeObjectUrl("index.jsp", getPath(), false));
		thisNode.append("');\n");

		StringBuffer childsarray = new StringBuffer(1000);
		childsarray.append(curNode +".addChildren([\n");
		int width = 0;
		boolean first = true;
		for(GraphTree o: childsMap.values()) {
			String child = curNode + "_" + width++;
			boolean noChild = o.getJavaScriptCode(out, params, child, f);
			if(! noChild) {
				if(! first)
					childsarray.append(", ");
				else {
					childsarray.append("    ");
				}
				childsarray.append(child);
				first = false;
			}
		}
		for(Map.Entry<String, GraphNode> e: graphsSet.entrySet()) {
			GraphNode currGraph = e.getValue();
			String path = getPath() + "/" + currGraph.getName();
			if(f == null || f.acceptGraph(currGraph, path)) {
				//Start to render the accepted graph
				if(! first)
					childsarray.append(",\n");
				first = false;

				/* replace \ with \\
				 * ex: C:\ become C:\\
				 * */   
				String leafName = e.getKey().replaceAll("\\\\","\\\\\\\\").replaceAll("'","&#x27;");
				childsarray.append("    [");
				childsarray.append("'" + leafName +"',");
				childsarray.append("'");
				childsarray.append(params.makeObjectUrl("index.jsp", currGraph, false));
				//childsarray.append("index.jsp?id=");
				//childsarray.append(currGraph.hashCode());
				//childsarray.append("&" + params);
				childsarray.append("']");
			}
		}
		if(! first)
			childsarray.append("\n");
		childsarray.append("]);\n");

		if(! first) {
			out.append(thisNode);
			out.append(childsarray);
		}
		return first;
	}

	public GraphTree getByPath(String path) {
		logger.trace("Looking for path " + path + " with id " + path.hashCode());
		return getById(path.hashCode());
	}

	public GraphTree getById(int id) {
		return pathsMap.get(id);
	}

	synchronized private void addChild(String childName) {
		if( ! childsMap.containsKey(childName)) {
			GraphTree newChild = new GraphTree(childName);
			childsMap.put(childName, newChild);
			newChild.parent = this;
			newChild.pathsMap = pathsMap;
			pathsMap.put(newChild.getPath().hashCode(), newChild);
		}
	}

	private void _addGraphByPath(LinkedList<String> path, GraphNode nodesGraph) {
		if(path.size() == 1) {
			graphsSet.put(path.getLast(), nodesGraph);
		}
		else {
			String pathElem = path.removeFirst();
			addChild(pathElem);
			getChildbyName(pathElem)._addGraphByPath(path, nodesGraph);
		}
	}

	public void addGraphByPath(LinkedList<String> path, GraphNode nodesGraph) {
		if(path.size() < 1) {
			logger.error("Path is empty : " + path + " for graph " + nodesGraph.getGraphTitle());
		}
		else
			_addGraphByPath(new LinkedList<String>(path), nodesGraph);
	}

	public Set<String> getChildsName() {
		Set<String> retValue = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		for(String childName: childsMap.keySet()) {
			retValue.add(childName);
		}
		return retValue;
	}

	public GraphTree getChildbyName(String name) {
		return (GraphTree) childsMap.get(name);
	}

	public String getName() {
		return name;
	}

	/**
	 * @return Returns the childsMap.
	 */
	public Map<String, GraphTree> getChildsMap() {
		return childsMap;
	}
	/**
	 * @return Returns the graphsSet.
	 */
	public Map<String, GraphNode>  getGraphsSet() {
		return graphsSet;
	}

	/**
	 * @param Filter, can be null
	 * @return
	 */
	public List<GraphNode> enumerateChildsGraph(Filter f) {
		List<GraphNode> retValue  = new ArrayList<GraphNode>();
		if(graphsSet != null) {
			if(f == null)
				retValue.addAll((Collection<GraphNode>) graphsSet.values());
			else {
				for(GraphNode g: graphsSet.values()) {
					String path = this.getPath() + "/" + g.getName();
					if(f.acceptGraph(g, path))
						retValue.add(g);
				}
			}
		}
		if(childsMap != null) {
			for(GraphTree child: childsMap.values()) {
				retValue.addAll(child.enumerateChildsGraph(f));
			}
		}	
		return retValue;
	}

	public String toString() {
		return name;
	}

	private StringBuffer _getPath() {
		StringBuffer retValue = null;
		if(parent == null)
			retValue = new StringBuffer();
		else
			retValue = parent._getPath();
		retValue.append("/");
		retValue.append(name);
		return retValue;
	}

	public String getPath() {
		return _getPath().toString();
	}
}
