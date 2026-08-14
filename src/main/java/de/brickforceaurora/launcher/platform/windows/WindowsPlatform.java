package de.brickforceaurora.launcher.platform.windows;

import java.io.IOException;
import java.nio.file.Files;

import de.brickforceaurora.launcher.LauncherApp;
import de.brickforceaurora.launcher.Main;
import de.brickforceaurora.launcher.config.UpdaterConfig;
import de.brickforceaurora.launcher.platform.Platform;
import de.brickforceaurora.launcher.platform.windows.update.WinLauncherUpdater;
import de.brickforceaurora.launcher.ui.UserInterface;
import de.brickforceaurora.launcher.updater.UpdateManager;
import de.brickforceaurora.launcher.util.TaskTracker;
import me.lauriichan.laylib.command.Actor;
import me.lauriichan.laylib.logger.ISimpleLogger;
import me.lauriichan.snowframe.ConfigModule;
import me.lauriichan.snowframe.util.Version;

public class WindowsPlatform extends Platform {

    public static volatile boolean START_AS_ADMIN = false;

    private final LauncherApp app;

    public WindowsPlatform(LauncherApp app) {
        this.app = app;
    }

    @Override
    public void applyLanguageFix(Actor<?> actor) {
        WindowsHelper.applyRegistryLanguageFix();
        actor.sendMessage("Applied registry language fix.");
    }

    @Override
    public void applyWindowedFix(Actor<?> actor) {
        WindowsHelper.applyWindowedFix();
        actor.sendMessage("Applied registry windowed fix.");
    }

    @Override
    public boolean updateLauncher() {
        final UpdaterConfig config = app.snowFrame().module(ConfigModule.class).manager().config(UpdaterConfig.class);
        final UserInterface userInterface = app.userInterface();

        userInterface.newVersionAvailable.set(false);
        userInterface.newVersionText.set("");

        userInterface.mainText.set("Checking for launcher updates...");
        userInterface.mainProgress.set(0.00f);

        final UpdateManager updateManager = new UpdateManager(app.snowFrame().logger(), new WinLauncherUpdater());
        try {
            Files.deleteIfExists(app.appDirectory().resolve("BrickForceAurora-Update-%s.exe".formatted(Main.version().toString())));
        } catch (final IOException _) {
            // We can ignore if this fails
        }
        if (!updateManager.checkForUpdates(config, Main.version()) || !updateManager.hasUpdates()) {
            return false;
        }
        Version newVersion;
        if ((newVersion = updateManager.applyUpdates(new TaskTracker(userInterface.mainText, userInterface.mainProgress, 1f, 100),
            app.appDirectory())) != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    new ProcessBuilder("cmd.exe", "/c", "start", "BrickForceAurora-Update-%s.exe".formatted(newVersion.toString()),
                        "/verysilent").directory(app.appDirectory().toFile()).start();
                } catch (final IOException _) {
                }
            }));
            Main.shutdown();
            return true;
        }
        return false;
    }

    @Override
    public void startGame() {
        final String gamePath = app.gameDirectory().resolve("BrickForce.exe").toString();
        final ISimpleLogger logger = app.snowFrame().logger();
        logger.info("Launching game '{0}'", gamePath);
        if (!WindowsHelper.isAuthorized("BrickForce", gamePath)) {
            WindowsHelper.authorizeProgram("BrickForce", gamePath);
        }
        try {
            WindowsHelper.startProgram(START_AS_ADMIN, app.gameDirectory().resolve("BrickForce.exe").toFile());
        } catch (final IOException e) {
            logger.error("Failed to launch game", e);
        }
    }

}
