package jrds.pcp.probe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import fr.jrds.pcp.Connection;
import fr.jrds.pcp.InstanceInfo;
import fr.jrds.pcp.PCPException;
import fr.jrds.pcp.PmDesc;
import fr.jrds.pcp.PmId;
import fr.jrds.pcp.ResultInstance;
import fr.jrds.pcp.credentials.CVersion;
import jrds.factories.ProbeBean;
import lombok.Getter;
import lombok.Setter;

@ProbeBean({"port"})
public class PcpConnexion extends jrds.starter.Connection<Connection> {

    private static final Map<String, PmId> idCache = new ConcurrentHashMap<>();
    static {
        idCache.put("kernel.all.uptime", new PmId(0xf006800));
    }

    private Connection cnx;

    @Getter @Setter
    private int port = 44321;

    private final Map<String, Map<String, Number>> valueCache = new HashMap<>();
    private final Function<String, PmId> lookup = (s) -> {
        try {
            return cnx.resolveName(s).get(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PCPException | InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    };

    @Override
    public boolean startConnection() {
        boolean success = false;
        try {
            cnx = new Connection(new InetSocketAddress(getResolver().getInetAddress(), port), getTimeout() * 1000L);
            cnx.startClient();
            cnx.authentication(new CVersion());
            success = true;
        } catch (IOException e) {
            log(Level.ERROR, e, "Can't connect to pmcd: %s", e);
        } catch (PCPException e) {
            log(Level.ERROR, e, "Can't talk to pmcd", e);
        } catch (InterruptedException e) {
            log(Level.ERROR, e, "Interrupted");
            Thread.currentThread().interrupt();
        } finally {
            if (! success && cnx != null) {
                try {
                    cnx.close();
                    cnx = null;
                } catch (IOException e) {
                }
            }
        }
        return success;
    }

    @Override
    public void stopConnection() {
        try {
            cnx.close();
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection to pcmd reset: %s", e);
        }
        valueCache.clear();
        cnx = null;
    }

    @Override
    public long setUptime() {
        PmId kernelUptime = idCache.computeIfAbsent("kernel.all.uptime", lookup);
        try {
            cnx.profile();
            Number uptime = cnx.fetchValue(kernelUptime).getIds().get(kernelUptime).get(0).getCheckedValue();
            return uptime.longValue();
        } catch (IOException e) {
            log(Level.ERROR, e, "Can't connect to pmcd: %s", e);
            return 0;
        } catch (PCPException e) {
            log(Level.ERROR, e, "Can't talk to pmcd", e);
            return 0;
        } catch (InterruptedException e) {
            log(Level.ERROR, "Interrupted");
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    @Override
    public Connection getConnection() {
        return cnx;
    }

    public Map<String, Number> getValue(String key, Collection<String> metricsnames) throws IOException, InterruptedException, PCPException {
        try {
            Map<String, Number> values = new HashMap<>(metricsnames.size());
            List<String> missingMetrics = metricsnames.stream().filter(s -> ! valueCache.containsKey(s)).collect(Collectors.toList());
            if (! missingMetrics.isEmpty()) {
                List<PmId> idsToCollect = missingMetrics.stream().map(i -> idCache.computeIfAbsent(i, lookup)).collect(Collectors.toList());
                Map<PmId, List<ResultInstance>> lr = cnx.fetchValue(idsToCollect).getIds();
                for (int i = 0 ; i < missingMetrics.size() ; i++) {
                    String name = missingMetrics.get(i);
                    PmId id = idsToCollect.get(i);
                    Map<String, Number> idvalues = new HashMap<>(lr.get(id).size());
                    if (lr.get(id).size() > 1) {
                        PmDesc desc = cnx.getDescription(id);
                        List<InstanceInfo> lin = cnx.getInstance(desc.getIndom(), 0xffffffff, "");
                        for (int j = 0 ; j < lin.size() ; j++) {
                            idvalues.put(lin.get(j).getName(), lr.get(id).get(j).getCheckedValue());
                        }
                    } else if (! lr.get(id).isEmpty()) {
                        idvalues.put(key, lr.get(id).get(0).getCheckedValue());
                    }
                    valueCache.put(name, idvalues);
                }
            }
            metricsnames.forEach(mn -> {
                Optional.ofNullable(valueCache.get(mn).get(key)).ifPresent(n -> values.put(mn, n));
            });
            return values;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            } else if (e.getCause() instanceof PCPException) {
                throw (PCPException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public Map<String, Number> getValue(Collection<String> metricsnames) throws IOException, InterruptedException, PCPException {
        return getValue("", metricsnames);
    }

}
