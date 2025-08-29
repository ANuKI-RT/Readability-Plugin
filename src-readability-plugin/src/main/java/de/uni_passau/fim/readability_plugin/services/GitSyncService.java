package de.uni_passau.fim.readability_plugin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * During the study carried out with the implemented readability plugin,
 * the source code implemented by the participants must be regularly pushed to a git repo,
 * in order to analyse the results afterward.
 *
 * The git sync service checks if the project opened has a git repo assigned and if this is the case,
 * will periodically create a commit for the code and push the commit to the remote repo connected with the local
 * repository (if a remote repo is present)
 */
@Service(Service.Level.PROJECT)
public final class GitSyncService implements Disposable {

    private Project project;
    private ScheduledFuture scheduledSync;
    private UsernamePasswordCredentialsProvider user;
    private Git git;

    private String projectUrl;

    public GitSyncService(Project project) {

        this.project = project;

    }

    /**
     * Starts the git sync process by periodically commiting and pushing the code.
     * @param syncPeriod the time period in minutes defining how often a commit is made
     */
    public void startSyncing(int syncPeriod) {

        SettingsService settingsService = project.getService(SettingsService.class);

        if(!settingsService.useGitSync()) {
            System.out.println("Git sync is disabled - skipping sync");
            return;
        }

        try {
            File gitWorkDir = new File(project.getBasePath());

            git = Git.open(gitWorkDir);
            List<RemoteConfig> remotes = git.remoteList().call();
            RemoteConfig defaultRemote = remotes.get(0);
            URIish defaultUri = defaultRemote.getURIs().get(0);
            String username = defaultUri.getUser();
            String password = defaultUri.getPass();

            if(username == null || password == null) {
                System.out.println("Git Sync: failed to detect remote repo - skipping sync");
                return;
            }

            projectUrl = defaultUri.toString();

            user = new UsernamePasswordCredentialsProvider(username,password);

        }
         catch (Exception e) {
            System.out.println("Git Sync: failed fetch remote repo user and password - skipping sync");
            e.printStackTrace();
            return;
        }


        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        scheduledSync = executorService.scheduleAtFixedRate(() -> {
            try {
                commitAllFilesToBranch();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, syncPeriod, TimeUnit.MINUTES);
    }

    /**
     * Creates a commit for all files and pushed it to the remote repo.
     * @throws GitAPIException if any interactions with the git api caused an exception
     */
    private void commitAllFilesToBranch() throws GitAPIException {

        git.add().addFilepattern(".").call();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        git.commit().setMessage(timestamp.toString()).call();
        git.push().setRemote(projectUrl).setCredentialsProvider(user).call();

    }

    /**
     * Ends the periodic sync if the GitSyncService object is disposed.
     */
    @Override
    public void dispose() {
        if(scheduledSync != null) {
            scheduledSync.cancel(false);
        }
    }
}

