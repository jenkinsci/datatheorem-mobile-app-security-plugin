package com.datatheorem.mobileappsecurity.jenkins.plugin.utils;


import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ListFiles extends MasterToSlaveFileCallable<Map<String, String>> {
    /*
     * Jenkins core way to get the relative path of all files from workspace directory
     * of either jenkins local server or external secondary agent
     */

    @Override
    public Map<String, String> invoke(File basedir, VirtualChannel channel) {
        Map<String, String> r = new HashMap<>();

        FileSet fileSet = Util.createFileSet(basedir, "", "");
        fileSet.setCaseSensitive(true);

        for (String f : fileSet.getDirectoryScanner().getIncludedFiles()) {
            f = f.replace(File.separatorChar, '/');
            r.put(f, f);
        }
        return r;
    }
}