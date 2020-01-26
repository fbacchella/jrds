package fr.jrds.pcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.jrds.pcp.credentials.CVersion;
import fr.jrds.pcp.kaitai.EthernetFrame;
import fr.jrds.pcp.kaitai.Ipv4Packet;
import fr.jrds.pcp.kaitai.Pcap;
import fr.jrds.pcp.kaitai.TcpSegment;
import io.kaitai.struct.ByteBufferKaitaiStream;

public class TestCapture extends Tester {

    private class ByteArrayTransport implements Transport {

        private final ByteBuffer content;

        ByteArrayTransport(byte[] p) throws IOException {
            this.content = ByteBuffer.wrap(p);
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void read(ByteBuffer buffer)
                        throws InterruptedException, IOException {
            byte[] b = new byte[buffer.remaining()];
            content.get(b);
            buffer.put(b);
        }

        @Override
        public void write(ByteBuffer buffer)
                        throws InterruptedException, IOException {
            byte[] sent = new byte[buffer.remaining()];
            byte[] ref = new byte[buffer.remaining()];
            content.get(ref);
            buffer.get(sent);
            System.out.println(Arrays.toString(ref));
            System.out.println(Arrays.toString(sent));
            Assert.assertArrayEquals(ref, sent);
        }

        @Override
        public Waiter getWaiter() {
            return (o) -> {};
        }

        @Override
        public ByteOrder getByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

    }

    private static Set<FEATURES> serverFeatures = new HashSet<>();
    static {
        serverFeatures.add(FEATURES.SECURE);
        serverFeatures.add(FEATURES.COMPRESS);
        serverFeatures.add(FEATURES.AUTH);
        serverFeatures.add(FEATURES.SECURE_ACK);
        serverFeatures.add(FEATURES.CONTAINER);
        serverFeatures.add(FEATURES.LABELS);
    }

    @Test
    public void testdpr() throws IOException, PCPException, InterruptedException {
        try (Connection cnx = readPcap("pcap/disk.partitions.read.pcap")) {
            cnx.setFrom(0);
            Assert.assertEquals(serverFeatures, cnx.startClient());
            cnx.authentication(new CVersion());
            String[] names = cnx.getNames(0, "disk.partitions.read").toArray(new String[] {});
            Map<String, PmId> id = cnx.resolveName(names);
            cnx.profile();
            ResultData r = cnx.fetchValue(new ArrayList<>(id.values()));
            Assert.assertEquals(Instant.ofEpochSecond(1579898739,468458000), r.getDate());
            Assert.assertEquals(new ResultInstance(0, Long.valueOf(3992)), r.getIds().get(new PmId(251668480)).get(0));
            Assert.assertEquals(new ResultInstance(1, Long.valueOf(44819)), r.getIds().get(new PmId(251668480)).get(1));
            Assert.assertEquals(new ResultInstance(2, Long.valueOf(185)), r.getIds().get(new PmId(251668480)).get(2));
            Assert.assertEquals(3, r.getIds().get(new PmId(251668480)).size());
        }
    }

    @Test
    public void testempty() throws IOException, PCPException, InterruptedException {
        try (Connection cnx = readPcap("pcap/empty.pcap")) {
            cnx.setFrom(12710);
            Assert.assertEquals(serverFeatures, cnx.startClient());
            cnx.authentication(new CVersion());
            cnx.setFrom(0);
            cnx.checkName(0, "empty");
            Assert.fail();
        } catch (PCPException ex) {
            Assert.assertEquals(ERROR.NAME, ex.getError());
        }
    }

    private Connection readPcap(String ressource) throws IOException, PCPException, InterruptedException {
        InputStream in = TestCapture.class.getClassLoader().getResourceAsStream(ressource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len = 0;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        ByteBufferKaitaiStream pcapcontent = new ByteBufferKaitaiStream(out.toByteArray());
        Pcap pcap = new Pcap(pcapcontent);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        pcap.packets().stream()
        .map(p -> (EthernetFrame)p.body())
        .map(p -> (Ipv4Packet)p.body())
        .map(Ipv4Packet::body)
        .map(p -> (TcpSegment)p.body())
        .forEach(t -> {
            int options = (t.b12() >>4) * 4 - 20;
            byte[] body = t.body();
            byte[] payload = Arrays.copyOfRange(body, options, body.length);
            try {
                content.write(payload);
            } catch (IOException e) {
            }
        });
        Transport t = new ByteArrayTransport(content.toByteArray());
        return new Connection(t);
    }

}
