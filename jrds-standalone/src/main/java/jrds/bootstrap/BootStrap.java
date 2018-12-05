package jrds.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BootStrap {

    static final private String[] propertiesList = { "jetty.host", "jetty.port", "propertiesFile", "loglevel" };
    static final private String defaultCommand = "jetty";
    static final Map<String, String> cmdClasses = new HashMap<String, String>();

    static {
        cmdClasses.put("jetty", "jrds.standalone.Jetty");
        cmdClasses.put("jmxserver", "jrds.standalone.JMX");
        cmdClasses.put("wikidoc", "jrds.standalone.EnumerateWikiProbes");
        cmdClasses.put("checkjar", "jrds.standalone.CheckJar");
        cmdClasses.put("collect", "jrds.standalone.Collector");
        cmdClasses.put("dosnmpprobe", "jrds.standalone.DoSnmpProbe");
        cmdClasses.put("dump", "jrds.standalone.Dump");
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {

        Path baseClassPath = getBaseClassPath();
        Properties configuration = new Properties();
        for(String prop: propertiesList) {
            String propVal = System.getProperty(prop);
            if(propVal != null)
                configuration.put(prop, propVal);
        }

        configuration.put("webRoot", baseClassPath.normalize().toString());

        ClassLoader cl = makeClassLoader(baseClassPath.resolve(Paths.get("WEB-INF", "lib")));

        String commandName = defaultCommand;
        if(args.length > 0) {
            commandName = args[0].trim().toLowerCase();
        }

        boolean help = false;
        if("help".equals(commandName)) {
            help = true;
            if(args.length > 1)
                commandName = args[1].trim().toLowerCase();
            else
                doHelp();
        } else {
            if(args.length > 1)
                args = Arrays.copyOfRange(args, 1, args.length);
            else {
                args = new String[0];
            }
        }

        CommandStarter command = findCmdClass(commandName, cl);
        if(command == null) {
            System.exit(1);
        }
        if(help) {
            command.help();
            System.exit(0);
        }
        command.configure(configuration);
        command.start(args);
    }

    static private String ressourcePath(Object o) {
        if(o instanceof Class<?>) {
            Class<?> c = (Class<?>) o;
            return "/".concat(c.getName().replace(".", "/").concat(".class"));
        } else if(o instanceof String) {
            return (String) o;
        } else {
            return "";
        }
    }

    static private Path getBaseClassPath() throws IOException, URISyntaxException {
        String path = ressourcePath(BootStrap.class);
        URL me = BootStrap.class.getResource(path);
        String protocol = me.getProtocol();
        URL rootUrl;
        if("jar".equals(protocol)) {
            JarURLConnection cnx = (JarURLConnection) me.openConnection();
            rootUrl = cnx.getJarFileURL();
        } else if("file".equals(protocol)) {
            rootUrl = me;
        } else {
            throw new URISyntaxException(me.toString(), "Unhandled protocol");
        }

        return Paths.get(rootUrl.toURI()).getParent();
    }

    static private ClassLoader makeClassLoader(Path baseClassPath) throws IOException {
        List<URL> classPath = new ArrayList<URL>();

        Predicate<Path> filter = p -> {return Files.isRegularFile(p) && p.toString().endsWith(".jar");};

        Consumer<Path> enumerateDirectory = p -> {
            try {
                Files.list(p)
                .filter(filter)
                .map( i -> i.normalize())
                .map( i-> i.toUri())
                .map(i -> {
                    try {
                        return i.toURL();
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .forEach(classPath::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        enumerateDirectory.accept(baseClassPath);

        String libspath = System.getProperty("libspath", "");

        Arrays.stream(libspath.split(String.valueOf(File.pathSeparatorChar)))
        .map(Paths::get)
        .forEach( p -> {
            if (Files.isDirectory (p)) {
                enumerateDirectory.accept(p);
            } else if (filter.test(p)){
                try {
                    classPath.add(p.normalize().toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }

            }
        });
        return URLClassLoader.newInstance(classPath.toArray(new URL[classPath.size()]));
    }

    static private void doHelp() {
        System.out.println("Lists of available command:");
        for(String commandName: cmdClasses.keySet()) {
            System.out.println("    " + commandName);
        }
        System.out.println("");
        System.out.println("Lists of configuration properties:");
        for(String propName: propertiesList) {
            System.out.println("    " + propName);
        }
        System.out.println(String.format("A class path can be auto build with the system property libspath that contains  list of directory or jar, separated by a %s", File.pathSeparatorChar));
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    static private CommandStarter findCmdClass(String command, ClassLoader cl) {
        String cmdClass = cmdClasses.get(command);
        if(cmdClass == null) {
            System.out.println("Unknown command: " + command);
            return null;
        }
        Class<CommandStarter> cmdStarter;
        try {
            cmdStarter = (Class<CommandStarter>) cl.loadClass(cmdClass);
        } catch (ClassNotFoundException e) {
            System.err.println("JRDS installation not found");
            return null;
        }
        try {
            return cmdStarter.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
