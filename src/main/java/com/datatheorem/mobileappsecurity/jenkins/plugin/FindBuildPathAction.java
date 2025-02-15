package com.datatheorem.mobileappsecurity.jenkins.plugin;

import com.datatheorem.mobileappsecurity.jenkins.plugin.utils.ListFiles;
import groovy.lang.Tuple2;
import hudson.FilePath;
import hudson.model.Run;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;


/**
 * Find the absolute path of a build name.
 * <p>
 * This action first looks if the build is on the artifact directory.
 * Then the action searches for the build on the current job workspace directory.
 * If he doesn't find any builds the action return null
 * </p>
 */
class FindBuildPathAction {

    private final String buildName;
    private final FilePath workspace;
    private final Run<?, ?> runner;
    private final PrintStream logger;

    FindBuildPathAction(String buildName, FilePath workspace, Run<?, ?> runner, PrintStream logger)
    {

        this.buildName = buildName;
        this.workspace = workspace;
        this.runner = runner;
        this.logger = logger;
    }


    private boolean isSimilarToBuildName(String fileName) {
        /*
         * Test if the filename respects the buildName pattern
         */
        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + buildName);
        return matcher.matches(Paths.get(fileName));
    }

    @SuppressWarnings("deprecation")
    public Tuple2<String, Boolean> perform() {
        /*
         *  Find the absolute path of a build name
         *  @return: absolute path to the build if exist, null otherwise
         *  @return: boolean "isBuildStoredInArtifactFolder" when the build is stored in the artifact directory
         */

        for (Run<?, ?>.Artifact artifact : runner.getArtifacts()) {
            if (isSimilarToBuildName(artifact.getFileName())) {
                return new Tuple2<>(
                        runner.getArtifactsDir().toString() + '/' + artifact.relativePath, true
                );
            }
        }

        try {
            Collection<String> files = workspace.act(new ListFiles()).values();
            for (String file : files) {
                if (isSimilarToBuildName(file)) {
                        return new Tuple2<>(file, false);
                }
            }
        } catch (IOException |
                InterruptedException e) {
            logger.println(e.toString());
        }
        return null;
    }

}
