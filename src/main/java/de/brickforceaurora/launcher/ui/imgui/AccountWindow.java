package de.brickforceaurora.launcher.ui.imgui;

import de.brickforceaurora.launcher.account.AccountSession;
import de.brickforceaurora.launcher.account.AccountSession.Account;
import de.brickforceaurora.launcher.account.AccountSession.AccountException;
import de.brickforceaurora.launcher.account.LocalServer;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

/**
 * Login / register and private lobby hosting (toggle with F2).
 */
public final class AccountWindow {

    private final AccountSession session;
    private final LocalServer localServer;

    private final ImBoolean open = new ImBoolean(true);
    private final ImString username = new ImString(32);
    private final ImString password = new ImString(128);
    private final ImBoolean hostLocally = new ImBoolean(true);
    private final ImString joinHost = new ImString(128);

    private volatile String status = "";
    private volatile boolean busy = false;

    public AccountWindow(final AccountSession session, final LocalServer localServer, final String username, final boolean hostLocally,
        final String joinHost) {
        this.session = session;
        this.localServer = localServer;
        this.username.set(username == null ? "" : username);
        this.hostLocally.set(hostLocally);
        this.joinHost.set(joinHost == null ? "" : joinHost);
    }

    public void toggle() {
        open.set(!open.get());
    }

    public void show() {
        open.set(true);
    }

    public boolean hostLocally() {
        return hostLocally.get();
    }

    public String joinHost() {
        return joinHost.get().trim();
    }

    public void status(final String status) {
        this.status = status;
    }

    public void render() {
        if (!open.get()) {
            return;
        }
        ImGui.setNextWindowSize(380, 0, ImGuiCond.FirstUseEver);
        if (!ImGui.begin("Aurora Account", open, ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.end();
            return;
        }
        if (session.isLoggedIn()) {
            final Account account = session.account();
            ImGui.text("Logged in as " + account.username());
            ImGui.text("Force Points: %d   Tokens: %d".formatted(account.forcePoints(), account.tokens()));
            if (ImGui.button("Log out")) {
                session.logout();
                status = "Logged out";
            }
        } else {
            ImGui.inputText("Username", username);
            ImGui.inputText("Password", password, ImGuiInputTextFlags.Password);
            if (busy) {
                ImGui.beginDisabled();
            }
            if (ImGui.button("Log in")) {
                run(() -> {
                    final Account account = session.login(username.get().trim(), password.get());
                    password.set("");
                    status = "Welcome " + account.username() + "!";
                });
            }
            ImGui.sameLine();
            if (ImGui.button("Register")) {
                run(() -> {
                    session.register(username.get().trim(), password.get());
                    session.login(username.get().trim(), password.get());
                    password.set("");
                    status = "Account created, you are logged in";
                });
            }
            if (busy) {
                ImGui.endDisabled();
            }
        }

        ImGui.separator();
        ImGui.checkbox("Host a private lobby on this PC", hostLocally);
        if (hostLocally.get()) {
            ImGui.textWrapped("Your server is only reachable from this PC, friends join through Steam. Your IP address stays hidden.");
        } else {
            ImGui.inputText("Server address", joinHost);
        }
        ImGui.text(localServer.isRunning() ? "Local server: running" : "Local server: stopped");
        if (localServer.isRunning()) {
            ImGui.sameLine();
            if (ImGui.button("Stop server")) {
                localServer.stop();
            }
        }

        final String current = status;
        if (!current.isEmpty()) {
            ImGui.separator();
            ImGui.textWrapped(current);
        }
        ImGui.end();
    }

    @FunctionalInterface
    private interface Action {
        void run() throws AccountException;
    }

    private void run(final Action action) {
        if (busy) {
            return;
        }
        busy = true;
        status = "Please wait...";
        Thread.ofVirtual().name("account-action").start(() -> {
            try {
                action.run();
            } catch (final AccountException e) {
                status = e.getMessage();
            } finally {
                busy = false;
            }
        });
    }

}
