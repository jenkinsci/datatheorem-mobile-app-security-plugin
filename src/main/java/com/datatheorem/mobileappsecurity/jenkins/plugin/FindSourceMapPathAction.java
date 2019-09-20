package com.datatheorem.mobileappsecurity.jenkins.plugin;

import groovy.lang.Tuple2;
import hudson.FilePath;
import hudson.model.Run;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;

public class FindSourceMapPathAction {
    final String sourceMapName;
    private final FilePath workspace;
    private final Run<?, ?> runner;
    private final PrintStream logger;

    public FindSourceMapPathAction(String sourceMapName, FilePath workspace, Run<?, ?> runner, PrintStream logger) {
        this.sourceMapName = sourceMapName;
        this.workspace = workspace;
        this.runner = runner;
        this.logger = logger;
    }

    private boolean isSimilarToSourceMapName(String fileName) {
        /*
         * Test if the filename respects the buildName pattern
         */
        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + sourceMapName);
        return matcher.matches(Paths.get(fileName));
    }

    public String perform(){
        try {
            Collection<String> files = workspace.act(new ListFiles()).values();
            for (String file : files) {
                if (isSimilarToSourceMapName(file)) {
                    return file;
                }
            }
        } catch (IOException |
                InterruptedException e) {
            logger.println(e.toString());
        }
        return null;
    }
}
