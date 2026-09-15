package campus.eventflow.registration;
import campus.eventflow.contracts.Actor;
import java.time.Clock;
import java.util.Objects;
import java.util.OptionalLong;
import static campus.eventflow.contracts.Failure.*;

public final class RegistrationService {
    /** Must be local/read-only and transaction-compatible; no model calls in the hot path. */
    public interface Eligibility { boolean permits(String userId, String sessionId); }
    private final RegistrationStore store;
    private final Eligibility eligibility;
    private final Clock clock;
    private final RegistrationPolicy policy = new RegistrationPolicy();
    public RegistrationService(RegistrationStore store, Eligibility eligibility, Clock clock) {
        this.store = Objects.requireNonNull(store); this.eligibility = Objects.requireNonNull(eligibility);
        this.clock = Objects.requireNonNull(clock);
    }
    public RegistrationPolicy.Change register(Actor actor, String sessionId, String key) {
        var command = command(actor, sessionId, key, "REGISTER", OptionalLong.empty());
        return store.transact(command, session -> {
            require(session.id().equals(command.sessionId()), Code.INVALID_INPUT, "Wrong session snapshot");
            return policy.register(session, actor.userId(), eligibility.permits(actor.userId(), session.id()), clock.instant());
        });
    }
    public RegistrationPolicy.Change cancel(Actor actor, String sessionId, long expectedSequence, String key) {
        require(expectedSequence > 0, Code.INVALID_INPUT, "Registration sequence required");
        var command = command(actor, sessionId, key, "CANCEL", OptionalLong.of(expectedSequence));
        return store.transact(command, session -> {
            require(session.id().equals(command.sessionId()), Code.INVALID_INPUT, "Wrong session snapshot");
            return policy.cancel(session, actor.userId(), expectedSequence, clock.instant());
        });
    }
    public RegistrationPolicy.Change cancelSession(Actor actor, String sessionId, String key) {
        require(actor.roles().contains(Actor.Role.ADMIN), Code.FORBIDDEN, "Administrator required");
        var command = command(actor, sessionId, key, "CANCEL_SESSION", OptionalLong.empty());
        return store.transact(command, session -> {
            require(session.id().equals(command.sessionId()), Code.INVALID_INPUT, "Wrong session snapshot");
            return policy.cancelSession(session);
        });
    }
    private RegistrationStore.Command command(Actor actor, String sessionId, String key, String operation, OptionalLong sequence) {
        Objects.requireNonNull(actor);
        key = text(key, "idempotencyKey"); sessionId = text(sessionId, "sessionId");
        require(key.length() <= 128, Code.INVALID_INPUT, "Idempotency key too long");
        return new RegistrationStore.Command(actor.userId(), key, sessionId, operation, sequence);
    }
}
