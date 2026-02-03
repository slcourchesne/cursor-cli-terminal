package cn.lupishan.agent;

import java.awt.*;
import java.io.File;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class CursorToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "Cursor CLI Terminal";
    public static final com.intellij.openapi.util.Key<ShellTerminalWidget> WIDGET_KEY =
            com.intellij.openapi.util.Key.create("CURSOR_AGENT_WIDGET");
    private static final com.intellij.openapi.util.Key<Boolean> AUTORUN_DONE_KEY =
            com.intellij.openapi.util.Key.create("CURSOR_AGENT_AUTORUN_DONE");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        String workDir = project.getBasePath() != null ? project.getBasePath() : System.getProperty("user.home");

        ShellTerminalWidget widget = TerminalToolWindowManager.getInstance(project)
                .createLocalShellWidget(workDir, TOOL_WINDOW_ID);

        panel.add(widget.getComponent(), BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        content.putUserData(WIDGET_KEY, widget);
        toolWindow.getContentManager().addContent(content);

        ToolWindow term = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (term != null && term.isVisible()) {
            term.hide(null);
        }

        if (Boolean.TRUE.equals(content.getUserData(AUTORUN_DONE_KEY))) return;
        content.putUserData(AUTORUN_DONE_KEY, true);

        ApplicationManager.getApplication().invokeLater(() -> autorun(project, widget, workDir));
    }

    private void autorun(Project project, ShellTerminalWidget widget, String workDir) {
        String agent = CursorAgentUtils.resolveAgentAbsolutePath(new File(workDir));
        if (agent == null) {
            CursorAgentUtils.notify(project, "cursor-agent cli not found", NotificationType.WARNING);
            return;
        }
        CursorAgentUtils.exec(project, widget, "cursor-agent");
    }
}
