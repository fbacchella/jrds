package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class JSonQueryParams
 */
public class JSonQueryParams extends JrdsServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ParamsBean params = getParamsBean(request);
		response.setContentType("application/json");
		ServletOutputStream out = response.getOutputStream();

		out.println("{");
		doVariable(out, "id", request.getParameter("id"));
		doVariable(out, "gid", request.getParameter("gid"));
		doVariable(out, "sort", request.getParameter("sort"));
		doVariable(out, "host", request.getParameter("host"));
		doVariable(out, "filter", request.getParameter("filter"));
		doVariable(out, "begin", params.getStringBegin());
		doVariable(out, "end", params.getStringEnd());
		doVariable(out, "min", params.getMinStr());
		doVariable(out, "max", params.getMaxStr());
		int scale = params.getScale();
		if(scale > 0) {
			doVariable(out, "autoperiod", "" + scale);
		}
		else {
			doVariable(out, "autoperiod", "0");

		}
		out.println("}");
	}

	private void doVariable(ServletOutputStream out, String key, String value) throws IOException {
		if(value != null && ! "".equals(value)) {
			value = value.replace("'", " ").replace("\"", " ");
			out.println(key + ": '" + value + "',");
		}
	}

}
