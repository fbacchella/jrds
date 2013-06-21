package jrds.store;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import jrds.JrdsSample;
import jrds.Probe;

import org.apache.log4j.Logger;

public class GraphiteConnection {
    static private final Logger logger = Logger.getLogger(GraphiteConnection.class);

    private final String host;
    private final int port;
    private final String commonPrefix;

    private Socket socket;
    private Writer writer;

    public GraphiteConnection(String host, int port, String prefix) {
        super();
        this.host = host;
        this.port = port;
        this.commonPrefix = (prefix != null && ! prefix.isEmpty()) ? prefix + "." : "";
    }

    public void send(Probe<?,?> probe, JrdsSample sample) throws IOException {
        String prefix = getPrefix(probe);
        for(Map.Entry<String, Number> e: sample.entrySet()) {
            String msg = prefix + "." + e.getKey() + " " + e.getValue() + " " + (int) (sample.getTime().getTime() / 1000);
            ensureGraphiteConnection();
            try {
                write(msg);
            } catch (IOException e1) {
                failed(e1);
            }
        }
        try {
            writer.flush();
        } catch (IOException e1) {
            failed(e1);
        }
    }

    private synchronized void write(String msg) throws IOException {
        writer.write(msg + '\n');
    }

    private void failed(Exception e) {
        logger.warn("message send failed: " + e.getMessage());
        try {
            socket.close();
        } catch (IOException e2) {
        }
        socket = null;
    }

    public synchronized void ensureGraphiteConnection() throws IOException {
        boolean socketIsValid = false;
        socketIsValid = socket != null &&
                socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
        if (!socketIsValid) {
            socket = new Socket();
            InetSocketAddress graphiteServer = new InetSocketAddress(host, port);
            socket.setKeepAlive(true);
            socket.setSoTimeout(1000);
            //flush is done manually, so try to buffer as much as possible
            socket.setTcpNoDelay(false);
            socket.setSendBufferSize(8192);
            socket.connect(graphiteServer, 1000);
            writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
        }
    }

    public String getPrefix(Probe<?, ?> probe) {
        String pname = probe.getName();
        String hostname = probe.getHost().getName();
        if(hostname.contains(".")) {
            String[] part = hostname.split("\\.");
            StringBuilder b = new StringBuilder(hostname.length());
            for(int i=part.length -1; i>=0; i--) {
                b.append(part[i]);
                b.append(".");
            }
            hostname = b.toString();
        }
        else {
            hostname = hostname + ".";
        }
        if(pname.contains(".")) {
            pname = pname.replace('.', '_');
        }
        return commonPrefix + hostname + pname;
    }

}
