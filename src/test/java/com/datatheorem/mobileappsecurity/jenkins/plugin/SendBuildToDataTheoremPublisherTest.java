package com.datatheorem.mobileappsecurity.jenkins.plugin;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.sun.javafx.util.Utils.isUnix;

/**
 * Functional tests of the sendBuildToDataTheorem Publisher plugin.
 * <p>
 * This class checks if the plugin can be added as a publisher action to a jenkins job
 * Then it tests that the plugin can correctly find the file generated by a common build action
 * </p>
 */
public class SendBuildToDataTheoremPublisherTest {

    private final String buildName = "t*est-*.apk";
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testAddPluginToJob() throws Exception {
        /*
         * Check if the plugin can be added has a publisher action to a jenkins job
         */
        FreeStyleProject job = jenkins.createFreeStyleProject("test");
        SendBuildToDataTheoremPublisher sendBuilder = new SendBuildToDataTheoremPublisher(
                buildName);
        job.getPublishersList().add(sendBuilder);
        job = jenkins.configRoundtrip(job);

    }


    @Test
    public void testCreateAndFindBuild() throws Exception {
        /*
         * Check if the plugin can find a file generated by a common build action
         * The build action is a simple shell that create a test-1.12.45.apk
         */
        if (isUnix()) {
            FreeStyleProject job = jenkins.createFreeStyleProject();
            job.getBuildersList().add(
                    new hudson.tasks.Shell("#!/bin/bash\n touch test-1.12.45.apk"));

            SendBuildToDataTheoremPublisher sendBuilder = new SendBuildToDataTheoremPublisher(
                    buildName);
            job.getPublishersList().add(sendBuilder);

            FreeStyleBuild completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));

            String expectedString = "Found the build at path:";
            jenkins.assertLogContains(expectedString, completedBuild);
        }
    }


    @Test
    public void testCreateAndFindBuildAndSourceMap() throws Exception {
        /*
         * Check if the plugin can find a file generated by a common build action
         * The build action is a simple shell that create a test-1.12.45.apk
         */
        if (isUnix()) {
            FreeStyleProject job = jenkins.createFreeStyleProject();
            job.getBuildersList().add(
                    new hudson.tasks.Shell("#!/bin/bash\n touch test-1.12.45.apk"));
            job.getBuildersList().add(
                    new hudson.tasks.Shell("#!/bin/bash\n touch test-mapping.txt"));

            SendBuildToDataTheoremPublisher sendBuilder = new SendBuildToDataTheoremPublisher(
                    buildName);
            job.getPublishersList().add(sendBuilder);

            FreeStyleBuild completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));

            String expectedString = "Found the build at path:";
            jenkins.assertLogContains(expectedString, completedBuild);
        }
    }

    @Test
    public void testWrongBuildName() throws Exception {
        /*
         * Check if the plugin can set the result to unstable if no build have been found
         */
        if (isUnix()) {
            FreeStyleProject job = jenkins.createFreeStyleProject();

            job.getBuildersList().add(
                    new hudson.tasks.Shell("#!/bin/bash\n " +
                            "touch test.apk"));

            SendBuildToDataTheoremPublisher sendBuilder = new SendBuildToDataTheoremPublisher(
                    buildName);
            job.getPublishersList().add(sendBuilder);

            FreeStyleBuild completedBuild = jenkins.assertBuildStatus(Result.UNSTABLE, job.scheduleBuild2(0));

            String expectedString = "Unable to find any build with name";
            jenkins.assertLogContains(expectedString, completedBuild);
        }
    }

}