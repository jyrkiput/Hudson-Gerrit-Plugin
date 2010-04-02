package com.fudeco.hudson.plugins.gerrit;

import com.fudeco.hudson.plugins.gerrit.git.GitTools;
import com.fudeco.hudson.plugins.gerrit.ssh.SSHMarker;
import hudson.FilePath;
import hudson.model.Build;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import junit.framework.TestCase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.UnstableBuilder;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


public class GerritNotifierTests extends HudsonTestCase {


    SSHMarker marker;
    GitTools git;

    String userName = "user";
    String hostName = "localhost";
    String gitDir = ".git";
    final String privateKeyFilePath = "private_key_path";
    String passPhrase = "";
    String hexString = "1234567890123456789012345678901234567890";
    ObjectId id = ObjectId.fromString(hexString);
    GerritNotifier notifier;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        marker = mock(SSHMarker.class);
        git = mock(GitTools.class);
        notifier = new GerritNotifier(gitDir, hostName, 29418, userName, "+1", "-1", "-1",
            privateKeyFilePath, passPhrase);
        notifier.setGitTools(git);
        notifier.setMarker(marker);
        when(git.getHead(Matchers.<File>any(), eq(".git"))).thenReturn(id);
        
    }

    private Build doBuild(Builder builder) throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject project = createFreeStyleProject();
        if(builder != null) {
            project.getBuildersList().add(builder);
        }

        project.getPublishersList().add(notifier);

        final FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build;
    }

    @Test
    public void testSettings() throws IOException, ExecutionException, InterruptedException {

        final Build build = doBuild(null);
        FilePath ws = build.getWorkspace();
        ws.act(new FilePath.FileCallable<Void>() {
            @Override
            public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                verify(git).getHead(f, ".git");
                return null;
            };
        });
        verify(marker).connect(hostName, 29418);
        verify(marker).authenticate(userName, new File(privateKeyFilePath), passPhrase);
    }



    @Test
    public void testUnstableBuild() throws IOException, ExecutionException, InterruptedException {

        final Build build = doBuild(new UnstableBuilder());
        assertEquals(Result.UNSTABLE, build.getResult());
        String command = notifier.generateUnstableCommand("No build url.", hexString);
        verify(marker).executeCommand(command);
    }

    @Test
    public void testStableBuild() throws IOException, ExecutionException, InterruptedException {

        final Build build = doBuild(null);
        assertEquals(Result.SUCCESS, build.getResult());
        String command = notifier.generateApproveCommand("No build url.", hexString);
        verify(marker).executeCommand(command);
    }

    @Test
    public void testFailedBuild() throws IOException, ExecutionException, InterruptedException {

        final Build build = doBuild(new FailureBuilder());
        assertEquals(Result.FAILURE, build.getResult());
        String command = notifier.generateFailedCommand("No build url.", hexString);
        verify(marker).executeCommand(command);
    }
}

