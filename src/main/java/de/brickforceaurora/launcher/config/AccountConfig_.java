package de.brickforceaurora.launcher.config;

import me.lauriichan.snowframe.config.Config;
import me.lauriichan.snowframe.config.ConfigValue;
import me.lauriichan.snowframe.config.IConfigHandler;
import me.lauriichan.snowframe.config.ISingleConfigExtension;
import me.lauriichan.snowframe.config.handler.JsonConfigHandler;
import me.lauriichan.snowframe.extension.Extension;

/**
 * Aurora account service and standalone server settings.
 */
@Extension
@Config(automatic = true)
public class AccountConfig_ implements ISingleConfigExtension {

    @ConfigValue("account.host")
    public String accountHost = "http://127.0.0.1:18900";
    @ConfigValue("account.username")
    public String username = "";

    /**
     * Server the game connects to when not hosting itself (empty = hosted by this player)
     */
    @ConfigValue("server.join_host")
    public String joinHost = "";
    @ConfigValue("server.port")
    public int serverPort = 18890;
    /**
     * Start the bundled match server when the game is launched (private lobby on this pc)
     */
    @ConfigValue("server.host_locally")
    public boolean hostLocally = true;
    /**
     * Path of the match server jar, relative to the launcher directory
     */
    @ConfigValue("server.jar")
    public String serverJar = "server/match.jar";
    @ConfigValue("server.name")
    public String serverName = "Aurora";

    @Override
    public IConfigHandler handler() {
        return JsonConfigHandler.JSON;
    }

    @Override
    public String path() {
        return "user://account.json";
    }

}
