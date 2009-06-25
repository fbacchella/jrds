
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jrds.probe.HttpProbe;
import org.apache.log4j.Logger;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author bbonfils
 */
public class Main {

    static final private Logger logger = Logger.getLogger(HttpProbe.class);

    public static void main(String[] args) {
        Main main = new Main();
        main.Launcher(args);
    }

//    static final private Logger logger = Logger.getLogger(Main.class);
    public void Launcher(String[] args) {
        System.out.println("unzip webapp, please wait." + args.length);
        String jrdsWebAppString = getHomeDir() + "/.jrds/webapps";

        File jrdsWebApp = new File(jrdsWebAppString);

        if (jrdsWebApp.isDirectory()) {
            System.out.println("webapp is already there, starting jetty.");
        } else {
            System.out.println("Creating directory " + jrdsWebAppString);
            if (!jrdsWebApp.mkdirs()) {
                System.err.println("Error while creating directory " + jrdsWebAppString);
                System.exit(1);
            }
            try {
                File file = new File("build/jrds.war");
                unzip(file, jrdsWebAppString);
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }

        }

        /* Start jetty */
        try {
            Jetty.runJetty(jrdsWebAppString);
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }

    /* Thanks to Hudson source code */
    public static File whoAmI() throws IOException, URISyntaxException {
        URL classFile = Main.class.getClassLoader().getResource("Main.class");
        return new File(((JarURLConnection) classFile.openConnection()).getJarFile().getName());
    }

    public static void unzip(File file, String prefix) throws IOException {
        ZipFile zipFile;
        Enumeration entries;

        try {
            zipFile = new ZipFile(file);

            entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    System.err.println("Extracting directory: " + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    (new File(prefix + "/" + entry.getName())).mkdir();
                    continue;
                }

                System.err.println("Extracting file: " + entry.getName());
                copyInputStream(zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(prefix + "/" + entry.getName())));
            }

            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    private static String getHomeDir() {
        // check JNDI for the home directory first
        try {
            InitialContext iniCtxt = new InitialContext();
            Context env = (Context) iniCtxt.lookup("java:comp/env");
            String value = (String) env.lookup("JRDS_HOME");
            if (value != null && value.trim().length() > 0) {
                return value;
            }
            // look at one more place. See issue #1314
            value = (String) iniCtxt.lookup("JRDS_HOME");
            if (value != null && value.trim().length() > 0) {
                return value;
            }
        } catch (NamingException e) {
            // ignore
        }

        // finally check the system property
        String sysProp = System.getProperty("JRDS_HOME");
        if (sysProp != null) {
            return sysProp;
        }

        sysProp = System.getProperty("HOME");
        if (sysProp != null) {
            return sysProp;
        }

        // look at the env var next
        try {
            String env = System.getenv("JRDS_HOME");
            if (env != null) {
                return env;
            }
        } catch (Throwable _) {
            // when this code runs on JDK1.4, this method fails
        }

        return null;
    }
}