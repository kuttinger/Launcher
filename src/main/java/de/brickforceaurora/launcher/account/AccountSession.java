package de.brickforceaurora.launcher.account;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Session of the player at the Aurora account service.
 *
 * The session token is only kept in memory, so the player logs in once per launcher start.
 */
public final class AccountSession {

    public record Account(long id, String username, long xp, int forcePoints, int brickPoints, int tokens, int coins, int starDust) {}

    public static final class AccountException extends Exception {

        private static final long serialVersionUID = 1L;

        public AccountException(final String message) {
            super(message);
        }

    }

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private volatile String host;
    private volatile String token;
    private volatile Account account;

    public void host(final String host) {
        final String trimmed = host == null ? "" : host.trim();
        this.host = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public Account account() {
        return account;
    }

    public void register(final String username, final String password) throws AccountException {
        request("POST", "/auth/register", null, Map.of("username", username, "password", password));
    }

    public Account login(final String username, final String password) throws AccountException {
        final JsonNode response = request("POST", "/auth/login", null, Map.of("username", username, "password", password));
        this.token = response.path("token").asText(null);
        this.account = mapper.convertValue(response.path("account"), Account.class);
        return account;
    }

    public Account refresh() throws AccountException {
        final JsonNode response = request("GET", "/me", requireToken(), null);
        this.account = mapper.convertValue(response.path("account"), Account.class);
        return account;
    }

    /**
     * @return a new play ticket the game client uses to log into a game server
     */
    public String createTicket() throws AccountException {
        return request("POST", "/me/ticket", requireToken(), null).path("ticket").asText();
    }

    public void logout() {
        final String current = token;
        token = null;
        account = null;
        if (current != null) {
            try {
                request("POST", "/auth/logout", current, null);
            } catch (final AccountException ignore) {
                // Session is dropped locally anyway
            }
        }
    }

    private String requireToken() throws AccountException {
        final String current = token;
        if (current == null) {
            throw new AccountException("Not logged in");
        }
        return current;
    }

    private JsonNode request(final String method, final String path, final String bearer, final Object body) throws AccountException {
        if (host == null || host.isEmpty()) {
            throw new AccountException("No account server configured");
        }
        try {
            final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(host + "/api/v1" + path)).timeout(TIMEOUT)
                .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (bearer != null) {
                builder.header("Authorization", "Bearer " + bearer);
            }
            final HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            final JsonNode json = response.body() == null || response.body().isBlank() ? mapper.createObjectNode()
                : mapper.readTree(response.body());
            if (response.statusCode() == 401 && bearer != null) {
                token = null;
                account = null;
                throw new AccountException("Session expired, please log in again");
            }
            if (response.statusCode() / 100 != 2) {
                throw new AccountException(json.path("message").asText("Request failed (" + response.statusCode() + ")"));
            }
            return json;
        } catch (final IOException e) {
            throw new AccountException("Account server not reachable (" + host + ")");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AccountException("Interrupted");
        } catch (final IllegalArgumentException e) {
            throw new AccountException("Invalid account server address '" + host + "'");
        }
    }

}
