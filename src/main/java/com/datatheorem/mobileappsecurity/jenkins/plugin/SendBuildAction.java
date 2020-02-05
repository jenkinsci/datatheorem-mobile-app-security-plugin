package com.datatheorem.mobileappsecurity.jenkins.plugin;


import com.datatheorem.mobileappsecurity.jenkins.plugin.utils.RemoteAgentStreamBody;
import hudson.FilePath;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Response message of SendBuildAction
 */
class SendBuildMessage {
    /*
      Response message of SendBuildAction
      @attr:
        boolean success: true if the sendBuild perform correctly false otherwise
        String message: Information to print to customers
    */

    public final boolean success;
    public final String message;

    SendBuildMessage(boolean success, String message) {
        this.success = success;
        this.message = message;

    }
}

/**
 * Upload a build to Data Theorem Upload Api.
 * <p>
 * The action uses the secret Upload API Key and the path to the build which has to be sent to Data Theorem.
 * The perform action first call data theorem upload_init endpoint with the apiKey to get the upload link.
 * Then if the API Key is correct the action send the build to Data Theorem using the upload link.
 * return : SendBuildMessage with success value and the body response
 * </p>
 */

class SendBuildAction {

    private final String apiKey;
    private final PrintStream logger; // Jenkins logger
    private final FilePath workspace;
    private String uploadUrl;
    private String version = "2.0.0";
    private final String proxyHostname;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private final boolean proxyUnsecureConnection;


    SendBuildAction(String apiKey, PrintStream logger, FilePath workspace) {
        /*
         * Constructor of the SendBuildAction
         * @param :
         *   apiKey : Secret Upload API Key to access Data Theorem Upload API
         *   logger : Jenkins Logger to show uploading steps on the console output
         */

        this.apiKey = apiKey;
        this.logger = logger;
        this.workspace = workspace;
        this.proxyHostname = null;
        this.proxyPort = 0;
        this.proxyUsername = null;
        this.proxyPassword = null;
        this.proxyUnsecureConnection = false;
    }

    SendBuildAction(String apiKey,
                    PrintStream logger,
                    FilePath workspace,
                    String proxyHostname,
                    int proxyPort,
                    String proxyUsername,
                    String proxyPassword,
                    boolean proxyUnsecureConnection) {
        /*
         * Constructor of the SendBuildAction with a proxy setting
         * @param :
         *   apiKey : Secret Upload API Key to access Data Theorem Upload API
         *   logger : Jenkins Logger to show uploading steps on the console output
         */

        this.apiKey = apiKey;
        this.logger = logger;
        this.workspace = workspace;
        this.proxyHostname = proxyHostname;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyUnsecureConnection = proxyUnsecureConnection;
    }

    public SendBuildMessage perform(String buildPath, String sourceMapPath, Boolean isBuildStoredInArtifactFolder) {
        /*
         * Perform the SendBuildAction : send the build to Data Theorem Upload API
         * @param :
         *    buildPath : Path of the build we want to send to Data Theorem
         * @return :
         *    SendBuildMessage containing the success or the failure information about the SendBuild process
         */

        SendBuildMessage uploadInitMessage = uploadInit();
        // If we successfully get an upload link : Send the build at the upload url
        if (uploadInitMessage.success && !uploadInitMessage.message.equals("")) {
            return uploadBuild(buildPath, sourceMapPath, isBuildStoredInArtifactFolder);
        } else {
            return uploadInitMessage;
        }
    }

    SendBuildMessage uploadInit() {
        /*
         * Get a temporary upload link from Data Theorem using the secret apiKey
         * @return:
         *   SendBuildMessage containing the success or the failure information of upload_init call
         */

        try {
            if (apiKey.startsWith("APIKey")) {
                return new SendBuildMessage(
                        false,
                        "Error your upload APIKey shouldn't start with \"APIKey\""
                );
            }
            if (apiKey.equals("")) {
                return new SendBuildMessage(
                        false,
                        "Upload APIKey secret key is empty"
                );
            }
        } catch (java.lang.NullPointerException e) {
            return new SendBuildMessage(
                    false,
                    "Missing Data Theorem upload APIKey:\n" +
                            "Ensure \"DATA_THEOREM_UPLOAD_API_KEY\" is set in Credentials Binding"
            );
        }

        logger.println("Retrieving the upload URL from Data Theorem ...");
        try {

            HttpResponse response = uploadInitRequest();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity, "UTF-8");

                // Catch forbidden access when the API Key is wrong
                if (response.getStatusLine().getStatusCode() == 401) {
                    return new SendBuildMessage(
                            false,
                            "Data Theorem upload_init call Forbidden Access: " + responseString
                    );
                }

                // If the status code is 200 verify the response body and update hash and sessionId
                else if (response.getStatusLine().getStatusCode() == 200) {

                    try {
                        JSONParser parser = new JSONParser();
                        JSONObject jsonResponse = (JSONObject) parser.parse(responseString);
                        this.uploadUrl = jsonResponse.get("upload_url").toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new SendBuildMessage(
                                false,
                                "Data Theorem upload_init wrong payload: " + responseString
                        );

                    }

                    // If nothing wrong has happened return success and the payload
                    return new SendBuildMessage(
                            true,
                            "Successfully retrieved the download URL from Data Theorem: " + responseString
                    );
                } else {
                    return new SendBuildMessage(
                            false,
                            "Data Theorem upload_init call error: " + responseString
                    );
                }
            } else {
                return new SendBuildMessage(
                        false,
                        "Data Theorem upload_init call error: Empty body response "
                );
            }
        } catch (UnknownHostException e) {
            return new SendBuildMessage(
                    false,
                    "Data Theorem upload_init call error: UnknownHostException \n" +
                            "Please contact Data Theorem support: " + e.getMessage()
            );
        } catch (IOException e) {
            return new SendBuildMessage(
                    false,
                    "Data Theorem upload_init call error: IOException " + e.getMessage()
            );
        }
    }

    private HttpClient createAuthenticatedHttpClient() {
        /*
         * Create an http client to perform post request with or without a proxy
         * @return:
         *   The HttpClient of the endpoint
         */

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.useSystemProperties();

        if (proxyUnsecureConnection)
            try {
                this.logger.println("Insecure connection option is check: bypassing SSL Validation");
                SSLConnectionSocketFactory acceptAllCertificate = new SSLConnectionSocketFactory(
                        SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                        NoopHostnameVerifier.INSTANCE);

                clientBuilder.setSSLSocketFactory(acceptAllCertificate);
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                logger.println(e.getMessage());
            }

        if (proxyHostname == null || proxyHostname.isEmpty())
            return clientBuilder.build();

        clientBuilder.setProxy(new HttpHost(proxyHostname, proxyPort));

        if (proxyUsername == null || proxyUsername.isEmpty()) {
            this.logger.println("Proxy has no username/password authentification");
            return clientBuilder.build();
        }

        this.logger.println("Proxy is set using username/password authentification");

        // Add the User/Password proxy authentication
        NTCredentials ntCreds = new NTCredentials(proxyUsername, proxyPassword, "", "");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyHostname, proxyPort), ntCreds);
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
        clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

        return clientBuilder.build();

    }

    HttpResponse uploadInitRequest() throws IOException {
        /*
         * Http call to upload_init endpoint of the Upload API
         * @return:
         *   The HTTPResponse of the endpoint
         */
        HttpClient client = createAuthenticatedHttpClient();

        String upload_init_url = "https://api.securetheorem.com/uploadapi/v1/upload_init";
        HttpPost requestUploadInit = new HttpPost(upload_init_url);

        // Add the api access key of the customer and tell to Upload API that the request comes from jenkins

        requestUploadInit.addHeader("Authorization", "APIKEY " + apiKey);
        requestUploadInit.addHeader("User-Agent", "Jenkins Upload API Plugin " + version);

        HttpResponse response = client.execute(requestUploadInit);
        logger.println(response.getStatusLine().toString());
        return response;
    }

    SendBuildMessage uploadBuild(String buildPath, String sourceMapPath, Boolean isBuildStoredInArtifactFolder) {
        /*
         * Send the build to Data Theorem using the current valid upload link
         * @param:
         *   buildPath : Path of the build we want to send to Data Theorem
         * @return:
         *   SendBuildMessage containing the success or the failure information of the SendBuild process
         */

        logger.println("Uploading build to Data Theorem...");

        HttpResponse response;
        try {
            response = uploadBuildRequest(buildPath, sourceMapPath, isBuildStoredInArtifactFolder);

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return new SendBuildMessage(
                        false,
                        "Data Theorem upload build returned an empty body error"
                );
            }
            String responseString = EntityUtils.toString(entity, "UTF-8");
            if (response.getStatusLine().getStatusCode() == 200) {
                return new SendBuildMessage(
                        true,
                        "Successfully uploaded build to Data Theorem : " + responseString
                );
            }

            return new SendBuildMessage(
                    false,
                    "Data Theorem upload build returned an error: " + responseString
            );


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return new SendBuildMessage(
                    false,
                    "Data Theorem upload build returned an error: IOException: " + e.getMessage()
            );

        }
    }

    HttpResponse uploadBuildRequest(String buildPath, String sourceMapPath, Boolean isBuildStoredInArtifactFolder)
            throws IOException, InterruptedException {
        /*
         * Http call of the upload link generated by upload_init
         * @return:
         *   The HTTPResponse of the endpoint
         */


        // Create an http client to make the post request to upload_init endpoint

        HttpClient client = createAuthenticatedHttpClient();
        HttpPost requestUploadbuild = new HttpPost(this.uploadUrl);
        requestUploadbuild.addHeader("User-Agent", "Jenkins Upload API Plugin " + version);

        MultipartEntityBuilder entity_builder = MultipartEntityBuilder.create();
        entity_builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entity_builder.setContentType(ContentType.create(
                "multipart/form-data",
                new BasicNameValuePair("boundary", "\"jenkinsautouploadboundary\""))
        );
        if (sourceMapPath != null) {
            this.logger.println("Mapping file path is: " + sourceMapPath);
            AddContentToEntity(entity_builder, sourceMapPath, "sourcemap", ContentType.DEFAULT_TEXT);
        }
        this.logger.println("Build file path is: " + buildPath);

        if (isBuildStoredInArtifactFolder) {
            // if the build is in the permanent artifact directory we can upload it directly
            entity_builder.addBinaryBody("file", new File(buildPath));
            requestUploadbuild.setEntity(entity_builder.build());

            // Add the api access key of the customer and tell to Upload API that the request comes from jenkins
            return client.execute(requestUploadbuild);
        }


        AddContentToEntity(entity_builder, buildPath, "file", ContentType.DEFAULT_BINARY);
        requestUploadbuild.setEntity(entity_builder.build());
        this.logger.println("Start uploading build to the endpoint: " + this.uploadUrl);
        // Add the api access key of the customer and tell to Upload API that the request comes from jenkins
        return client.execute(requestUploadbuild);
    }

    private void AddContentToEntity(MultipartEntityBuilder entityBuilder, String binaryPath, String bodyName, ContentType contentType)
            throws IOException, InterruptedException {

        if (!workspace.child(binaryPath).isRemote()) {
            entityBuilder.addBinaryBody(bodyName, new File(workspace.child(binaryPath).toURI()));
            return;
        }
        entityBuilder.addPart(bodyName, new RemoteAgentStreamBody(
                workspace.child(binaryPath),
                contentType,
                workspace.child(binaryPath).getName()
        ));
    }
}

