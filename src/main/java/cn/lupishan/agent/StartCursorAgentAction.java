package cn.lupishan.agent;

import java.awt.event.InputEvent;

import org.jetbrains.annotations.NotNull;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

public class StartCursorAgentAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        InputEvent ie = e.getInputEvent();
        boolean forceRestart = ie != null && (ie.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ToolWindow tw = ToolWindowManager.getInstance(project)
                        .getToolWindow(CursorToolWindowFactory.TOOL_WINDOW_ID);
                if (tw == null) {
                    CursorAgentUtils.notify(project, "couldn't create cursor tool window：" + CursorToolWindowFactory.TOOL_WINDOW_ID, NotificationType.ERROR);
                    return;
                }
                tw.activate(null, true);

                var cm = tw.getContentManager();

                if (cm.getContentCount() == 0) {
                    new CursorToolWindowFactory().createToolWindowContent(project, tw);
                    return;
                }

                if (forceRestart) {
                    Content selected = cm.getSelectedContent();
                    if (selected != null) {
                        cm.removeContent(selected, true);
                    } else {
                        for (Content c : cm.getContents()) {
                            cm.removeContent(c, true);
                        }
                    }
                    new CursorToolWindowFactory().createToolWindowContent(project, tw);
                    return;
                }
            } catch (Throwable ex) {
                CursorAgentUtils.notify(project, "error running cursor tool window：" + ex.getMessage(), NotificationType.ERROR);
            }
        });
    }
}
