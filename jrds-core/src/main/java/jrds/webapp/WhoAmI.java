package jrds.webapp;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/whami")
public class WhoAmI extends JrdsServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType("text/plain");
        resp.addHeader("Cache-Control", "no-cache");

        Principal p = req.getUserPrincipal();
        if(p == null) {
            out.println("Anonymous user");
        } else {
            out.println("Principal: " + p.getName() + " (" + p.getClass().getCanonicalName() + ")");
        }

        if(req.isUserInRole(getPropertiesManager().adminrole)) {
            out.println("is admin with role " + getPropertiesManager().adminrole);
        }
        out.print("also member of");
        for(String role: getPropertiesManager().defaultRoles) {
            if(req.isUserInRole(role)) {
                out.print(" " + role);
            }
        }
        out.println();
        HttpSession s = req.getSession();
        if(s != null) {
            for(String attr: Collections.list(s.getAttributeNames())) {
                out.println(attr + ": " + s.getAttribute(attr));
            }
        }
    }

}
