package com.datatheorem.mobileappsecurity.jenkins.plugin;

import groovy.lang.Tuple2;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
/**
 * This class aims to provide a simple plugin to automatically upload builds to Data Theorem Upload API.
 * <p>
 * The plugin is a publish action that gets a buildName as a parameter and automatically sends the corresponding build
 * to Data Theorem.
 * It needs a secret DATA_THEOREM_UPLOAD_API_KEY variable generated using:
 * https://wiki.jenkins.io/display/JENKINS/Credentials+Plugin
 * </p>
 */
public class SendBuildToDataTheoremPublisher extends Publisher implements SimpleBuildStep {
    private final String buildToUpload;
    private final boolean dontUpload;
    private final String proxyHostname;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;

    @DataBoundConstructor
    public SendBuildToDataTheoremPublisher(
            String buildToUpload,
            boolean dontUpload,
            String proxyHostname,
            int proxyPort,
            String proxyUsername,
            String proxyPassword
    ) {
        /*
        Bind the parameter value of the job configuration page
        */
        this.buildToUpload = buildToUpload;
        this.dontUpload = dontUpload;
        this.proxyHostname = proxyHostname;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
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
        if (result != null && result.isWorseOrEqualTo(Result.FAILURE)) {
            listener.getLogger().println("Skipping upload because the build step failed");
            return;
        } else if (result != null && result.isWorseOrEqualTo(Result.UNSTABLE)) {
            listener.getLogger().println("Skipping upload because the build is unstable");
            return;
        }

        listener.getLogger().println("Uploading the build to Data Theorem : " + this.buildToUpload);

        // First find the path to the build to upload
        FindBuildPathAction buildToSend = new FindBuildPathAction(this.buildToUpload, workspace, run, listener.getLogger());
        Tuple2<String, Boolean> findPathResult = buildToSend.perform();
        if (findPathResult != null) {
            String buildPath = findPathResult.getFirst();
            Boolean isBuildStoredInArtifactFolder = findPathResult.getSecond();

            listener.getLogger().println("Found the build at path: " + buildPath);

            // If the user only wants to check if the path was correct we don't call the Upload API
            if (dontUpload) {
                listener.getLogger().println("Skipping upload... \"Don't Upload\" option enabled");
            } else {
                // Then upload the build to DT
                if (proxyHostname == null || proxyHostname.isEmpty()) {
                    listener.getLogger().println("No proxy configuration");

                    sendBuild = new SendBuildAction(
                            run.getEnvironment(listener).get("DATA_THEOREM_UPLOAD_API_KEY"),
                            listener.getLogger(),
                            workspace
                    );
                }
                else {
                        listener.getLogger().println("Proxy Configuration is : " + proxyHostname + ":" + proxyPort);

                        sendBuild = new SendBuildAction(
                            run.getEnvironment(listener).get("DATA_THEOREM_UPLOAD_API_KEY"),
                            listener.getLogger(),
                            workspace,
                            proxyHostname,
                            proxyPort,
                            proxyUsername,
                            proxyPassword
                        );
                    }

                SendBuildMessage sendBuildResult = sendBuild.perform(buildPath, isBuildStoredInArtifactFolder);
                if (!sendBuildResult.message.equals("")) {
                    listener.getLogger().println(sendBuildResult.message);
                }
                if (!sendBuildResult.success) {
                    run.setResult(Result.UNSTABLE);
                }
            }
        } else {
            listener.getLogger().println("Unable to find any build with name : " + this.buildToUpload);
            run.setResult(Result.UNSTABLE);
        }
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
        return buildToUpload;
    }

    public boolean isDontUpload() {

        // Required to get the last value when we update a job config
        return dontUpload;
    }

    public String getProxyHostname() {
        // Required to get the last value when we update a job config

        return proxyHostname;
    }

    public int getProxyPort() {
        // Required to get the last value when we update a job config

        return proxyPort;
    }

    public String getProxyUsername() {
        // Required to get the last value when we update a job config

        return proxyUsername;
    }

    public String getProxyPassword() {
        // Required to get the last value when we update a job config

        return proxyPassword;
    }

    @Symbol("Data Theorem")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /*
         * Validate that the build name parameter in job configuration page is not left empty
         */

        public FormValidation doCheckBuildToUpload(@QueryParameter("buildToUpload") String value) {
            if (value.length() == 0)
                return FormValidation.error("The build name is empty");
            if (!value.toLowerCase().endsWith(".apk") && !value.toLowerCase().endsWith(".ipa"))
                return FormValidation.error("the build name should ends with .apk or .ipa");
            if (value.length() < 5)
                return FormValidation.error("The build name is too short");
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
