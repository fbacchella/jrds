import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jrds.Tools;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class UriParse {
    static final private Logger logger = Logger.getLogger(UriParse.class);
    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
    }

    @SuppressWarnings("unused")
    @Test
    public void uritest() throws URISyntaxException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        logger.trace("start");
        Class<?> c = "".getClass();
        String retValue="Not found";
        String cName = c.getCanonicalName();
        logger.trace(logger.getClass().getPackage().getName());
        logger.trace(c.getResource(logger.getClass().getPackage().getName()));
        int lastDot = cName.lastIndexOf('.');
        if(lastDot > 1) {
            String scn = cName.substring(lastDot + 1);
            URL jarUrl = c.getResource(scn + ".class");
            URI uri = jarUrl.toURI();
            if(jarUrl != null)
                retValue = jarUrl.getPath();
            else
                retValue = scn + " not found";
            Class<?> Uriclass = uri.getClass();
            for(Method m: Uriclass.getDeclaredMethods()) {
                if(m.getName().startsWith("get")) {
                    Object getted = m.invoke(uri);
                    logger.trace(m + " " + getted);
                }
            }

        }
    }
}
