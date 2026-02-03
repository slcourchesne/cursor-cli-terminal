package cn.lupishan.agent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public final class CursorAgentUtils {
    private static final String NOTIF_GROUP_ID = "cursor.agent.notifications";

    private CursorAgentUtils() {
    }

    public static String resolveAgentAbsolutePath(File workDir) {
        String env = System.getenv("CURSOR_AGENT");
        if (env != null && !env.trim().isEmpty()) {
            String p = expandHome(env.trim());
            if (new File(p).canExecute()) return p;
        }

        // Try multiple shells to maximize compatibility
        String[] shells = {
                "/opt/homebrew/bin/fish",
                "/bin/zsh",  // macOS default shell (handles oh-my-zsh)
                "/bin/bash", // Common alternative
                "/bin/sh"    // Fallback
        };

        for (String shell : shells) {
            if (!new File(shell).exists()) continue;

            try {
                Process proc = new ProcessBuilder(shell, "-lc", "command -v cursor-agent || true")
                        .directory(workDir)
                        .redirectErrorStream(true)
                        .start();
                proc.waitFor();
                byte[] out = proc.getInputStream().readAllBytes();
                String resolved = new String(out, StandardCharsets.UTF_8).trim();
                if (!resolved.isEmpty() && new File(resolved).canExecute()) return resolved;
            } catch (Exception ignore) {
            }
        }

        return null;
    }

    public static String expandHome(String p) {
        if (p == null) return null;
        if (p.equals("~")) return System.getProperty("user.home");
        if (p.startsWith("~" + File.separator)) {
            return p.replaceFirst("~", System.getProperty("user.home"));
        }
        return p;
    }

    public static void exec(Project project, ShellTerminalWidget widget, String cmd) {
        try {
            widget.executeCommand(cmd);
        } catch (IOException e) {
            notify(project, "error executing command：" + e.getMessage(), NotificationType.ERROR);
        }
    }

    public static void notify(Project project, String msg, NotificationType type) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIF_GROUP_ID);
        Notification notification = (group != null)
                ? group.createNotification(msg, type)
                : new Notification(NOTIF_GROUP_ID, "Cursor CLI Terminal", msg, type);
        Notifications.Bus.notify(notification, project);
    }
}
