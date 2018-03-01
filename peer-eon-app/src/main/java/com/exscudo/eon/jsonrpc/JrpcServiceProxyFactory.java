package com.exscudo.eon.jsonrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.*;

import com.exscudo.eon.jsonrpc.proxy.PeerServiceProxy;
import com.exscudo.jsonrpc.JrpcServiceProxy;
import com.exscudo.peer.core.env.IServiceProxyFactory;
import com.exscudo.peer.core.env.PeerInfo;

/**
 * Implementation of {@code IServiceProxyFactory} with {@code JrpcServiceProxy}.
 * <p>
 * Uses list of supported classes and its implementation.
 *
 * @see IServiceProxyFactory
 * @see JrpcServiceProxy
 */
public class JrpcServiceProxyFactory implements IServiceProxyFactory {
    private final Map<Class<?>, String> clazzMap = new HashMap<>();
    private final Map<Class<?>, Class<?>> clazzMapImpl = new HashMap<>();
    private int readTimeout = 1000;
    private int connectTimeout = 3000;

    public JrpcServiceProxyFactory(Map<String, String> clazzMap, Map<String, String> clazzMapImpl) {
        clazzMap.forEach((String k, String v) -> {
            try {
                Class<?> c = Class.forName(k);
                this.clazzMap.put(c, v);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        clazzMapImpl.forEach((String k, String v) -> {
            try {
                Class<?> ck = Class.forName(k);
                Class<?> cv = Class.forName(v);
                this.clazzMapImpl.put(ck, cv);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        // TODO: should be removed in release network
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    private URL getUrl(PeerInfo peer, String endpoint) {

        URL url;
        try {

            url = new URL("https://" +
                                  peer.getAddress() +
                                  ((new URL("https://" + peer.getAddress())).getPort() < 0 ? ":8443" : "") +
                                  endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    JrpcServiceProxy getProxy(URL url) {
        JrpcServiceProxy proxy = new JrpcServiceProxy(url, ObjectMapperProvider.createModule());

        proxy.setReadTimeout(getReadTimeout());
        proxy.setConnectTimeout(getConnectTimeout());

        return proxy;
    }

    @Override
    public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(clazz);

        String endpoint = "/peer";
        return createProxy(getUrl(peer, endpoint), clazz);
    }

    <TService> TService createProxy(URL url, Class<TService> clazz) {

        try {

            JrpcServiceProxy proxy = getProxy(url);

            String serviceName = clazzMap.get(clazz);

            Class<?> implClass = clazzMapImpl.get(clazz);
            Object instance = implClass.newInstance();

            if (instance instanceof PeerServiceProxy) {
                ((PeerServiceProxy) instance).setProxy(proxy);
                ((PeerServiceProxy) instance).setServiceName(serviceName);
            }

            @SuppressWarnings("unchecked")
            TService service = (TService) instance;

            return service;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
