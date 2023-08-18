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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.snmp4j.smi.OID;

import fr.jrds.snmpcodec.MibStore;
import fr.jrds.snmpcodec.OIDFormatter;
import fr.jrds.snmpcodec.parsing.MibLoader;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.configuration.ModuleConfigurator;

public class SnmpConfigurator extends ModuleConfigurator {

    static final private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        try {
            Collections.list(SnmpConfigurator.class.getClassLoader().getResources("oidmap.properties")).stream()
            .map(u -> {
                try {
                    return u.openStream();
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Can't read OID mapping from " + u, ex);
                }
            })
            .forEach(oidmapreader);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Can't find oidmap.properties", ex);
        }
        String oidmapfiles = pm.getProperty("oidmaps", "");
        Arrays.stream(oidmapfiles.split(";"))
        .map(String::trim)
        .filter(i -> ! i.isEmpty())
        .map(failopener)
        .forEach(oidmapreader);
        oidprops.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().toString(), new OID(e.getValue().toString())))
        .forEach(e -> SnmpCollectResolver.oidmapping.put(e.getKey(), e.getValue()));
        // Using the full mib parser
        String propertiesmibDirs = pm.getProperty("mibdirs", "/usr/share/snmp/mibs");
        if(!propertiesmibDirs.trim().isEmpty()) {
            List<String> snmpMibDirs = new ArrayList<>();
            for( String i: propertiesmibDirs.split(";")) {
                i = i.trim();
                snmpMibDirs.add(Paths.get(i).toString());
            }
            if (snmpMibDirs.size() > 0) {
                String[] paths_list = snmpMibDirs.toArray(new String[0]);
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
                    }).toArray(Path[]::new);
                } else {
                    return new Path[] {i};
                }
            } catch (IOException e1) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .forEach(loader::load);

        return loader.buildTree();
    }

    public static void SaveMapping(String path) throws IOException {
        Properties oidprops = new Properties();
        SnmpCollectResolver.oidmapping.forEach((k,v) -> oidprops.setProperty(k, v.toDottedString()));
        try (Writer dest =new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.US_ASCII)) {
            oidprops.store(dest, "");
        }
    }

}
