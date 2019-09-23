package com.datatheorem.mobileappsecurity.jenkins.plugin.utils;

import hudson.FilePath;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.util.Args;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

public class RemoteAgentStreamBody extends AbstractContentBody {
    private FilePath filePath;
    private final String filename;


    public RemoteAgentStreamBody(FilePath filePath, ContentType contentType, String filename) {
        super(contentType);
        Args.notNull(filePath, "Input stream");
        this.filePath = filePath;
        this.filename = filename;
    }


    public void writeTo(OutputStream out) throws IOException {
        try {
            filePath.act(new RemoteAgentStreamBody.CopyToCallable(out));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public String getTransferEncoding() {
        return "binary";
    }

    public long getContentLength() {
        try {
            return filePath.length();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    public String getFilename() {
        return this.filename;
    }

    public FilePath getFilePath() {
        return this.filePath;
    }

    private static class CopyToCallable extends MasterToSlaveFileCallable<Map<String, String>> {
        private final RemoteOutputStream out;

        CopyToCallable(OutputStream out) {
            this.out = new RemoteOutputStream(out);
        }

        public Map<String, String> invoke(File f, VirtualChannel channel) throws IOException {

            InputStream fis = Files.newInputStream(f.toPath());
            org.apache.commons.io.IOUtils.copyLarge(fis, this.out);
            return null;
        }
    }
}
