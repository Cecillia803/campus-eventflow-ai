package campus.eventflow.agent;
import campus.eventflow.contracts.Actor;
import java.time.*;
import java.util.Objects;
import java.util.function.Supplier;
import static campus.eventflow.contracts.Failure.*;

/** Creates confirmation proposals only, never a reservation or enrollment. */
public final class DraftService {
    public record SessionRules(String sessionId, long version, boolean isDemo, boolean permitsUser,
                               boolean cancelled, Instant opensAt, Instant closesAt) {}
    public interface Store {
        // Atomically replay key + same payload, or load authoritative rules and persist supplied draft.
        ConfirmationPolicy.Draft prepareOwned(Actor actor, String commandKey, String sessionId, long expectedRuleVersion,
                java.util.function.Function<SessionRules, ConfirmationPolicy.Draft> create);
    }
    private final Store store;
    private final Clock clock;
    private final Duration validity;
    private final Supplier<String> ids;
    public DraftService(Store store, Clock clock, Duration validity, Supplier<String> ids) {
        this.store = Objects.requireNonNull(store); this.clock = Objects.requireNonNull(clock);
        this.validity = Objects.requireNonNull(validity); this.ids = Objects.requireNonNull(ids);
        require(!validity.isNegative() && !validity.isZero(), Code.INVALID_INPUT, "Positive draft validity required");
    }
    public ConfirmationPolicy.Draft prepare(Actor actor, String sessionId, long expectedRuleVersion, String key) {
        String target = text(sessionId, "sessionId"), commandKey = text(key, "commandKey");
        require(commandKey.length() <= 128, Code.INVALID_INPUT, "Command key too long");
        return store.prepareOwned(actor, commandKey, target, expectedRuleVersion, rules -> {
            require(target.equals(rules.sessionId()) && rules.isDemo(), Code.INVALID_INPUT, "Explicit demo session required");
            require(rules.version() == expectedRuleVersion, Code.VERSION_CONFLICT, "Rules changed");
            require(!rules.cancelled(), Code.CANCELLED, "Session cancelled");
            require(rules.permitsUser(), Code.FORBIDDEN, "Not eligible");
            Instant now = clock.instant();
            require(!now.isBefore(rules.opensAt()), Code.NOT_OPEN, "Not open");
            require(now.isBefore(rules.closesAt()), Code.CLOSED, "Closed");
            Instant expires = now.plus(validity);
            if (expires.isAfter(rules.closesAt())) expires = rules.closesAt();
            return new ConfirmationPolicy.Draft(ids.get(), actor.userId(), target, rules.version(),
                    expires, ConfirmationPolicy.Status.PENDING);
        });
    }
}

