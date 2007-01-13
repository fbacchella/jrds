package jrds.webapp;

import javax.servlet.http.HttpServletRequest;

import jrds.HostsList;
import jrds.RdsGraph;

public class GraphBean {
	RdsGraph graph = null;
	ParamsBean params;
	String root = "";
	
	
	public GraphBean(){

	}

	public GraphBean(HttpServletRequest req) {
		root = req.getContextPath();
		params = new ParamsBean(req);
	}

	public GraphBean(HttpServletRequest req, ParamsBean cgiParams) {
		config(req, cgiParams);
	}

	public void config(HttpServletRequest req, ParamsBean cgiParams) {
		root = req.getContextPath();
		params = cgiParams;
	}

	public void setGraph(int id) {
		graph = HostsList.getRootGroup().getGraphById(id);
	}
	
	public void setGraph(RdsGraph graph) {
		this.graph = graph;
	}
	
	public int getGraph() {
		int retValue = 0;
		if(graph != null)
			retValue = graph.hashCode();
		return retValue;
	}
	
	public int getHeight() {
		return graph.getRealHeight();
	}
	
	public int getWidht() {
		return graph.getRealWidth();
	}
	
	public String getImgUrl() {
		//We build the Url
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append(root);
		if(! "".equals(root)) {
			urlBuffer.append("/");
		}
		urlBuffer.append("graph?id=" + graph.hashCode());
		urlBuffer.append("&" + params.getPeriodUrl());
		return urlBuffer.toString();
	}
	
	public String getImgElement() {
		StringBuffer imgElement = new StringBuffer();
		//HostsList.getRootGroup().getRenderer().render(graph, period.getBegin(), period.getEnd());

		imgElement.append("<img class='graph' ");
		imgElement.append("id='" + graph.hashCode() + "'" );
		imgElement.append("src='" + getImgUrl() + "' ");

		//A few more attributes
		imgElement.append("name='" + graph.getQualifieName() + "' ");
		imgElement.append("alt='" + graph.getQualifieName() + "' ");
//		int rHeight = graph.getRealHeight();
//		if(rHeight > 0)
//			imgElement.append("height='" + Integer.toString(rHeight) + "' ");
//		int rWidth = graph.getRealWidth();
//		if(rWidth > 0)
//			imgElement.append("width='" + Integer.toString(rWidth)+ "' ");

		imgElement.append(" />");

		return imgElement.toString();
	}
	
}
