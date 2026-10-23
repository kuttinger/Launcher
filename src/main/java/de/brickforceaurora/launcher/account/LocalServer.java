package de.brickforceaurora.launcher.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import me.lauriichan.laylib.logger.ISimpleLogger;

/**
 * Runs the Aurora match server as child process of the launcher (private lobby on the players pc).
 *
 * The server is started with the same java runtime the launcher runs on, so no extra installation is needed.
 */
public final class LocalServer {

    private final ISimpleLogger logger;
    private volatile Process process;

    public LocalServer(final ISimpleLogger logger) {
        this.logger = logger;
    }

    public boolean isRunning() {
        final Process current = process;
        return current != null && current.isAlive();
    }

    /**
     * Starts the server if it doesn't run yet and waits until it accepts connections.
     *
     * @throws IOException if the jar is missing or the server didn't come up
     */
    public synchronized void start(final Path jar, final int port, final String accountUrl, final String name, final boolean publicAccess)
        throws IOException {
        if (isRunning()) {
            return;
        }
        if (!Files.isRegularFile(jar)) {
            throw new IOException("Server not found: " + jar);
        }
        final List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(jar.toAbsolutePath().toString());
        command.add("--port=" + port);
        // Only reachable from this pc unless the player explicitly opts in, see AccountConfig server.public_access
        command.add("--bind=" + (publicAccess ? "0.0.0.0" : "127.0.0.1"));
        command.add("--public-host=127.0.0.1");
        command.add("--name=" + name);
        if (accountUrl != null && !accountUrl.isBlank()) {
            command.add("--account-url=" + accountUrl);
        }
        final Path workDir = jar.toAbsolutePath().getParent();
        final Process started = new ProcessBuilder(command).directory(workDir.toFile()).redirectErrorStream(true).start();
        process = started;
        Thread.ofVirtual().name("local-server-log").start(() -> pipeLog(started));
        logger.info("Started local server (pid {0}) on port {1}", started.pid(), port);

        final long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (!started.isAlive()) {
                throw new IOException("Server stopped right after start (exit code " + started.exitValue() + "), see launcher console");
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
                return;
            } catch (final IOException notYet) {
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for the server");
                }
            }
        }
        stop();
        throw new IOException("Server didn't start within 20 seconds");
    }

    public synchronized void stop() {
        final Process current = process;
        process = null;
        if (current == null || !current.isAlive()) {
            return;
        }
        logger.info("Stopping local server");
        current.destroy();
        try {
            if (!current.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                current.destroyForcibly();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            current.destroyForcibly();
        }
    }

    private void pipeLog(final Process started) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip ansi colors of the server logger
                logger.info("[Server] {0}", line.replaceAll("\u001B\\[[;\\d]*m", ""));
            }
        } catch (final IOException ignore) {
            // Process ended
        }
    }

}
