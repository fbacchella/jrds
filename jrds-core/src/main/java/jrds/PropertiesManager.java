package jrds;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.configuration.LogConfigurator;
import jrds.configuration.ModuleConfigurator;
import jrds.starter.Timer;
import jrds.store.RrdDbStoreFactory;
import jrds.store.StoreFactory;
import jrds.webapp.ACL;
import jrds.webapp.RolesACL;

/**
 * An less ugly class supposed to manage properties should be reworked
 * 
 * @author Fabrice Bacchella
 */
public class PropertiesManager extends Properties {
    private final Logger logger = LoggerFactory.getLogger(PropertiesManager.class);

    public static final class TimerInfo {
        public int step;
        public int timeout;
        public int numCollectors;
        public int slowCollectTime;
    }

    private final FileFilter filter = new FileFilter() {
        public boolean accept(File file) {
            return (!file.isHidden()) && (file.isFile() && file.getName().endsWith(".jar"));
        }
    };

    public PropertiesManager() {
    }

    public PropertiesManager(File propFile) {
        join(propFile);
        update();
    }

    private int parseInteger(String s) {
        Integer integer;
        if (s != null) {
            if (s.startsWith("#")) {
                integer = Integer.valueOf(s.substring(1), 16);
            } else if (s.startsWith("0x")) {
                integer = Integer.valueOf(s.substring(2), 16);
            } else if (s.startsWith("0") && s.length() > 1) {
                integer = Integer.valueOf(s.substring(1), 8);
            } else {
                integer = Integer.valueOf(s);
            }
            return integer.intValue();
        }
        throw new NumberFormatException("Parsing null string");
    }

    public boolean parseBoolean(String s) {
        s = s.toLowerCase().trim();
        boolean retValue = false;
        if("1".equals(s))
            retValue = true;
        else if("yes".equals(s))
            retValue = true;
        else if("y".equals(s))
            retValue = true;
        else if("true".equals(s))
            retValue = true;
        else if("enable".equals(s))
            retValue = true;
        else if("on".equals(s))
            retValue = true;

        return retValue;
    }

    public Map<String, String> subKey(String prefix) {
        Pattern regex = Pattern.compile("^" + prefix + "\\.");
        Map<String, String> props = new HashMap<String, String>();

        for(Map.Entry<Object, Object> e: entrySet()) {
            String key = (String) e.getKey();
            Matcher m = regex.matcher(key);
            if(m.find()) {
                String value = (String) e.getValue();
                props.put(m.replaceFirst(""), value);
            }
        }
        return props;
    }

    public void join(URL url) {
        try (InputStream inputstream = url.openStream()){
            load(inputstream);
            inputstream.close();
        } catch (IOException ex) {
            logger.warn("Invalid URL: " + ex.getLocalizedMessage());
        }
    }

    public void join(Properties moreProperties) {
        putAll(moreProperties);
    }

    public void join(File propFile) {
        logger.debug("Using propertie file " + propFile.getAbsolutePath());
        try (InputStream inputstream = new FileInputStream(propFile)) {
            load(inputstream);
        } catch (IOException ex) {
            logger.warn("Invalid properties file " + propFile.getAbsolutePath() + ": " + ex.getLocalizedMessage());
        }
    }

    public void join(InputStream propStream) {
        try {
            load(propStream);
        } catch (IOException ex) {
            logger.warn("Invalid properties stream " + propStream + ": " + ex);
        }
    }

    private ClassLoader doClassLoader(String extendedclasspath) {
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return (!file.isHidden()) && file.isFile() && file.getName().endsWith(".jar");
            }
        };

        Collection<URI> urls = new ArrayList<URI>();

        if(extendedclasspath != null && !"".equals(extendedclasspath)) {
            for(String pathElement: extendedclasspath.split(";")) {
                logger.debug("Setting class directories to: " + pathElement);

                File path = new File(pathElement);

                if(path.isDirectory()) {
                    for(File f: path.listFiles(filter)) {
                        urls.add(f.toURI());
                    }
                } else if(filter.accept(path)) {
                    urls.add(path.toURI());
                }
            }
        }

        for(URI u: libspath) {
            urls.add(u);
        }

        URL[] arrayUrl = new URL[urls.size()];
        int i = 0;
        for(URI u: urls) {
            try {
                arrayUrl[i++] = u.toURL();
            } catch (MalformedURLException e) {
                logger.error("Invalid URL in libs path: " + u);
            }
        }
        if(logger.isDebugEnabled())
            logger.debug("Internal class loader will look in:" + urls);
        return new URLClassLoader(arrayUrl, getClass().getClassLoader()) {
            @Override
            public String toString() {
                return "JRDS' class loader";
            }
        };
    }

    private File prepareDir(File dir, boolean autocreate, boolean readOnly) throws IOException {
        if(dir == null) {
            throw new IOException("path not defined");
        }
        if(!dir.exists()) {
            if(!autocreate) {
                throw new IOException(dir + " doesn't exist");
            }
            if(autocreate && !dir.mkdirs()) {
                throw new IOException(dir + " doesn't exist and can't be created");
            }
        } else if(!dir.isDirectory()) {
            throw new IOException(dir + " exists but is not a directory");
        } else if(!dir.canWrite() && !readOnly) {
            throw new IOException(dir + " exists can't be written");
        }
        return dir;
    }

    private File prepareDir(String path, boolean autocreate, boolean readOnly) throws IOException {
        if(path == null || path.isEmpty()) {
            throw new IOException("path not defined");
        }
        File dir = new File(path);
        return prepareDir(dir, autocreate, readOnly);
    }

    public void configureStores() {
        String defaultStorename = getProperty("defaultstore", StoreFactory.DEFAULTNAME);
        Map<String, Properties> storesConfig = new HashMap<String, Properties>(1);
        storesConfig.put(defaultStorename, new Properties());

        Properties defaultStoreProps = storesConfig.get(defaultStorename);
        // Put old values in the default factory properties
        for(String oldProps: new String[] { "rrdbackend", "dbPoolSize", "usepool" }) {
            if(getProperty(oldProps) != null)
                defaultStoreProps.put(oldProps, getProperty(oldProps));
        }

        // Simple case, just the store factory
        if(getProperty("storefactory") != null) {
            String defaultstorefactoryclassname = getProperty("storefactory");
            defaultStoreProps.put("factory", defaultstorefactoryclassname);
        }

        String propertiesListStores = getProperty("stores", "");
        if(!propertiesListStores.trim().isEmpty()) {
            for(String storeName: propertiesListStores.split(",")) {
                storeName = storeName.trim();
                Map<String, String> storeInfo = subKey("store." + storeName);
                Properties props = new Properties();
                props.putAll(storeInfo);
                storesConfig.put(storeName, props);
            }
        }

        // Ensure that the default store was not forgotten
        if(defaultStoreProps.get("factory") == null) {
            defaultStoreProps.put("factory", RrdDbStoreFactory.class.getName());
        }

        logger.trace("Stores configuration: {}", storesConfig);

        // Ok, now configure and store the factories
        for(Map.Entry<String, Properties> e: storesConfig.entrySet()) {
            String storeName = e.getKey();
            Properties storesInfo = e.getValue();
            try {
                String storefactoryclassname = storesInfo.getProperty("factory");
                if(storefactoryclassname != null && !storefactoryclassname.isEmpty()) {
                    StoreFactory sf = (StoreFactory) extensionClassLoader.loadClass(storefactoryclassname).getConstructor().newInstance();
                    sf.configureStore(this, storesInfo);
                    sf.start();
                    stores.put(storeName, sf);
                } else {
                    logger.error("store factory {} invalid, no factory given", storeName);
                }
            } catch (Exception e1) {
                jrds.Util.log(getClass().getCanonicalName(), logger, Level.ERROR, e1, "store factory %s failed to configure: %s", storeName, e1);
            }
        }
        logger.debug("Stores configured: {}", stores);
        logger.debug("default store: {}", defaultStorename);

        defaultStore = stores.remove(defaultStorename);

    }

    @SuppressWarnings("unchecked")
    public void importSystemProps() {
        String localPropFile = System.getProperty("jrds.propertiesFile");
        if(localPropFile != null)
            join(new File(localPropFile));

        Pattern jrdsPropPattern = Pattern.compile("jrds\\.(.+)");
        Properties p = System.getProperties();
        for(String name: Collections.list((Enumeration<String>) p.propertyNames())) {
            Matcher m = jrdsPropPattern.matcher(name);
            if(m.matches()) {
                String prop = System.getProperty(name);
                if(prop != null)
                    setProperty(m.group(1), prop);
            }
        }
    }

    public void update() {

        Locale.setDefault(new Locale("POSIX"));

        // **********************
        // The class loader configuration

        // First resolve libspath, doClassLoader needs it
        try {
            Enumeration<URL> descurl = getClass().getClassLoader().getResources("desc");
            while (descurl.hasMoreElements()) {
                libspath.add(descurl.nextElement().toURI());
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI syntax exception", e);
        } catch (IOException e) {
            throw new RuntimeException("Can't locate embedded desc", e);
        }

        String libspathString = getProperty("libspath", "");
        if(!"".equals(libspathString.trim())) {
            for(String libName: libspathString.split(";")) {
                File lib = new File(libName.trim());

                boolean noJarDir = true;
                if(lib.isDirectory()) {
                    File[] foundFiles = lib.listFiles(filter);
                    if(foundFiles == null) {
                        logger.error("Failed to search in " + lib);
                        continue;
                    }
                    for(File f: foundFiles) {
                        libspath.add(f.toURI());
                        noJarDir = false;
                    }
                }

                // If a jar was found previously, it's not a source directory,
                // don't add it, only the jar in it are interesting
                if(lib.isFile() || (lib.isDirectory() && noJarDir))
                    libspath.add(lib.toURI());
            }
        }

        // The class loader is configured early, it can be used by log4j
        extensionClassLoader = doClassLoader(getProperty("classpath", ""));


        // **********************
        // The log configuration

        boolean nologging = parseBoolean(getProperty("nologging", "false"));
        if (!nologging) {
            ServiceLoader<LogConfigurator> logservices = ServiceLoader.load(LogConfigurator.class, extensionClassLoader);
            Iterator<LogConfigurator> iter = logservices.iterator();
            if (iter.hasNext()) {
                iter.next().configure(this);
            }
        }

        legacymode = parseBoolean(getProperty("legacymode", "1"));

        strictparsing = parseBoolean(getProperty("strictparsing", "false"));

        // Directories configuration
        autocreate = parseBoolean(getProperty("autocreate", "false"));
        try {
            configdir = prepareDir(getProperty("configdir"), autocreate, true);
        } catch (IOException e) {
            throw new IllegalArgumentException("configuration directory invalid: " + e.getMessage(), e);
        }
        try {
            rrddir = prepareDir(getProperty("rrddir"), autocreate, false);
        } catch (IOException e) {
            // rrddir is mandatory only if default store is rrd
            if(RrdDbStoreFactory.class.getName().equals(getProperty("storefactory", RrdDbStoreFactory.class.getName()))) {
                throw new IllegalArgumentException("probe storage directory invalid: " + e.getMessage(), e);
            }
        }

        // Different place to find the temp directory
        try {
            String tmpDirProperty = getProperty("tmpdir", "");
            if(tmpDirProperty.isEmpty()) {
                tmpDirProperty = System.getProperty("javax.servlet.context.tempdir", "");
            }
            if(tmpDirProperty.isEmpty()) {
                File tempDirPath = new File(System.getProperty("java.io.tmpdir"), "jrds");
                tempDirPath.mkdir();
                tmpDirProperty = tempDirPath.getCanonicalPath();
            }
            if(tmpDirProperty == null) {
                throw new IllegalArgumentException("No temp directory path found");
            }
            tmpdir = prepareDir(tmpDirProperty, autocreate, true);
        } catch (IOException e) {
            throw new IllegalArgumentException("No temp directory defined: " + e.getMessage(), e);
        }

        // Configure the timers
        step = parseInteger(getProperty("step", "300"));
        timeout = parseInteger(getProperty("timeout", "10"));
        numCollectors = parseInteger(getProperty("collectorThreads", "1"));
        slowcollecttime = parseInteger(getProperty("slowcollecttime", Integer.toString(timeout + 1)));
        String propertiesList = getProperty("timers", "");
        if(timeout * 2 >= step) {
            logger.warn("useless default timer, step must be more than twice the timeout");
        }
        if(!propertiesList.trim().isEmpty()) {
            for(String timerName: propertiesList.split(",")) {
                timerName = timerName.trim();
                TimerInfo ti = new TimerInfo();
                ti.step = parseInteger(getProperty("timer." + timerName + ".step", Integer.toString(step)));
                ti.timeout = parseInteger(getProperty("timer." + timerName + ".timeout", Integer.toString(timeout)));
                ti.numCollectors = parseInteger(getProperty("timer." + timerName + ".collectorThreads", Integer.toString(numCollectors)));
                ti.slowCollectTime = parseInteger(getProperty("timer." + timerName + ".slowcollecttime", Integer.toString(ti.timeout + 1)));
                if(ti.timeout * 2 >= ti.step) {
                    logger.warn("useless timer " + timerName + ", step must be more than twice the timeout");
                    break;
                }

                timers.put(timerName, ti);
            }
        }
        // Add the default timer
        TimerInfo ti = new TimerInfo();
        ti.step = step;
        ti.timeout = timeout;
        ti.numCollectors = numCollectors;
        ti.slowCollectTime = slowcollecttime;
        timers.put(Timer.DEFAULTNAME, ti);

        //
        // Tab configuration
        //

        // We search for the tabs list in the property tab
        // spaces are non-significant
        String tabsList = getProperty("tabs");
        if(tabsList != null && !"".equals(tabsList.trim())) {
            this.tabsList = new ArrayList<String>();
            for(String tab: tabsList.split(",")) {
                this.tabsList.add(tab.trim());
            }
        }

        //
        // Security configuration
        //
        security = parseBoolean(getProperty("security", "false"));
        if(security) {
            userfile = getProperty("userfile", "users.properties");

            adminrole = getProperty("adminrole", adminrole);
            adminACL = new ACL.AdminACL(adminrole);

            String defaultRolesString = getProperty("defaultroles", "ANONYMOUS");
            defaultRoles = new HashSet<String>();
            for(String aRole: defaultRolesString.split(",")) {
                defaultRoles.add(aRole.trim());
            }
            defaultACL = new RolesACL(defaultRoles);
            defaultACL = defaultACL.join(adminACL);

            logger.debug("Admin ACL is {}", adminACL);
            logger.debug("Default ACL is {}", defaultACL);
        }

        readonly = parseBoolean(getProperty("readonly", "0"));

        withjmx = parseBoolean(getProperty("jmx", "0"));
        if(withjmx) {
            jmxprops = subKey("jmx");
            if(!jmxprops.containsKey("protocol")) {
                jmxprops.put("protocol", "rmi");
            }
        }
        archivesSet = getProperty("archivesset", ArchivesSet.DEFAULT.getName());

        ServiceLoader<ModuleConfigurator> sl = ServiceLoader.load(ModuleConfigurator.class, extensionClassLoader);
        sl.forEach(i -> {
            Object subconf = i.configure(this);
            if (subconf != null) {
                extendedConfiguration.put(i.getClass(), subconf);
            }
        });
    }

    public File configdir;
    public File rrddir;
    public File tmpdir;
    public String urlpngroot;
    public int slowcollecttime;
    public int step;
    public Map<String, TimerInfo> timers = new HashMap<String, TimerInfo>();
    public int numCollectors;
    public final Set<URI> libspath = new HashSet<URI>();
    public boolean strictparsing = false;
    public ClassLoader extensionClassLoader = getClass().getClassLoader();
    public boolean legacymode;
    public boolean autocreate;
    public int timeout;
    public boolean security = false;
    public String userfile = "/dev/zero";
    public Set<String> defaultRoles = Collections.emptySet();
    public String adminrole = "admin";
    public ACL defaultACL = ACL.ALLOWEDACL;
    public ACL adminACL = ACL.ALLOWEDACL;
    public boolean readonly = false;
    public boolean withjmx = false;
    public Map<String, String> jmxprops = Collections.emptyMap();
    public String archivesSet;
    public static final String FILTERTAB = "filtertab";
    public static final String CUSTOMGRAPHTAB = "customgraph";
    public static final String SUMSTAB = "sumstab";
    public static final String SERVICESTAB = "servicestab";
    public static final String VIEWSTAB = "viewstab";
    public static final String HOSTSTAB = "hoststab";
    public static final String TAGSTAB = "tagstab";
    public static final String ADMINTAB = "adminTab";
    public Map<String, StoreFactory> stores = new HashMap<String, StoreFactory>();
    public StoreFactory defaultStore;
    public Map<Class <? extends ModuleConfigurator>, Object> extendedConfiguration = new HashMap<>();

    public List<String> tabsList = Arrays.asList(FILTERTAB, CUSTOMGRAPHTAB, "@", SUMSTAB, SERVICESTAB, VIEWSTAB, HOSTSTAB, TAGSTAB, ADMINTAB);
}
