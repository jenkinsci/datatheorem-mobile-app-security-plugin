package com.datatheorem.mobileappsecurity.jenkins.plugin;

import hudson.FilePath;
import hudson.model.Run;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

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

    FindBuildPathAction(String buildName, FilePath workspace, Run<?, ?> runner) {

        this.buildName = buildName;
        this.workspace = workspace;
        this.runner = runner;
    }

    private String findBuildFile(File[] contents) {
        /*
         * Iterate through the content of a directory and try to find the build that should be uploaded
         */
        for (File content : contents) {

            if (content.isDirectory()) {
                File[] ListFile = content.listFiles();
                if (ListFile != null) {
                    String buildToUploadPath = findBuildFile(ListFile);
                    if (buildToUploadPath != null) {
                        return buildToUploadPath;
                    }
                }
            } else if (content.isFile()) {
                if (isSimilarToBuildName(content.getName())) {
                    return content.getAbsolutePath();
                }
            }
        }
        return null;
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
    public String perform() {
        /*
         *  Find the absolute path of a build name
         *  @return: absolute path to the build if exist, null otherwise
         */
        for (Run<?, ?>.Artifact artifact : runner.getArtifacts()) {
            if (isSimilarToBuildName(artifact.getFileName())) {
                return runner.getArtifactsDir().toString() + '/' + artifact.relativePath;
            }
        }

        File workspaceRootItem = new File(workspace.getRemote());
        if (workspaceRootItem.exists() && workspaceRootItem.isDirectory()) {
            File[] ListFile = workspaceRootItem.listFiles();
            if (ListFile != null)
                return findBuildFile(ListFile);
        }
        return null;
    }
}
