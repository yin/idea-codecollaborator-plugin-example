import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.smartbear.CollabClientException;

import java.io.IOException;

public class ShowCompareViewAction extends AnAction {
    private static final Logger log = PluginManager.getLogger();
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        VirtualFile file = DataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        VirtualFile[] selected = FileChooser.chooseFiles(
                new FileChooserDescriptor(true, false, false, false, false, false),
                project, file);

        try {
            ReviewFolder reviewFolder = new ReviewFolderManager().createReviewFolder(selected[0].getCanonicalPath());
            VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
            VirtualFile before = fileSystem.findFileByPath(reviewFolder.before());
            VirtualFile after = fileSystem.findFileByPath(reviewFolder.after());
            DiffRequest req = DiffRequestFactory.getInstance().createFromFiles(project, before, after);
            DiffManager.getInstance().showDiff(project, req);
        } catch(IOException ex) {
            log.error("IO error preparing review folder", ex);
        } catch (CollabClientException ex) {
            log.error("Weird error preparing review folder", ex);
        }

    }
}
