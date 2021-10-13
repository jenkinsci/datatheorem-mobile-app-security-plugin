package com.datatheorem.mobileappsecurity.jenkins.plugin;

import com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild.ApplicationCredential;
import com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild.Proxy;
import com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild.SendBuildAction;
import com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild.SendBuildMessage;
import groovy.lang.Tuple2;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This class aims to provide a simple plugin to automatically upload builds to Data Theorem Upload API.
 * <p>
 * The plugin is a publish action that gets a buildName as a parameter and automatically sends the corresponding build
 * to Data Theorem.
 * It needs a secret DATA_THEOREM_UPLOAD_API_KEY variable generated using:
 * https://wiki.jenkins.io/display/JENKINS/Credentials+Plugin
 * </p>
 */

public class SendBuildToDataTheoremPublisher extends Publisher implements SimpleBuildStep, Serializable {
    private  String buildToUpload;
    private  String mappingFileToUpload = null;
    private  boolean dontUpload = false;
    private  String proxyHostname = null;
    private  int proxyPort = 0;
    private  String proxyUsername = null;
    private Secret proxyPassword = null;
    private  boolean proxyUnsecuredConnection = false;
    private String dataTheoremUploadApiKey = null;
    private  boolean sendBuildDirectlyFromRemote = false;
    private  String applicationCredentialUsername = null;
    private Secret applicationCredentialPassword = null;
    private  String applicationCredentialComments = null;
    private  String releaseType = null;
    private  String externalId = null;

    @DataBoundConstructor
    public SendBuildToDataTheoremPublisher(
            String buildToUpload
    ) {
        /*
        * Bind the parameter value of the job configuration page
        */
        this.buildToUpload = buildToUpload;
    }

    public String getDataTheoremUploadApiKey() {
        return dataTheoremUploadApiKey;
    }


    private String getSecretKey(Run<?,?> run, TaskListener listener) throws IOException, InterruptedException {
    /*
    * Environment variable is handled differently as a pipeline step or as a post-build action
    * Documentation: https://jenkins.io/doc/developer/plugin-development/pipeline-integration/ section "Variable substitutions"
    */
        if (run instanceof AbstractBuild) {
            //As a post-build action we have access to any defined environment variable value
            listener.getLogger().println("Data Theorem Upload Build plugin is running as a post-build action");
            return run.getEnvironment(listener).get("DATA_THEOREM_UPLOAD_API_KEY");
        } else {
            //As a pipeline step, the plugin should take any configuration values as literal strings
            listener.getLogger().println(
                    "Data Theorem Upload Build plugin is called from a jenkins pipeline script"
            );
            if (this.dataTheoremUploadApiKey == null)
                listener.getLogger().println(
                        "You should set dataTheoremUploadApiKey " +
                        "with DATA_THEOREM_UPLOAD_API_KEY environment variable value "
                );
            return this.dataTheoremUploadApiKey;
        }
    }

    @Override
    public void perform(
            Run<?, ?> run,
            @Nonnull FilePath workspace,
            @Nonnull Launcher launcher,
            TaskListener listener
    ) throws InterruptedException, IOException {

        SendBuildAction sendBuild;
        listener.getLogger().println("Data Theorem upload build plugin starting...");

        Result result = run.getResult();
        if (result != null && result.isWorseOrEqualTo(Result.UNSTABLE)) {
            listener.getLogger().println(
                    "Skipping Data Theorem CI/CD because the previous step result is: " + Result.UNSTABLE.toString()
            );
            run.setResult(result);
            return;
        }

        listener.getLogger().println("Uploading the build to Data Theorem : " + this.buildToUpload);
        // First find the path to the build to upload
        FindBuildPathAction buildToSend = new FindBuildPathAction(this.buildToUpload, workspace, run, listener.getLogger());
        Tuple2<String, Boolean> findPathResult = buildToSend.perform();
        if (findPathResult == null) {
            listener.getLogger().println("Unable to find any build with name : " + this.buildToUpload);
            run.setResult(Result.UNSTABLE);
            return;
        }

        // Check if the build is in artifact folder or the workspace
        String buildPath = findPathResult.getFirst();
        Boolean isBuildStoredInArtifactFolder = findPathResult.getSecond();

        String findSourceMapResult = null;
        if (!(mappingFileToUpload == null || mappingFileToUpload.isEmpty())){
            FindSourceMapPathAction findSourceMapPathAction = new FindSourceMapPathAction(this.mappingFileToUpload, workspace, listener.getLogger());
             findSourceMapResult = findSourceMapPathAction.perform();
            if (findSourceMapResult == null) {
                listener.getLogger().println("Unable to find any mapping file with name : " + this.mappingFileToUpload);
                run.setResult(Result.UNSTABLE);
                return;
            }
            listener.getLogger().println("Found the mapping file at path: " + findSourceMapResult);
        }

        // If the user only wants to check if the path was correct we don't call the Upload API
        if (dontUpload) {
            listener.getLogger().println("Skipping upload... \"Don't Upload\" option enabled");
            run.setResult(Result.SUCCESS);
            return;
        }

        // Configure proxy and credentials

        sendBuild = new SendBuildAction(
                    getSecretKey(run, listener),
                    listener,
                    workspace,
                    buildPath,
                    findSourceMapResult,
                    isBuildStoredInArtifactFolder
            );

        if (proxyHostname == null || proxyHostname.isEmpty()) {
            listener.getLogger().println("No proxy configuration");
        }
        else
        {
            listener.getLogger().println("Proxy Configuration is : " + proxyHostname + ":" + proxyPort);
            try {
                Proxy proxy = new Proxy(
                        listener,
                        proxyHostname,
                        proxyPort,
                        proxyUsername,
                        proxyPassword.getPlainText(),
                        proxyUnsecuredConnection
                );
                sendBuild.setProxy(proxy);
            } catch (IllegalArgumentException e){
                run.setResult(Result.UNSTABLE);
                return;
            }

        }

        if (releaseType != null && !releaseType.isEmpty()){
            if (!Arrays.asList("ENTERPRISE", "PRE_PROD").contains(releaseType)){
                listener.getLogger().println("Only PRE_PROD and ENTERPRISE release type are allowed");
                run.setResult(Result.UNSTABLE);
                return;

            }
            sendBuild.setReleaseType(releaseType);
        }

        if (externalId != null) {
            if (externalId.isEmpty()) {
                listener.getLogger().println("External ID cannot be set to an empty string");
                run.setResult(Result.UNSTABLE);
                return;
            }
            sendBuild.setExternalId(externalId);
        }

        if (applicationCredentialUsername != null && !applicationCredentialUsername.isEmpty()) {
            // Set application credentials
            try{
                ApplicationCredential applicationCredential = new ApplicationCredential(
                        listener,
                        applicationCredentialUsername,
                        applicationCredentialPassword.getPlainText()
                );

                if (applicationCredentialComments != null && !applicationCredentialComments.isEmpty()) {
                    applicationCredential.setComments(applicationCredentialComments);
                }

                sendBuild.setApplicationCredential(applicationCredential);
            } catch (IllegalArgumentException e) {
                run.setResult(Result.UNSTABLE);
                return;
            }
        }

        // Then upload the build to DT

        SendBuildMessage sendBuildResult;
        if (sendBuildDirectlyFromRemote){
            sendBuildResult = workspace.act(sendBuild);
        }
        else{
            sendBuildResult = sendBuild.perform();
        }

        if (!sendBuildResult.message.isEmpty()) {
            listener.getLogger().println(sendBuildResult.message);
        }
        if (!sendBuildResult.success) {
            run.setResult(Result.UNSTABLE);
            return;
        }
       run.setResult(Result.SUCCESS);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getBuildToUpload() {

        // Required to get the last value when we update a job config
        return this.buildToUpload;
    }

    public String getmappingFileToUpload() {

        // Required to get the last value when we update a job config
        return this.mappingFileToUpload;
    }

    public boolean isDontUpload() {

        // Required to get the last value when we update a job config
        return this.dontUpload;
    }

    public String getProxyHostname() {
        // Required to get the last value when we update a job config

        return this.proxyHostname;
    }

    public int getProxyPort() {
        // Required to get the last value when we update a job config

        return this.proxyPort;
    }


    public String getProxyUsername() {
        // Required to get the last value when we update a job config

        return this.proxyUsername;
    }

    public Secret getProxyPassword() {
        // Returns the encrypted value of the field

        return this.proxyPassword;
    }

    public boolean getProxyUnsecuredConnection() {
        // Required to get the last value when we update a job config

        return this.proxyUnsecuredConnection;
    }

    public boolean getSendBuildDirectlyFromRemote() {
        // Required to get the last value when we update a job config

        return this.sendBuildDirectlyFromRemote;
    }

    public String getApplicationCredentialUsername() {
        return this.applicationCredentialUsername;
    }

    public Secret getApplicationCredentialPassword() {
        return this.applicationCredentialPassword;
    }

    public String getApplicationCredentialComments() {
        return applicationCredentialComments;
    }


    public String getReleaseType() {
        return releaseType;
    }

    public String getExternalId() {
        return externalId;
    }

    @DataBoundSetter
    public void setDataTheoremUploadApiKey(String dataTheoremUploadApiKey) {
        /*
         * dataTheoremUploadApiKey can be set in a jenkins pipeline by adding dataTheoremUploadApiKey parameter at the call of the plugin
         */
        this.dataTheoremUploadApiKey = dataTheoremUploadApiKey;
    }

    @DataBoundSetter
    public void setBuildToUpload(String buildToUpload) {
        this.buildToUpload = buildToUpload;
    }

    @DataBoundSetter
    public void setMappingFileToUpload(String mappingFileToUpload) {
        this.mappingFileToUpload = mappingFileToUpload;
    }

    @DataBoundSetter
    public void setProxyPassword(String proxyPassword) {

        this.proxyPassword = Secret.fromString(proxyPassword);
    }

    @DataBoundSetter
    public void setApplicationCredentialPassword(String applicationCredentialPassword) {
        this.applicationCredentialPassword = Secret.fromString(applicationCredentialPassword);
    }

    @DataBoundSetter
    public void setDontUpload(boolean dontUpload) {
        this.dontUpload = dontUpload;
    }

    @DataBoundSetter
    public void setProxyHostname(String proxyHostname) {
        this.proxyHostname = proxyHostname;
    }

    @DataBoundSetter
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @DataBoundSetter
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    @DataBoundSetter
    public void setProxyUnsecuredConnection(boolean proxyUnsecuredConnection) {
        this.proxyUnsecuredConnection = proxyUnsecuredConnection;
    }

    @DataBoundSetter
    public void setSendBuildDirectlyFromRemote(boolean sendBuildDirectlyFromRemote) {
        this.sendBuildDirectlyFromRemote = sendBuildDirectlyFromRemote;
    }

    @DataBoundSetter
    public void setApplicationCredentialUsername(String applicationCredentialUsername) {
        this.applicationCredentialUsername = applicationCredentialUsername;
    }

    @DataBoundSetter
    public void setApplicationCredentialComments(String applicationCredentialComments) {
        this.applicationCredentialComments = applicationCredentialComments;
    }

    @DataBoundSetter
    public void setReleaseType(String releaseType) {
        this.releaseType = releaseType;
    }

    @DataBoundSetter
    public void setExternalId(String externalId) {
        this.externalId = externalId
    }

    @Extension
    // Define the symbols needed to call the jenkins plugin in a DSL pipeline
    @Symbol({
        "sendBuildToDataTheorem",
        "buildToUpload",
        "mappingFileToUpload",
        "dontUpload",
        "proxyHostname",
        "proxyPort",
        "proxyUsername",
        "proxyPassword",
        "proxyUnsecuredConnection",
        "dataTheoremUploadApiKey",
        "sendBuildDirectlyFromRemote",
        "applicationCredentialUsername",
        "applicationCredentialPassword",
        "applicationCredentialComments",
        "releaseType",
        "externalId",
    })
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /*
         * Validate that the build name parameter in job configuration page is not left empty
         */

        public FormValidation doCheckBuildToUpload(@QueryParameter(value = "buildToUpload") String value) {
            if (value.length() == 0)
                return FormValidation.error("The build name is empty");
            if (!value.toLowerCase().endsWith(".apk") && !value.toLowerCase().endsWith(".ipa"))
                return FormValidation.error("the build name should ends with .apk or .ipa");
            if (value.length() < 5)
                return FormValidation.error("The build name is too short");
            return FormValidation.ok();
        }

        public FormValidation doCheckmappingFileToUpload(
                @QueryParameter(value = "mappingFileToUpload") String sourceMapName,
                @QueryParameter(value = "buildToUpload") String buildName
                )
        {
            if (sourceMapName.isEmpty())
                return FormValidation.ok();
            if (!sourceMapName.toLowerCase().endsWith(".txt"))
                return FormValidation.warning("mapping file should ends with .txt");
            if (!buildName.toLowerCase().endsWith(".apk"))
                return FormValidation.warning("mapping file should only be uploaded with an apk file");

            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Upload build to Data Theorem";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }


}
