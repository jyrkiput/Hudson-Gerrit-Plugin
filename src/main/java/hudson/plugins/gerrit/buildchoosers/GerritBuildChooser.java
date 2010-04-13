package hudson.plugins.gerrit.buildchoosers;


import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.plugins.gerrit.GerritNotifier;
import hudson.plugins.git.*;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.ExtensionBuildChooser;
import hudson.plugins.git.util.GitUtils;
import hudson.util.DescribableList;
import org.joda.time.DateTime;
import org.spearce.jgit.lib.ObjectId;

import java.io.IOException;
import java.util.*;

@Extension
public class GerritBuildChooser extends ExtensionBuildChooser {

    private boolean isRequired = false;
    private final String separator = "#";
    private IGitAPI               git;
    private GitUtils utils;
    private GitSCM                gitSCM;

    //-------- Data -----------
    private BuildData data;

    public GerritBuildChooser() {
        this.gitSCM = null;
        this.git = null;
        this.utils = null;
        this.data = null;

    }

    public GerritBuildChooser(GitSCM gitSCM, IGitAPI git, GitUtils utils, BuildData data)
    {
        this.gitSCM = gitSCM;
        this.git = git;
        this.utils = utils;
        this.data = data == null ? new BuildData() : data;
    }
    
    @Override
    public void setUtilities(GitSCM gitSCM, IGitAPI git, GitUtils gitUtils) {
        this.gitSCM = gitSCM;
        this.git = git;
        this.utils = gitUtils;
        this.data = data == null ? new BuildData() : data;
    }

    /**
     * Determines which Revisions to build.
     *
     * Uses git log --all to get every commit in repository. Then orders commits by commit time
     * and determines what to build next.
     *
     * Doesn't care about branches.
     * @throws IOException
     * @throws GitException
     */
    @Override
    public Collection<Revision> getCandidateRevisions(boolean isPollCall, String singleBranch)
            throws GitException, IOException {

        Build lastTimeBased = data.getLastBuildOfBranch("timebased");

        Revision last = null;
        if(lastTimeBased != null) {
            last = data.getLastBuildOfBranch("timebased").getRevision();
            if(!last.getSha1String().equals(data.getLastBuiltRevision().getSha1String())) {
                //previous build wasn't timebased, so consider this as a new start
                last = null;
            }
        }

        String result = git.getAllLogEntries(singleBranch);
        Collection<TimedCommit> commits = sortRevList(result);
        Iterator<TimedCommit> i = commits.iterator();
        ArrayList<Revision> revs = new ArrayList<Revision>();
        DateTime lastBuilt = null;

        while(i.hasNext()) {
            TimedCommit tc = i.next();
            //When encountered last build, break
            if(last != null && tc.commit.name().equals(last.getSha1String())) {
                break;
            }
            addToRevs(revs, tc);
        }


        if(last == null) {
            return revs;
        }
        if(revs.size() == 0 && !isPollCall) {
            return Collections.singletonList(last);
        }
        //reverse order
        ArrayList<Revision> finalRevs = new ArrayList<Revision>();
        for(int j = revs.size() - 1 ; j >= 0 ; j--) {
            finalRevs.add(revs.get(j));

        }
        return finalRevs;

    }

    private void addToRevs(ArrayList<Revision> revs, TimedCommit tc) {
        Revision rev = new Revision(tc.commit);
        rev.getBranches().add(new Branch("timebased", rev.getSha1()));
        revs.add(rev);
    }

    /* This returns commits that are always in same order.
     *
     */
    private Collection<TimedCommit> sortRevList(String logOutput) {
        SortedSet<TimedCommit> timedCommits = new TreeSet<TimedCommit>();
        String[] lines = logOutput.split("\n");
        for (String s : lines ) {
            timedCommits.add(parseCommit(s));
        }

        return timedCommits;
    }

    private TimedCommit parseCommit(String line) {

        String[] lines = line.split(separator);
        /*Line has ' in the beginning and in the end */
        String id = lines[0].substring(1);
        String date = lines[1].substring(0, lines[1].length() - 1 );
        //From seconds to milliseconds
        return new TimedCommit(ObjectId.fromString(id),
                new DateTime(Long.parseLong(date) * 1000));
    }

    private class TimedCommit implements Comparable<TimedCommit> {

        private ObjectId commit;
        public DateTime when;

        public TimedCommit(ObjectId c, DateTime when) {
            this.commit = c;
            this.when = when;
        }

        public ObjectId getCommit() {
            return commit;
        }

        @Override
        public int compareTo(TimedCommit o) {
            //I want newest to be first
            int result = -(when.compareTo(o.when));
            //If time is equal, keep order from log.
            if(result == 0) {
                return -1;
            }
            return result;
        }
     }

    @Override
    public Build revisionBuilt(Revision revision, int buildNumber, Result result )
    {
        Build build = new Build(revision, buildNumber, result);
        data.saveBuild(build);
        return build;
    }


    @Override
    public Action getData()
    {
        return data;
    }

    @Override
    public String getName() {
        return "Gerrit-Plugin";
    }

       /**
     * Is this build chooser required to be used. Useful when some other
     * functionality depends on using this specific build chooser. Defaults to false
     *  * @return
     */
    @Override
    public boolean isRequiredFor(AbstractProject p) {
           //If GerritNotifier is in publisher list, it's required to use this BuildChooser
           DescribableList list = p.getPublishersList();
           return list.get(GerritNotifier.class) != null;

       }

}
