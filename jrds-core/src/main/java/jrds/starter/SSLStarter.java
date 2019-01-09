package jrds.starter;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Level;

import jrds.PropertiesManager;

public class SSLStarter extends Starter {

    // Create a trust manager that does not validate certificate chains
    private static final X509TrustManager trustAllCerts = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    };

    private String protocol = null;
    private boolean strict = false;
    private String truststore = null;
    private String trustpassword = null;
    private String format = null;
    private String securerandom = null;

    private SSLContext sc = null;

    /*
     * (non-Javadoc)
     * 
     * @see jrds.starter.Starter#configure(jrds.PropertiesManager)
     */
    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        protocol = pm.getProperty("ssl.protocol", null);
        // Default is false, because it was the default setting in previous release
        strict = pm.parseBoolean(pm.getProperty("ssl.strict", "false"));
        truststore = pm.getProperty("ssl.truststore", null);
        trustpassword = pm.getProperty("ssl.trustpassword", "");
        format = pm.getProperty("ssl.trusttype", KeyStore.getDefaultType());
        securerandom = pm.getProperty("ssl.securerandom", null);
    }

    @Override
    public boolean start() {
        try {
            sc = protocol != null ? SSLContext.getInstance(protocol) : SSLContext.getDefault();
            if(!"Default".equals(sc.getProtocol())) {
                KeyManager[] km = null;
                TrustManager[] tm = null;
                SecureRandom sr = null;
                if ( ! strict ) {
                    tm = new TrustManager[] { trustAllCerts };
                } else if (truststore != null) {
                    KeyStore ks = KeyStore.getInstance(format);
                    ks.load(new FileInputStream(truststore), trustpassword.toCharArray());

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ks);
                    tm = tmf.getTrustManagers();

                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(ks, trustpassword.toCharArray());
                    km = kmf.getKeyManagers();
                }
                if (securerandom != null) {
                    sr = SecureRandom.getInstance(securerandom);
                }
                // Default SSLContext is already initialized
                sc.init(km, tm, sr);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | CertificateException | IOException | UnrecoverableKeyException e) {
            log(Level.ERROR, e, "failed to init ssl: %s", e.getMessage());
            sc = null;
            return false;
        }
        log(Level.DEBUG, "Using SSL context %s", sc);
        return sc != null;
    }

    public SSLContext getContext() {
        return sc;
    }

    public Socket connect(String host, int port) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        SocketFactory ss = getLevel().find(SocketFactory.class);
        Socket s = ss.createSocket(host, port);

        SSLSocketFactory ssf = getContext().getSocketFactory();
        s = ssf.createSocket(s, host, port, true);
        log(Level.DEBUG, "done SSL handshake for %s", host);
        return s;
    }

    @Override
    public boolean isStarted() {
        return sc != null;
    }

}
