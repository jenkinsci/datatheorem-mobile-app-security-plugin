package com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;

public class SendFromRemote extends MasterToSlaveFileCallable<SendBuildMessage> {
        // Modified version of FilePath.CopyTo
        String buildPath;
        String sourceMapPath;
        Boolean isBuildStoredInArtifactFolder;

    public SendFromRemote(
            String buildPath,
            String sourceMapPath,
            Boolean isBuildStoredInArtifactFolder
    ){
        this.buildPath = buildPath;
        this.sourceMapPath =sourceMapPath;
        this.isBuildStoredInArtifactFolder = isBuildStoredInArtifactFolder;
        }

    public SendBuildMessage invoke(File f, VirtualChannel channel) throws IOException {
        return new SendBuildMessage(true,"lol");
//
//            return perform(
//                    buildPath,
//                    sourceMapPath,
//                    isBuildStoredInArtifactFolder
//            );
        }

}
