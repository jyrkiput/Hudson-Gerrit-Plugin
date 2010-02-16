/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fudeco.hudson.ssh;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.sshtools.j2ssh.transport.TransportProtocolState;
import com.sshtools.j2ssh.transport.publickey.InvalidSshKeyException;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;
import java.io.File;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Jyrki
 */
public class SSHMarker {

    private SshClient client = null;
    public SSHMarker() {
    }

    public void connect(String host, int port) throws IOException {

        SshClient client = new SshClient();
        client.connect(host, port, new IgnoreHostKeyVerification());

        this.client = client;
    }

    public void authenticate(String username, File private_key_file, String passPhrase)
            throws IOException {
        assert client != null;

        PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();

        pk.setUsername(username);

        SshPrivateKeyFile file = SshPrivateKeyFile.parse(private_key_file);
        //file.setFormat(new SshtoolsPrivateKeyFormat(), passPhrase);
        SshPrivateKey key = file.toPrivateKey(passPhrase);
        pk.setKey(key);
        int result = client.authenticate(pk);
    }
    public static boolean IsPrivateKeyFileValid(File f) {

        SshPrivateKeyFile file = null;
        try {
            file = SshPrivateKeyFile.parse(f);
        } catch (IOException e) {
            return false;
        }
        if (file == null) {
            return false;
        }
        return true;
    }
    public static boolean CheckPassPhrase(File f, String p) {
        SshPrivateKeyFile file = null;
        try {
            file = SshPrivateKeyFile.parse(f);
        } catch (IOException e) {
            return false;
        }

        try {
            SshPrivateKey key = file.toPrivateKey(p);
        } catch (InvalidSshKeyException e) {
            return false;
        }
        return true;
    }
    public void executeCommand(String command) throws IOException {
        assert client != null;
        SessionChannelClient session = client.openSessionChannel();
        
        session.executeCommand(command);
        session.close();
    }

    public void disconnect() {
        assert client != null;
        client.disconnect();
    }
}
