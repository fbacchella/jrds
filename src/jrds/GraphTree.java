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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jrds.webapp.PeriodBean;

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

	private GraphTree parent;
	private GraphTree root;
	private Map<String, GraphTree> pathsMap;
	//The node's name
	private String name;
	//The childs
	private Map<String, GraphTree> childsMap;
	//The graphs in this node
	private Map<String, RdsGraph> graphsSet;

	/**
	 *  Private constructor, no one can generate an graph on the fly
	 *  
	 */
	 private GraphTree(String name) {
		graphsSet = new TreeMap<String, RdsGraph>(String.CASE_INSENSITIVE_ORDER);
		childsMap = new TreeMap<String, GraphTree>(GraphTree.getComparator());
		this.name = name;
	}
	 
	 /**
	  * The only way to get a new graph
	 * @param root the graph's root name
	 * @return
	 */
	 public static GraphTree makeGraph(String root) {
		 GraphTree rootNode = new GraphTree(root);
		 rootNode.pathsMap = new HashMap<String, GraphTree>();
		 rootNode.pathsMap.put(rootNode.getPath(), rootNode);
		 return rootNode;
	 }
	 
	public void getJavaScriptCode(Writer out, String queryString, String curNode) throws IOException {
		StringBuffer childsarray = new StringBuffer(1000);
		childsarray.append(curNode + " = gFld('" + name + "', 'graphlist" + getPath());
		if(queryString != null &&  ! "".equals(queryString))
			childsarray.append("?" + queryString);
		childsarray.append("');\n");
		childsarray.append(curNode +".addChildren([\n");
		int width = 0;
		boolean first = true;
		//for(Iterator i = childsMap.values().iterator(); i.hasNext(); width++) {
		for(GraphTree o: childsMap.values()) {
			if(! first)
				childsarray.append(", ");
			else {
				childsarray.append("    ");
				first = false;
			}
			String child = curNode + "_" + width++;
			//GraphTree o = (GraphTree) i.next();
			o.getJavaScriptCode(out, queryString, child);
			childsarray.append(child);
		}
		for(Iterator i = graphsSet.entrySet().iterator(); i.hasNext(); ) {
			if(! first)
				childsarray.append(",\n");
			else
				first = false;
			Map.Entry e = (Map.Entry) i.next();
			RdsGraph currGraph = (RdsGraph) e.getValue();
			/* replace \ with \\
			 * ex: C:\ become C:\\
			 * */   
			String leafName = ((String) e.getKey()).replaceAll("\\\\","\\\\\\\\");
			childsarray.append("    [");
			childsarray.append("'" + leafName +"',");
			childsarray.append("'");
			childsarray.append("index.jsp?id=");
			childsarray.append(currGraph.hashCode());
			if(queryString != null &&  ! "".equals(queryString)) {
				childsarray.append("&");
				childsarray.append(queryString);
			}
			childsarray.append("']");
		}
		if(! first)
			childsarray.append("\n");
		childsarray.append("]);\n");
		if(childsarray.length() > 0) {
			out.write(childsarray.toString());
		}
	}

	public GraphTree getByPath(String path) {
		return (GraphTree) pathsMap.get(path);
	}
	
	synchronized private void addChild(String childName) {
		if( ! childsMap.containsKey(childName)) {
			GraphTree newChild = new GraphTree(childName);
			newChild.root = root;
			childsMap.put(childName, newChild);
			newChild.setParent(this);
			newChild.pathsMap = pathsMap;
			pathsMap.put(newChild.getPath(), newChild);
		}
	}
	
	private void _addGraphByPath(LinkedList<String> path, RdsGraph nodesGraph) {
		if(path.size() == 1) {
			graphsSet.put(path.getLast(), nodesGraph);
		}
		else {
			String pathElem = (String)path.removeFirst();
			addChild(pathElem);
			getChildbyName(pathElem)._addGraphByPath(path, nodesGraph);
		}
	}
	
	public void addGraphByPath(LinkedList<String> path, RdsGraph nodesGraph) {
		if(path.size() < 1) {
			logger.error("Path is empty : " + path + " for graph " + nodesGraph.getGraphTitle());
		}
		else
			_addGraphByPath((LinkedList<String>) path.clone(), nodesGraph);
	}
	
	public Set getChildsName() {
		Set<String> retValue = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		for(Iterator i = childsMap.keySet().iterator() ; i.hasNext() ;) {
			String childName = (String) i.next();
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
	 * @return Returns the parent.
	 */
	public GraphTree getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(GraphTree parent) {
		this.parent = parent;
	}
	/**
	 * @return Returns the childsMap.
	 */
	public Map getChildsMap() {
		return childsMap;
	}
	/**
	 * @return Returns the graphsSet.
	 */
	public Map getGraphsSet() {
		return graphsSet;
	}
	
	public List<GraphTree> enumerateChildsGraph() {
		List<GraphTree> retValue  = new ArrayList<GraphTree>();
		if(graphsSet != null)
			retValue.addAll((Collection<? extends GraphTree>) graphsSet.values());
		if(childsMap != null) {
			for(GraphTree child: childsMap.values()) {
				retValue.addAll(child.enumerateChildsGraph());
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
	
	static Comparator getComparator() {
		return new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return String.CASE_INSENSITIVE_ORDER.compare(arg0.toString(), arg1.toString());
			}
		};
	}

}
