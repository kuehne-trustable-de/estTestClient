package de.trustable.ca3s.est;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;

public class TLSServerHelper {

    public static String getServerCertificates(String host, int port)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyManagementException {

        // this is rare case where a trustAll-Manager makes sense as the details of the certificate get checked later on
        // please think twice before using the trustAll-Manager in a productive context !!
        TrustManager[] trustAllCerts = {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        Certificate[] serverCerts;
        SSLSocket sslSocket = null;
        try {
            // Code for creating a client side SSLSocket
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null,
                    trustAllCerts,
                    new SecureRandom());
            SSLSocketFactory sslsf = sslContext.getSocketFactory();

            sslSocket = (SSLSocket) sslsf.createSocket(host, port);

            // Get an SSLParameters object from the SSLSocket
            SSLParameters sslp = sslSocket.getSSLParameters();

            SNIHostName serverName = new SNIHostName(host);
            sslp.setServerNames(Collections.singletonList(serverName));

            // Populate the SSLSocket object with the SSLParameters object
            sslSocket.setSSLParameters(sslp);

            sslSocket.startHandshake();
            serverCerts = sslSocket.getSession().getPeerCertificates();

        } finally {
            if (sslSocket != null) {
                sslSocket.close();
            }
        }

        StringBuilder pemCerts = new StringBuilder();
        for (Certificate cert : serverCerts) {
            X509Certificate x509 = (X509Certificate) cert;
            System.out.println("found cert: " + x509.getSubjectX500Principal().getName());
            pemCerts.append("-----BEGIN CERTIFICATE-----\n")
                    .append(Base64.getMimeEncoder().encodeToString(cert.getEncoded())).append("\n")
                    .append("-----END CERTIFICATE-----\n");
        }
        return pemCerts.toString();
    }
}