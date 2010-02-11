package com.fudeco.hudson;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.File;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link GerritNotifier} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(Build, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class GerritNotifier extends Notifier {

    private final String name;

    @DataBoundConstructor
    public GerritNotifier(String name) {
        this.name = name;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return name;
    }

    private Repository getRepository(File workspace, BuildListener listener) {
        String git_path = workspace.getAbsolutePath() + File.separatorChar + ".git";
        File git_home = new File(git_path);
        if (!git_home.isDirectory()) {
            listener.getLogger().println("Couldn't find GIT_HOME, tried " + git_path);
            return null;
        }
        Repository repo = null;
        try {
            repo = new Repository(git_home);
        } catch (IOException e) {
            listener.getLogger().println(workspace.getAbsolutePath() + " isn't git repository");
            return null;
        }
        return repo;
    }

    private ObjectId getHead(Repository repo, BuildListener listener) {
        ObjectId head = null;
        try {
            head = repo.resolve("HEAD");
        } catch (IOException e) {
            listener.getLogger().println("Failed to resolve HEAD");
            return null;
        }

        if (head == null) {
            listener.getLogger().println("HEAD is null for " + repo.getDirectory().getAbsolutePath()
                    + ", are you sure that you're using git?");
            return null;
        }
        return head;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, final BuildListener listener) {
        // this is where you 'build' the project
        // since this is a dummy, we just say 'hello world' and call that a build

        // this also shows how you can consult the global configuration of the builder
        if (getDescriptor().useFrench()) {
            listener.getLogger().println("Bonjour, " + name + "!");
        } else {
            listener.getLogger().println("Hello, " + name + "!");
        }

        FilePath ws = build.getWorkspace();

        try {

            ws.act(new FileCallable<Boolean>() {
                // if 'file' is on a different node, this FileCallable will
                // be transfered to that node and executed there.

                public Boolean invoke(File workspace, VirtualChannel channel) {
                    // f and file represents the same thing
                    Repository repo = getRepository(workspace, listener);
                    if (repo == null) {
                        listener.getLogger().println("Failed to get repository");
                        return false;
                    }
                    ObjectId head = getHead(repo, listener);
                    listener.getLogger().println(head.name());
                    return true;
                }
            });
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e2) {
            return false;
        }

        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for {@link GerritNotifier}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/GerritNotifier/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a name");
            }
            if (value.length() < 4) {
                return FormValidation.warning("Isn't the name too short?");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Say hello world";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            useFrench = o.getBoolean("useFrench");
            save();
            return super.configure(req, o);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
        public boolean useFrench() {
            return useFrench;
        }
    }
}

