package jrds.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import jrds.HostsList;

public abstract class JSonData extends HttpServlet {
	static final private Logger logger = Logger.getLogger(JSonData.class);
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ParamsBean params = new ParamsBean(request);

		HostsList root = HostsList.getRootGroup();

		try {
			response.setContentType("application/json");
			ServletOutputStream out = response.getOutputStream();
			out.println("{ identifier: 'id'," +
					"  label: 'name'," +
					"  items: [ "
			);

			if (! generate(out, root, params)) {
				logger.warn("Invalid request received: " + request.getRequestURI() + "?" + request.getQueryString());
			}
			
			out.println("]}");
		} catch (Exception e) {
			logger.warn("Failed request: " + request.getRequestURI() + "?" + request.getQueryString() +": " + e);
		}
	}

	public abstract boolean generate(ServletOutputStream out, HostsList root, ParamsBean params) throws IOException;
	
	public String doNode(String name, int id, String type, List<String> childsref) {
		return doNode(name, Integer.toString(id), type, childsref, null);
	}
	public String doNode(String name, int id, String type, List<String> childsref, Map<String, String> attributes) {
		return doNode(name, Integer.toString(id), type, childsref, attributes);
	}
	public String doNode(String name, String id, String type, List<String> childsref) {
		return doNode(name, id, type, childsref, null);

	}
	public String doNode(String name, String id, String type, List<String> childsref, Map<String, String> attributes) {
		StringWriter buffer = new StringWriter();
		PrintWriter out = new PrintWriter(buffer );
		name = name.replace("'", " ").replace("\"", " ");
		out.print("{ name:'" + name + "', type:'" + type + "', id:'" + id + "'");
		if(attributes != null && attributes.size() > 0) {
			for(Map.Entry<String, String> e: attributes.entrySet()) {
				out.print(", " + e.getKey() + ":'" + e.getValue().replace("'", " ") +"'");
			}
		}
		if(childsref != null && childsref.size() >0 ) {
			out.println(", ");
			out.println("children:[");
			out.print("  ");
			for(String child: childsref) {
				out.print("{_reference:'" + child + "'},");
			}
			out.println();
			out.println("]");
		}
		out.println("},");

		return buffer.toString();
	}

}
