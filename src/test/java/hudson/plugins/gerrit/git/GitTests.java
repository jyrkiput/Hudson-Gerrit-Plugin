package hudson.plugins.gerrit.git;

import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;


/**
 *
 */
public class GitTests {

    static int counter = 0;

    public static class MockSystemReader extends SystemReader {

       private volatile String hostname;

		public String getenv(String variable) {
			return System.getenv(variable);
		}

		public String getProperty(String key) {
			return System.getProperty(key);
		}

		public FileBasedConfig openUserConfig() {
			final File home = FS.userHome();
			return new FileBasedConfig(new File(home, ".gitconfig"));
		}

		public String getHostname() {
			if (hostname == null) {
				try {
					InetAddress localMachine = InetAddress.getLocalHost();
					hostname = localMachine.getCanonicalHostName();
				} catch (UnknownHostException e) {
					// we do nothing
					hostname = "localhost";
				}
				assert hostname != null;
			}
			return hostname;
		}
		@Override
		public long getCurrentTime() {
            counter += 1000;
            //Cheating here to get different timestamps for commits
			return System.currentTimeMillis() + counter;
		}

		@Override
		public int getTimezone(long when) {
			return TimeZone.getDefault().getOffset(when) / (60 * 1000);
		}
    }

    public static Logger logger = Logger.getLogger(GitTests.class.getName());

    @Rule
    public static TemporaryFolder gitRepoFolder = new TemporaryFolder();

    Commit c1;
    File work_folder;
    File git_repo;
    Repository repo;

    GitTools git;
    Random r = new Random();

    @BeforeClass
    public static void initMock() {
        SystemReader mock = new MockSystemReader();
        SystemReader.setInstance(mock);
    }
    @Before
    public void initGit() throws IOException {
        work_folder = gitRepoFolder.newFolder("repository");
        git_repo = new File(work_folder,".git/");
        repo = new Repository(git_repo);

        repo.create();
        File file1 = new File(work_folder, "file1");
        FileWriter writer = new FileWriter(file1);
        writer.write("test_text");


        ObjectId id = null;
        for(int i = 0 ; i < 2 ; i++)
        {
            id = makeCommit(repo, "commit1", id).getCommitId();
            updateRef(repo, "commit" + i, id, "HEAD");
        }
        c1 = addCommitToBranch("HEAD", id);

        git = new GitTools();
    }

    @After
    public void destroy()
    {
        c1 = null;
        work_folder = null;
        git_repo = null;
        repo = null;

    }

    private Commit makeCommit(Repository repo, String msg, ObjectId parent) throws IOException {

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 40 ; i++) {
            sb.append(Integer.toHexString(r.nextInt(16)));
        }
        ObjectId t1Id = ObjectId.fromString(sb.toString());

        Tree tree = new Tree(repo);
        tree.setId(t1Id);
        PersonIdent ident = new PersonIdent(repo);

        Commit commit = new Commit(repo);
        commit.setAuthor(ident);
        commit.setCommitter(ident);
        commit.setTree(tree);
        commit.setMessage(msg);
        if (parent != null ) {

            ObjectId parents[] = { parent };
            commit.setParentIds( parents );

        }
        commit.commit();

        return commit;
    }

    private void updateRef(Repository repo, String msg, ObjectId id, final String ref) throws IOException {
        RefUpdate refUp = repo.updateRef(ref);
        refUp.setNewObjectId(id);
        refUp.setRefLogMessage("id: " + msg, false);
        refUp.forceUpdate();
    }

    private Commit addCommitToBranch(String branch_name, ObjectId parent) throws IOException {
        return addCommitToBranch(branch_name, parent, "branch_commit");
    }

    private Commit addCommitToBranch(String branch_name, ObjectId parent, final String msg) throws IOException {
        Commit branch_commit = makeCommit(repo, msg, parent);
        updateRef(repo, "Test branching", branch_commit.getCommitId(), branch_name);
        return branch_commit;
    }

    @Test public void getHeadRevision()
    {
        ObjectId id = git.getHead(git_repo.getParentFile(), ".git");
        assertEquals(c1.getCommitId().name(), id.name());

    }


    @Test public void getHeadRevisionWithMultipleBranches() throws IOException {
        //add new branch
        String branch_name = Constants.R_HEADS + "test_branch";
        //add commit to new branch
        Commit branch_commit = addCommitToBranch(branch_name, c1.getCommitId());
        //check that revision is right
        ObjectId id = git.getHead(git_repo.getParentFile(), ".git");
        assertEquals(c1.getCommitId().name(), id.name());
        //move HEAD to new branch and test
        updateRef(repo, "Moving to test branch", repo.resolve(branch_name).toObjectId(), "HEAD");

        id = git.getHead(git_repo.getParentFile(), ".git");

        assertEquals(branch_commit.getCommitId(), repo.resolve(branch_name));
        //assertEquals(branch_name, repo.getBranch());
        assertEquals(branch_commit.getCommitId().name(), id.name());
    }



}
