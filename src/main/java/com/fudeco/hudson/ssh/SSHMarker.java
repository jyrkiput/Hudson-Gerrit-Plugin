/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fudeco.hudson.ssh;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
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

    public SSHMarker() {
    }

    public SshClient connect(String host, int port) throws IOException {

        SshClient client = new SshClient();
        client.connect(host, port, new IgnoreHostKeyVerification());

        return client;
    }

    public void authenticate(SshClient client, String username, File private_key_file, String passPhrase)
            throws IOException {

        PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();

        pk.setUsername(username);

        SshPrivateKeyFile file = SshPrivateKeyFile.parse(private_key_file);
        //file.setFormat(new SshtoolsPrivateKeyFormat(), passPhrase);
        SshPrivateKey key = file.toPrivateKey(passPhrase);
        pk.setKey(key);
        int result = client.authenticate(pk);
    }

    public String executeCommand(SshClient client, String command) throws IOException {
        SessionChannelClient session = client.openSessionChannel();
        session.executeCommand(command);
        InputStream in = session.getInputStream();
        byte buffer[] = new byte[255];
        int read;
        StringBuffer sb = new StringBuffer();
        while ((read = in.read(buffer)) > 0) {
            String out = new String(buffer, 0, read);
            System.out.println(out);
            sb.append(out);
        }
        session.close();
        return sb.toString();
    }
}
