package hudson.plugins.gerrit.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;

public class GitTools {

    public File getGitHome(File workspace, String gitHome) {
        String git_path = workspace.getAbsolutePath() + File.separatorChar + gitHome;
        File git_home_directory = new File(git_path);
        if (!git_home_directory.isDirectory()) {
            return null;
        }
        return git_home_directory;
    }

    public Repository getRepository(File git_home) {

        Repository repo;
        try {
            repo = new Repository(git_home);
        } catch (IOException e) {
            return null;
        }
        return repo;
    }

    public ObjectId getHead(Repository repo) {
        ObjectId head;
        try {
            head = repo.resolve("HEAD");
        } catch (IOException e) {
            return null;
        }

        return head;
    }

    public ObjectId getHead(File workspace, String gitHome) {
        File git_home_directory = getGitHome(workspace, gitHome);
         if (git_home_directory == null) {
             throw new IllegalArgumentException("Failed to find GIT_HOME in "
                     + workspace.getAbsolutePath() + File.separatorChar + gitHome);
         }
         Repository repo = getRepository(git_home_directory);
         if (repo == null) {
             throw new IllegalArgumentException("Failed to read repository from "
                     + git_home_directory.getAbsolutePath());
         }
         ObjectId head = getHead(repo);
         if (head == null) {
            throw new IllegalArgumentException("HEAD is null for " + repo.getDirectory().getAbsolutePath()
                     + ", are you sure that you're using git?");
         }
         return head;

    }
}
