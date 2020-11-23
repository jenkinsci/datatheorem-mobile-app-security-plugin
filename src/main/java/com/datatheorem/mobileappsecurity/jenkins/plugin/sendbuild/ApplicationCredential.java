package com.datatheorem.mobileappsecurity.jenkins.plugin.sendbuild;

import hudson.model.TaskListener;
import org.apache.http.entity.mime.MultipartEntityBuilder;

public class ApplicationCredential  implements java.io.Serializable{
    private TaskListener listener;
    private String username;
    private String password;
    private String comments = null;


    public ApplicationCredential(
            TaskListener listener,
            String username,
            String password
    ){
        this.listener = listener;

        if (username.isEmpty() || password.isEmpty()) {
            listener.getLogger().println(
                    "application credential is set but incomplete. " +
                            "Missing username or password"
            );
            throw new IllegalArgumentException();
        }

        this.username = username;
        this.password = password;
    }


    public void setComments(String comment) {
        this.comments = comment;
    }

    public void add_credential_to_entity(MultipartEntityBuilder entity_builder){
            listener.getLogger().println("adding credentials to entity...");
            entity_builder.addTextBody("username", username);
            entity_builder.addTextBody("password", password);
            if (comments != null) entity_builder.addTextBody("comments", comments);
    }
}
