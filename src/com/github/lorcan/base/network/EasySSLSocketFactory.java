package com.github.lorcan.base.network;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class EasySSLSocketFactory extends SSLSocketFactory {

    protected SSLContext Cur_SSL_Context;

    public EasySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, UnrecoverableKeyException {
        super(truststore);
        try {
            Cur_SSL_Context = SSLContext.getInstance("TLS");
        } catch (Exception e) {
            Cur_SSL_Context = SSLContext.getInstance("LLS");
        }
        Cur_SSL_Context.init(null, new TrustManager[] { new EasyX509TrustManager() }, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException {
        return Cur_SSL_Context.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return Cur_SSL_Context.getSocketFactory().createSocket();
    }

}
