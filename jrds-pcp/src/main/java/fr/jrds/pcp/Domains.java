package fr.jrds.pcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Domains {

    public static final String PROPERTY = "jrds.pcp.domains";
    public static final Domains instance = new Domains();

    private Map<String, Short> domainsByName = new HashMap<>();
    private Map<Short, String> domainsById = new HashMap<>();
    private Domains() {
        try {
            String domainFile = System.getProperty(PROPERTY);
            InputStream ressource;
            if (domainFile != null) {
                ressource = Paths.get(domainFile).toUri().toURL().openStream();
            } else {
                ressource = Domains.class.getClassLoader().getResourceAsStream("stdpmid");
            }
            load(ressource);
        } catch (IOException e) {
        }
    }

    public String getDomain(short id) {
        if (domainsById.containsKey(id)) {
            return domainsById.get(id);
        } else {
            return Short.toString(id);
        }
    }

    public short getDomain(String name) {
        if (domainsByName.containsKey(name)) {
            return domainsByName.get(name);
        } else {
            return -1;
        }
    }

    private void load(InputStream ressource) throws NumberFormatException, IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(ressource, StandardCharsets.UTF_8));
        Pattern linepattern = Pattern.compile("^([A-Z]+)\\s+([0-9]+).*");
        String line;
        while((line = r.readLine()) != null) {
            Matcher m = linepattern.matcher(line);
            if (m.matches()) {
                String domainname = m.group(1);
                String domainIdStr = m.group(2);
                Short domainId = Short.parseShort(domainIdStr);
                domainsByName.put(domainname, domainId);
                domainsById.put(domainId, domainname);
            }
        }
    }
}
