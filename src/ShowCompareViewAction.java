import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class ShowCompareViewAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        VirtualFile[] files = DataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
        Project project = DataKeys.PROJECT.getData(event.getDataContext());

        if (files.length == 2) {
            DiffRequest req = DiffRequestFactory.getInstance().createFromFiles(project, files[0], files[1]);
            DiffManager.getInstance().showDiff(project, req);
        }
    }
}
