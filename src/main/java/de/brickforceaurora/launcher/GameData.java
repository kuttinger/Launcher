package de.brickforceaurora.launcher;

import java.io.IOException;

import de.brickforceaurora.launcher.data.DataStore;
import de.brickforceaurora.launcher.data.StoredData;
import me.lauriichan.snowframe.util.Version;

public final class GameData {

    private GameData() {
        throw new UnsupportedOperationException();
    }

    public static final DataStore STORE = LauncherApp.get().gameData();

    public static final StoredData<Version> GAME_VERSION = STORE.register("version", DataHandler.VERSION, new Version(0, 0, 0));

    /*
     * Aurora account / standalone server, read by the game client on start
     */

    /**
     * Play ticket of the account service, empty when not logged in
     */
    public static final StoredData<String> AURORA_TICKET = STORE.register("aurora.ticket", DataHandler.STRING, "");
    /**
     * Server the game should connect to ({@code 127.0.0.1} when this player hosts)
     */
    public static final StoredData<String> AURORA_SERVER_HOST = STORE.register("aurora.server.host", DataHandler.STRING, "");
    public static final StoredData<Integer> AURORA_SERVER_PORT = STORE.register("aurora.server.port", DataHandler.INT, 18890);

    static void init() {
        try {
            STORE.load();
        } catch (final IOException e) {
            LauncherApp.LOGGER.error("Failed to initialize game data", e);
        }
    }

}
