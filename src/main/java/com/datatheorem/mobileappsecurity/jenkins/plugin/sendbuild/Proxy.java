package com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild;

import hudson.model.TaskListener;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContexts;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class Proxy {
    private TaskListener listener;

    private String hostname;
    private int port;
    private String username;
    private String password;
    private boolean isUnsecureAllowed;
    public Proxy(TaskListener listener, String hostname, int port, String username, String password, boolean isUnsecureAllowed) {
        this.listener = listener;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.isUnsecureAllowed = isUnsecureAllowed;

        if (!username.isEmpty() && password.isEmpty()){
            listener.getLogger().println("Proxy password should be specify when proxy username is set");

            throw new IllegalArgumentException("Proxy password can't be empty if proxy username is set");
        }
    }

    public void add_to_http_client(HttpClientBuilder clientBuilder) {
        clientBuilder.useSystemProperties();
        if (isUnsecureAllowed)
            try {
                listener.getLogger().println("Insecure connection option is check: bypassing SSL Validation");
                SSLConnectionSocketFactory acceptAllCertificate = new SSLConnectionSocketFactory(
                        SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                        NoopHostnameVerifier.INSTANCE);

                clientBuilder.setSSLSocketFactory(acceptAllCertificate);
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                listener.getLogger().println(e.getMessage());
            }

        clientBuilder.setProxy(new HttpHost(hostname, port));

        if (username != null && !username.isEmpty()) {


            listener.getLogger().println("Proxy is set using username/password authentification");

            // Add the User/Password proxy authentication
            NTCredentials ntCreds = new NTCredentials(username, password, "", "");
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(hostname, port), ntCreds);
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
            clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
        }
    }
}
