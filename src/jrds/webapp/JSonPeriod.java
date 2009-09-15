package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.Period;

/**
 * Servlet implementation class JSonPeriod
 */
public class JSonPeriod extends JrdsServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ParamsBean params = getParamsBean(request);
		response.setContentType("application/json");
		ServletOutputStream out = response.getOutputStream();

		out.println("{");
		Period p = params.getPeriod();
		out.println("begin: \"" + params.getStringBegin() + ",\"");
		out.println("end: \"" + params.getStringEnd() + ",\"");
		int scale = p.getScale();
		if(scale != 0) {
			out.println("scale: \"" + scale + "\"");
		}
		out.println("}");
	}

}
