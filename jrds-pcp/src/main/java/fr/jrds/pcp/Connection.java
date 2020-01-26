package fr.jrds.pcp;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.credentials.Credential;
import fr.jrds.pcp.pdu.Creds;
import fr.jrds.pcp.pdu.Desc;
import fr.jrds.pcp.pdu.DescReq;
import fr.jrds.pcp.pdu.Fetch;
import fr.jrds.pcp.pdu.Instance;
import fr.jrds.pcp.pdu.InstanceReq;
import fr.jrds.pcp.pdu.PnmsIds;
import fr.jrds.pcp.pdu.PnmsNames;
import fr.jrds.pcp.pdu.PnmsTraverse;
import fr.jrds.pcp.pdu.Profile;
import fr.jrds.pcp.pdu.Result;
import fr.jrds.pcp.pdu.Start;

public class Connection implements Closeable {

    @FunctionalInterface
    public static interface Waiter {
        public void waitFor(int op) throws InterruptedException, IOException;
    }

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ProtocolHandler phandler;

    public Connection(InetSocketAddress isa, long timeout) throws IOException, PCPException, InterruptedException {
        this(new PlainTcpTransport(isa, timeout));
        logger.info("Connected to {}", isa);
    }

    public Connection(Transport trp) throws IOException, PCPException, InterruptedException {
        phandler = new ProtocolHandler(trp);
    }

    public ServerInfo startClient() throws IOException, PCPException, InterruptedException {
        Start start = phandler.receive();
        return start.getServerInfo();
    }

    public void startServer(ServerInfo si) throws IOException, PCPException, InterruptedException {
        Start start = new Start();
        start.setServerInfo(si);
        phandler.send(start);
    }

    @Override
    public void close() throws IOException {
        phandler.close();
    }

    public void authentication(Credential nc) throws IOException, PCPException, InterruptedException {
        Creds creds = new Creds();
        creds.addCred(nc);
        phandler.send(creds);
    }

    public boolean checkName(int subType, String name) throws IOException, PCPException, InterruptedException {
        PnmsTraverse traverse = new PnmsTraverse();
        traverse.setSubtype(subType);
        traverse.setName(name);
        phandler.send(traverse);

        PnmsNames received = phandler.receive();
        PnmsNames namesPdu = (PnmsNames)received;
        return name.equals(namesPdu.getNames().get(0)) && namesPdu.getNames().size() == 1;
    }

    public List<String> getNames(int subType, String name) throws IOException, PCPException, InterruptedException {
        PnmsTraverse traverse = new PnmsTraverse();
        traverse.setSubtype(subType);
        traverse.setName(name);
        phandler.send(traverse);

        PnmsNames received = phandler.receive();
        return received.getNames();
    }

    public Map<String, PmId> resolveName(String... names) throws IOException, PCPException, InterruptedException {
        PnmsNames pnames = new PnmsNames();
        pnames.setStatus(0);
        Arrays.stream(names).forEach(pnames::add);
        phandler.send(pnames);

        PnmsIds received = phandler.receive();
        Map<String, PmId> namesMapping = new LinkedHashMap<>(names.length);
        for (int i = 0 ; i < names.length ; i++) {
            namesMapping.put(names[i], received.getIds().get(i));
        }
        return namesMapping;
    }

    public Map<String, PmId> resolveName(List<String> names) throws IOException, PCPException, InterruptedException {
        PnmsNames pnames = new PnmsNames();
        pnames.setStatus(0);
        names.forEach(pnames::add);
        phandler.send(pnames);

        PnmsIds received = phandler.receive();
        Map<String, PmId> namesMapping = new LinkedHashMap<>(names.size());
        for (int i = 0 ; i < names.size() ; i++) {
            namesMapping.put(names.get(i), received.getIds().get(i));
        }
        return namesMapping;
    }

    public void profile() throws IOException, InterruptedException {
        Profile p = new Profile();
        phandler.send(p);
    }

    public ResultData fetchValue(List<PmId> ids) throws IOException, PCPException, InterruptedException {
        Fetch fetch = new Fetch();
        fetch.setContextNumber(0);
        fetch.setTimeValue(Instant.EPOCH);
        ids.forEach(fetch::addPmId);
        phandler.send(fetch);
        Result r = phandler.receive();
        return r.getRd();
    }

    public ResultData fetchValue(PmId... ids) throws IOException, PCPException, InterruptedException {
        Fetch fetch = new Fetch();
        fetch.setContextNumber(0);
        fetch.setTimeValue(Instant.EPOCH);
        Arrays.stream(ids).forEach(fetch::addPmId);
        phandler.send(fetch);
        Result r = phandler.receive();
        return r.getRd();
    }

    public PmDesc getDescription(PmId id) throws IOException, PCPException, InterruptedException {
        DescReq desReq = new DescReq();
        desReq.setId(id);
        phandler.send(desReq);
        Desc d = phandler.receive();
        return d.getDesc();
    }

    public List<InstanceInfo> getInstance(int instanceDomain, int instance, String instanceName) throws IOException, PCPException, InterruptedException {
        InstanceReq ir = new InstanceReq();
        ir.setInstanceDomain(instanceDomain);
        ir.setInstance(instance);
        ir.setInstanceName(instanceName);
        phandler.send(ir);
        Instance i = phandler.receive();
        return i.getInstances();
    }

    public void setFrom(int from) {
        phandler.setFrom(from);
    }

    public int getFrom() {
        return phandler.getFrom();
    }

}
