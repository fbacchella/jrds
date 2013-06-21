package jrds.store;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import jrds.JrdsSample;
import jrds.Probe;

public class GraphiteConnection {

    private final String host;
    private final int port;
    private final String commonPrefix;

    private Socket socket;
    private PrintWriter writer;

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
            write(msg);
        }
        writer.flush();
    }

    private synchronized void write(String msg) throws IOException {
        writer.println(msg);
    }

    public synchronized void ensureGraphiteConnection() throws IOException {
        boolean socketIsValid = false;
        try {
            socketIsValid = socket != null &&
                    socket.isConnected()
                    && socket.isBound()
                    && !socket.isClosed()
                    && !socket.isInputShutdown()
                    && !socket.isOutputShutdown();
        } catch (Exception e) {
        }
        if (!socketIsValid) {
            socket = new Socket(host, port);
            socket.setKeepAlive(true);
            //flush is done manually, so try to buffer as much as possible
            socket.setTcpNoDelay(false);
            socket.setSendBufferSize(8192);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), false);
        }
    }

    public String getPrefix(Probe<?, ?> probe) {
        return commonPrefix + probe.getQualifiedName().replace('/', '.');
    }

}
