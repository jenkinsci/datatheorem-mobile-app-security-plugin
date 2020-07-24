package com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild;

/**
 * Response message of SendBuildAction
 */
public class SendBuildMessage {
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
