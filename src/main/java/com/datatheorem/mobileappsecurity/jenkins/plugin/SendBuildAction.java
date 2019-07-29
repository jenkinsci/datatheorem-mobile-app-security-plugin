package com.datatheorem.mobileappsecurity.jenkins.plugin;


import hudson.FilePath;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.UnknownHostException;

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
    private String version = "1.1.0";

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
    }

    public SendBuildMessage perform(String buildPath, Boolean isArtifact) {
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
            return uploadBuild(buildPath, isArtifact);
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

    HttpResponse uploadInitRequest() throws IOException {
        /*
         * Http call to upload_init endpoint of the Upload API
         * @return:
         *   The HTTPResponse of the endpoint
         */

        HttpClient client = HttpClientBuilder.create().build();

        // Create an http client to make the post request to upload_init endpoint

        String upload_init_url = "https://api.securetheorem.com/uploadapi/v1/upload_init";
        HttpPost requestUploadInit = new HttpPost(upload_init_url);

        // Add the api access key of the customer and tell to Upload API that the request comes from jenkins

        requestUploadInit.addHeader("Authorization", "APIKEY " + apiKey);
        requestUploadInit.addHeader("User-Agent", "Jenkins Upload API Plugin " + version);

        HttpResponse response = client.execute(requestUploadInit);
        logger.println(response.getStatusLine().toString());
        return response;
    }

    SendBuildMessage uploadBuild(String buildPath, Boolean isArtifact) {
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
            response = uploadBuildRequest(buildPath, isArtifact);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity, "UTF-8");
                if (response.getStatusLine().getStatusCode() == 200) {
                    return new SendBuildMessage(
                            true,
                            "Successfully uploaded build to Data Theorem : " + responseString
                    );
                } else {
                    return new SendBuildMessage(
                            false,
                            "Data Theorem upload build returned an error: " + responseString
                    );
                }
            } else {
                return new SendBuildMessage(
                        false,
                        "Data Theorem upload build returned an empty body error"
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new SendBuildMessage(
                    false,
                    "Data Theorem upload build returned an error: IOException: " + e.getMessage()
            );

        } catch (InterruptedException e) {
            e.printStackTrace();
            return new SendBuildMessage(
                    false,
                    "Data Theorem upload build returned an error: IOException: " + e.getMessage()
            );
        }
    }

    HttpResponse uploadBuildRequest(String buildPath, Boolean isArtifact) throws IOException, InterruptedException {
        /*
         * Http call of the upload link generated by upload_init
         * @return:
         *   The HTTPResponse of the endpoint
         */


        // Create an http client to make the post request to upload_init endpoint

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost requestUploadbuild = new HttpPost(this.uploadUrl);

        if (isArtifact) {
            // if the build is in the permanent artifact directory we can upload it directly


            HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("file", new File(buildPath)).build();
            requestUploadbuild.setEntity(entity);

            // Add the api access key of the customer and tell to Upload API that the request comes from jenkins
            requestUploadbuild.addHeader("User-Agent", "Jenkins Upload API Plugin " + version);
            HttpResponse response = client.execute(requestUploadbuild);
            logger.println(response.getStatusLine().toString());
            return response;
        }

        // if the build is in the workspace we copy the content in an Output Stream and we upload the stream

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        workspace.child(buildPath).copyTo(os);
        logger.println(os.toByteArray().length);

        // send the file
        HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody(
                "file",
                os.toByteArray(),
                ContentType.DEFAULT_BINARY,
                workspace.child(buildPath).getName()
        ).build();
        requestUploadbuild.setEntity(entity);

        // Add the api access key of the customer and tell to Upload API that the request comes from jenkins
        requestUploadbuild.addHeader("User-Agent", "Jenkins Upload API Plugin " + version);
        HttpResponse response = client.execute(requestUploadbuild);
        os.flush();
        os.close();
        return response;
    }
}