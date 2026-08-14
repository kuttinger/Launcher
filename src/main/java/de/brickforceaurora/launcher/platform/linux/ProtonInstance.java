package de.brickforceaurora.launcher.platform.linux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import de.brickforceaurora.launcher.LauncherApp;

public final class ProtonInstance {

    private final String protonPath, compatPath, steamPath, wineTricksPath;

    public ProtonInstance(Path protonPath, Path compatPath, Path steamPath) {
        this.protonPath = protonPath.resolve("proton").toAbsolutePath().toString();
        this.compatPath = compatPath.toAbsolutePath().toString();
        this.steamPath = steamPath.toAbsolutePath().toString();
        Path wineTricksPath = protonPath.resolve("protonfixes/winetricks");
        this.wineTricksPath = Files.exists(wineTricksPath) ? wineTricksPath.toAbsolutePath().toString() : null;
    }

    public String wineTricksPath() {
        return wineTricksPath;
    }

    public int executeAwait(String... arguments) throws IOException, InterruptedException {
        return executeAwait(null, arguments);
    }

    public int executeAwait(Path directory, String... arguments) throws IOException, InterruptedException {
        return execute(directory, arguments).waitFor();
    }

    public Process execute(String... arguments) throws IOException {
        return execute(null, arguments);
    }

    public Process execute(Path directory, String... arguments) throws IOException {
        String[] processArgs = new String[arguments.length + 2];
        processArgs[0] = protonPath;
        processArgs[1] = "run";
        System.arraycopy(arguments, 0, processArgs, 2, arguments.length);
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        Map<String, String> environment = processBuilder.environment();
        environment.put("SteamGameId", "0");
        environment.put("STEAM_COMPAT_DATA_PATH", compatPath);
        environment.put("STEAM_COMPAT_CLIENT_INSTALL_PATH", steamPath);
        if (directory != null) {
            processBuilder.directory(directory.toAbsolutePath().toFile());
        }
        return processBuilder.start();
    }

    public void setRegistry(String path, String key, String type, String value) {
        try {
            executeAwait("reg", "add", path, "/v", key, "/t", type, "/d", value, "/f");
        } catch (InterruptedException | IOException e) {
            if (LauncherApp.LOGGER.isDebug()) {
                LauncherApp.LOGGER.debug("Failed to set registry entry '{0}' -> '{1}'", e, path, key);
            }
        }
    }

}
