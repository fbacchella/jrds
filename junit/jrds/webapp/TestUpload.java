package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import jrds.Tools;
import jrds.factories.xml.EntityResolver;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpTester.Request;
import org.eclipse.jetty.http.HttpTester.Response;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestUpload {
    static final private Logger logger = Logger.getLogger(TestStats.class);

    org.eclipse.jetty.servlet.ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, Upload.class.getName(), EntityResolver.class.getName());
    }

    @Before
    public void prepareServlet() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), Upload.class, "/upload");
        tester.start();
    }

    @Test
    public void testXXE1() throws Exception {
        InputStream is = Tools.class.getResourceAsStream("/ressources/xxetesting1.mime");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        logger.debug("Sending " + baos.toString("UTF-8"));

        Response response = ToolsWebApp.doRequestPost(tester, "http://tester/upload", new ToolsWebApp.MakePostContent() {
            @Override
            void fillRequest(Request r) {
                r.setHeader("Content-Type", "multipart/form-data; boundary=----------------------------84223b7e8d58");
                try {
                    r.setContent(baos.toString("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 200);
        Assert.assertEquals(200, response.getStatus());
        String content =  response.getContent();
        logger.debug("got " + content);
        JSONObject jobject = new JSONObject("{ \"vector\": " + content.substring(content.indexOf('['), content.lastIndexOf(']') + 1) + "}");
        Assert.assertEquals(1, jobject.getJSONArray("vector").length());
        // If parsing is secure, it doesn't fail on a non existent file
        Assert.assertEquals(Boolean.TRUE, jobject.getJSONArray("vector").getJSONObject(0).get("parsed"));
    }

    @Test
    public void testXXE2() throws Exception {
        InputStream is = Tools.class.getResourceAsStream("/ressources/xxetesting2.mime");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        logger.debug("Sending " + baos.toString("UTF-8"));

        Response response = ToolsWebApp.doRequestPost(tester, "http://tester/upload", new ToolsWebApp.MakePostContent() {
            @Override
            void fillRequest(Request r) {
                r.setHeader("Content-Type", "multipart/form-data; boundary=----------------------------84223b7e8d58");
                try {
                    r.setContent(baos.toString("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 200);
        Assert.assertEquals(200, response.getStatus());
        String content =  response.getContent();
        logger.debug("got " + content);
        JSONObject jobject = new JSONObject("{ \"vector\": " + content.substring(content.indexOf('['), content.lastIndexOf(']') + 1) + "}");
        Assert.assertEquals(1, jobject.getJSONArray("vector").length());
        // If parsing is secure, it doesn't fail on a non existent file
        logger.debug(jobject.getJSONArray("vector").getJSONObject(0));
        Assert.assertEquals(Boolean.TRUE, jobject.getJSONArray("vector").getJSONObject(0).get("parsed"));
    }

    @Test
    public void testGood() throws Exception {
        InputStream is = Tools.class.getResourceAsStream("/ressources/goodupload.mime");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        logger.debug("Sending " + baos.toString("UTF-8"));

        Response response = ToolsWebApp.doRequestPost(tester, "http://tester/upload", new ToolsWebApp.MakePostContent() {
            @Override
            void fillRequest(Request r) {
                r.setHeader("Content-Type", "multipart/form-data; boundary=----------------------------84223b7e8d58");
                try {
                    r.setContent(baos.toString("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 200);
        String content =  response.getContent();
        JSONObject jobject = new JSONObject("{ \"vector\": " + content.substring(content.indexOf('['), content.lastIndexOf(']') + 1) + "}");
        Assert.assertEquals(1, jobject.getJSONArray("vector").length());
        Assert.assertEquals(Boolean.TRUE, jobject.getJSONArray("vector").getJSONObject(0).get("parsed"));
    }

}
