package jrds.snmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;

import fr.jrds.snmpcodec.MibStore;
import fr.jrds.snmpcodec.OIDFormatter;
import fr.jrds.snmpcodec.parsing.MibLoader;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.configuration.ModuleConfigurator;

public class SnmpConfigurator extends ModuleConfigurator {

    static final private Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    static MibStore resolver;

    @Override
    public Object configure(PropertiesManager pm) {
        SnmpCollectResolver.oidmapping.clear();
        // Try to resolve OID from a simple properties file
        Properties oidprops = new Properties();
        Consumer<InputStream> oidmapreader = is -> {
            try (Reader mapread = new InputStreamReader(is, StandardCharsets.US_ASCII)) {
                oidprops.load(mapread);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        };
        Function<String,InputStream> failopener = (t -> {
            try {
                Util.log(null, logger, Level.DEBUG, null, "Looking for OID map in %s", t);
                return new FileInputStream(t);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Can't read OID mapping from " + t, e);
            }
        });
        oidmapreader.accept(SnmpConfigurator.class.getClassLoader().getResourceAsStream("oidmap.properties"));
        String oidmapfiles = pm.getProperty("oidmaps", "");
        Arrays.stream(oidmapfiles.split(";"))
        .map(String::trim)
        .filter(i -> ! i.isEmpty())
        .map(failopener)
        .forEach(oidmapreader);
        oidprops.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<String, OID>(e.getKey().toString(), new OID(e.getValue().toString())))
        .forEach(e -> SnmpCollectResolver.oidmapping.put(e.getKey(), e.getValue()));
        ;
        // Using the full mib parser
        String propertiesmibDirs = pm.getProperty("mibdirs", "/usr/share/snmp/mibs");
        if(!propertiesmibDirs.trim().isEmpty()) {
            List<String> snmpMibDirs = new ArrayList<>();
            for( String i: propertiesmibDirs.split(";")) {
                i = i.trim();
                snmpMibDirs.add(Paths.get(i).toString());
            }
            if (snmpMibDirs.size() > 0) {
                String[] paths_list = snmpMibDirs.toArray(new String[snmpMibDirs.size()]);
                return OIDFormatter.register(paths_list);
            }
        }
        return null;
    }

    public MibStore register(String... mibdirs) {
        MibLoader loader = new MibLoader();
        Arrays.stream(mibdirs)
        .map(Paths::get)
        .filter( i-> {
            try {
                File dest = i.toRealPath().toFile();
                return dest.isDirectory() || dest.isFile();
            } catch (IOException e) {
                return false;
            }
        })
        .map( i -> {
            try {
                if (i.toRealPath().toFile().isDirectory()) {
                    return Files.list(i).filter(j -> {
                        try {
                            return j.toRealPath().toFile().isFile();
                        } catch (IOException e1) {
                            return false;
                        }
                    }).toArray(j -> new Path[j]);
                } else {
                    return new Path[] {i};
                }
            } catch (IOException e1) {
                return null;
            }
        })
        .filter(i -> i != null)
        .forEach( i-> loader.load(i));

        return loader.buildTree();
    }

    public static void SaveMapping(String path) throws FileNotFoundException, IOException {
        Properties oidprops = new Properties();
        SnmpCollectResolver.oidmapping.forEach((k,v) -> oidprops.setProperty(k, v.toDottedString()));
        try (Writer dest =new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.US_ASCII)) {
            oidprops.store(dest, "");
        }
    }

}
