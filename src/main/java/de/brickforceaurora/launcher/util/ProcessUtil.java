package de.brickforceaurora.launcher.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

import de.brickforceaurora.launcher.LauncherApp;

public final class ProcessUtil {

    public static record ProgramResult(Exception exception, int code, String result, String error) {}

    private ProcessUtil() {
        throw new UnsupportedOperationException();
    }

    public static ProgramResult runAndRead(String[] command) {
        return runAndRead(null, command);
    }

    public static ProgramResult runAndRead(Path directory, String[] command) {
        final ProcessBuilder builder = new ProcessBuilder(command);
        if (directory != null) {
            builder.directory(directory.toAbsolutePath().toFile());
        }
        return await(builder);
    }

    public static ProgramResult await(ProcessBuilder builder) {
        String commandLine = String.join(" ", builder.command());
        try {
            return await(commandLine, builder.start());
        } catch (IOException e) {
            if (LauncherApp.LOGGER.isDebug()) {
                LauncherApp.LOGGER.debug("Execution of '{0}' failed", e, commandLine);
            }
            return new ProgramResult(e, -1, "", "");
        }
    }

    public static ProgramResult await(Process process) {
        return await(process.info().commandLine().orElseThrow(), process);
    }

    private static ProgramResult await(String commandLine, Process process) {
        try {
            BufferedReader inputReader = process.inputReader(), errorReader = process.errorReader();
            int cycles = 100;
            StringBuilder resultBuilder = new StringBuilder(), errorBuilder = new StringBuilder();
            while (process.isAlive()) {
                if (inputReader.ready()) {
                    resultBuilder.append(inputReader.readAllAsString());
                }
                if (errorReader.ready()) {
                    errorBuilder.append(errorReader.readAllAsString());
                }
                Thread.sleep(5);
                if (cycles-- == 0) {
                    break;
                }
            }
            if (process.isAlive()) {
                process.destroy();
            }
            if (inputReader.ready()) {
                resultBuilder.append(inputReader.readAllAsString());
            }
            if (errorReader.ready()) {
                errorBuilder.append(errorReader.readAllAsString());
            }
            int code = -1;
            if (!process.isAlive()) {
                code = process.exitValue();
            }
            String result = resultBuilder.toString(), error = errorBuilder.toString();
            if (LauncherApp.LOGGER.isDebug()) {
                LauncherApp.LOGGER.debug("""
                    Execution of '{3}' done
                    =======================
                    Code: {0}
                    Error: {2}
                    Result: {1}
                    """, code, result, error, commandLine);
            }
            return new ProgramResult(null, code, result, error);
        } catch (IOException | InterruptedException e) {
            if (LauncherApp.LOGGER.isDebug()) {
                LauncherApp.LOGGER.debug("Execution of '{0}' failed", e, commandLine);
            }
            return new ProgramResult(e, -1, "", "");
        }
    }

}
