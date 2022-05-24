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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jrds.Util;

public class BootStrap {

    private static final String[] propertiesList = { "jetty.host", "jetty.port", "propertiesFile", "loglevel" };
    private static final String defaultCommand = "jetty";
    static final Map<String, CommandStarter> cmdClasses = new HashMap<>();

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        Path baseClassPath = getBaseClassPath();
        Properties configuration = new Properties();
        for (String prop: propertiesList) {
            String propVal = System.getProperty(prop);
            if (propVal != null) {
                configuration.put(prop, propVal);
            }
        }

        configuration.put("webRoot", baseClassPath.normalize().toString());

        Path libpath = baseClassPath.resolve(Paths.get("WEB-INF", "lib"));
        ClassLoader cl = makeClassLoader(libpath);

        String commandName = defaultCommand;
        if(args.length > 0) {
            commandName = args[0].trim().toLowerCase();
        }

        ServiceLoader<CommandStarter> serviceLoader = ServiceLoader.load(CommandStarter.class, cl);
        Iterator<CommandStarter> i = serviceLoader.iterator();
        while (i.hasNext()) {
            try {
                CommandStarter cmd = i.next();
                cmdClasses.put(cmd.getName(), cmd);
            } catch (Exception e) {
                System.out.format("Failed comamnd: %s%n", Util.resolveThrowableException(e));
            }
        }

        boolean help = false;
        if ("help".equals(commandName)) {
            help = true;
            if(args.length > 1)
                commandName = args[1].trim().toLowerCase();
            else
                doHelp();
        } else {
            if (args.length > 1)
                args = Arrays.copyOfRange(args, 1, args.length);
            else {
                args = new String[0];
            }
        }

        CommandStarter command = cmdClasses.get(commandName);
        if (command == null) {
            System.out.format("No command given, available commands: %s%n", String.join(", ", cmdClasses.keySet()));
            System.exit(1);
        }
        // Release unused commands
        cmdClasses.clear();
        if (help) {
            command.help();
            System.exit(0);
        }
        command.configure(configuration);
        command.start(args);
    }

    private static Path getBaseClassPath() throws IOException, URISyntaxException {
        String path = "/" + BootStrap.class.getName().replace(".", "/") + ".class";
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

    private static ClassLoader makeClassLoader(Path baseClassPath) {
        List<URL> classPath = new ArrayList<>();

        Predicate<Path> filter = p -> Files.isRegularFile(p) && p.toString().endsWith(".jar");

        Consumer<Path> enumerateDirectory = p -> {
            try {
                Files.list(p)
                     .filter(filter)
                     .map(Path::normalize)
                     .map(Path::toUri)
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

        if (Files.isDirectory(baseClassPath)) {
            enumerateDirectory.accept(baseClassPath);
        }

        String libspath = System.getProperty("libspath", "");

        Arrays.stream(libspath.split(String.valueOf(File.pathSeparatorChar)))
              .map(Paths::get)
              .forEach(p -> {
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
        return URLClassLoader.newInstance(classPath.toArray(new URL[0]));
    }

    private static void doHelp() {
        System.out.println("Lists of available command:");
        for (String commandName: cmdClasses.keySet()) {
            System.out.println("    " + commandName);
        }
        System.out.println("");
        System.out.println("Lists of configuration properties:");
        for (String propName: propertiesList) {
            System.out.println("    " + propName);
        }
        System.out.println(String.format("A class path can be auto build with the system property libspath that contains  list of directory or jar, separated by a %s", File.pathSeparatorChar));
        System.exit(0);
    }

}
