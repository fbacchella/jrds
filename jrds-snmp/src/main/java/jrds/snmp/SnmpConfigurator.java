package jrds.snmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.jrds.snmpcodec.MibStore;
import fr.jrds.snmpcodec.OIDFormatter;
import fr.jrds.snmpcodec.parsing.MibLoader;
import jrds.PropertiesManager;
import jrds.configuration.ModuleConfigurator;

public class SnmpConfigurator extends ModuleConfigurator {
    
    static MibStore resolver;

    @Override
    public Object configure(PropertiesManager pm) {
        String propertiesmibDirs = pm.getProperty("mibdirs", "/usr/share/snmp");
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

}
