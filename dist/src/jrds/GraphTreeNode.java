/*
 * Created on 9 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class GraphTreeNode {
	static final int LEAF_GRAPHTITLE = 1;
	static final int LEAF_HOSTNAME = 2;
	static final private Logger logger = JrdsLogger.getLogger(GraphTreeNode.class);

	private GraphTreeNode parent;
	private Map childsMap;
	private String name;
	private Map graphsSet;

	/**
	 * 
	 */
	public GraphTreeNode(String name) {
		graphsSet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		childsMap = new TreeMap(GraphTreeNode.getComparator());
		this.name = name;
	}
	
	public GraphTreeNode(String name, RdsGraph nodesGraph) {
		graphsSet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		childsMap = new TreeMap(GraphTreeNode.getComparator());
		this.name = name;
		graphsSet.put(name, nodesGraph);
	}
	
	public String getHtmlCode(Calendar begin, Calendar end) {
		String retValue = "";
		retValue += "<li>"  + name + "\n";
		retValue += "<ul>\n";
		for(Iterator i = childsMap.values().iterator(); i.hasNext(); ) {
			Object o = i.next();
			retValue += ((GraphTreeNode) o).getHtmlCode(begin, end);
		}
		retValue += "</ul>\n";

		return retValue;
	}

	private StringBuffer _getJavaScriptCode(String argsBeginEnd) {


		StringBuffer retValue = new StringBuffer(1000);
	
		retValue.append("[ '"  + name + "', 'graphlist" + getPath() + "?" + argsBeginEnd + "', '_blank', \n");
	
		for(Iterator i = childsMap.values().iterator(); i.hasNext(); ) {
			GraphTreeNode o = (GraphTreeNode) i.next();
			retValue.append(o._getJavaScriptCode(argsBeginEnd));
		}
		for(Iterator i = graphsSet.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry) i.next();
			RdsGraph currGraph = (RdsGraph) e.getValue();
			String leafName = (String) e.getKey();
			
			StringBuffer imgUrl = new StringBuffer(50);
			imgUrl.append("simplegraph.html?id=");
			imgUrl.append(currGraph.hashCode());
			imgUrl.append("&");
			imgUrl.append(argsBeginEnd);
			
			retValue.append("[ '" + leafName + "', ");
			retValue.append("'" + imgUrl +"', ");
			retValue.append("'graph1' ], \n");
		}
		retValue.append(" ],\n");

		return retValue;
	}
	
	public String getJavaScriptCode(Calendar begin, Calendar end, int leafTitle) {
		String argsBeginEnd = "begin=" + begin.getTimeInMillis() + "&end=" + end.getTimeInMillis();
		return _getJavaScriptCode(argsBeginEnd).toString();
	}

	public String getJavaScriptCode(Date begin, Date end, int leafTitle) {
		String argsBeginEnd = "begin=" + begin.getTime() + "&end=" + end.getTime();
		return _getJavaScriptCode(argsBeginEnd).toString();
	}
	
	public GraphTreeNode getByPath(List path) {
		GraphTreeNode retValue = null;
		if(path.size() == 1) {
				retValue = this;
		}
		else {
			GraphTreeNode nextNode = null;
			path.remove(0);
			String nextName = (String)path.get(0);
			for(Iterator i = childsMap.values().iterator(); i.hasNext(); ) {
				GraphTreeNode tryNode = (GraphTreeNode) i.next();
				if(tryNode.getName().equals(nextName)) {
					nextNode = tryNode;
					break;
				}
			}
			if(nextNode != null)
				retValue = nextNode.getByPath(path);
		}
		return retValue;
	}
	
	public Iterator valuesIterator()
	{
		return childsMap.values().iterator();
	}

	public Iterator keysIterator()
	{
		return childsMap.keySet().iterator();
	}

	synchronized public void addChild(GraphTreeNode newChild) {
		String childName = newChild.getName();
		if( ! childsMap.containsKey(childName)) {
			childsMap.put(childName, newChild);
		}
		else {
			GraphTreeNode childNode = (GraphTreeNode) childsMap.get(childName);
			childNode.graphsSet.putAll(newChild.graphsSet);
			childNode.childsMap.putAll(newChild.childsMap);
		}
	}
	
	synchronized public void addChild(String childName) {
		if( ! childsMap.containsKey(childName)) {
			GraphTreeNode newChild = new GraphTreeNode(childName);
			childsMap.put(childName, newChild);
			newChild.setParent(this);
		}
	}
	
	private void addGraphByPath2(LinkedList path, RdsGraph nodesGraph) {
		if(path.size() == 1)
			graphsSet.put(path.getLast(), nodesGraph);
		else {
			String pathElem = (String)path.removeFirst();
			addChild(pathElem);
			getChildbyName(pathElem).addGraphByPath2(path, nodesGraph);
		}
	}
	
	public void addGraphByPath(LinkedList path, RdsGraph nodesGraph) {
		if(path.size() < 1) {
			logger.error("Path is empty : " + path + " for graph " + nodesGraph.getGraphTitle());
		}
		else
			addGraphByPath2((LinkedList) path.clone(), nodesGraph);
	}
	
	public Set getChildsName() {
		Set retValue = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for(Iterator i = childsMap.keySet().iterator() ; i.hasNext() ;) {
			String childName = (String) i.next();
			retValue.add(childName);
		}
		return retValue;
	}
	
	public GraphTreeNode getChildbyName(String name) {
		return (GraphTreeNode) childsMap.get(name);
	}
	
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the parent.
	 */
	public GraphTreeNode getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(GraphTreeNode parent) {
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
	
	public List enumerateChildsGraph() {
		List retValue  = new ArrayList();
		if(graphsSet != null)
			retValue.addAll(graphsSet.values());
		if(childsMap != null) {
			for(Iterator i = childsMap.values().iterator() ; i.hasNext() ;) {
				GraphTreeNode child = (GraphTreeNode) i.next();
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
