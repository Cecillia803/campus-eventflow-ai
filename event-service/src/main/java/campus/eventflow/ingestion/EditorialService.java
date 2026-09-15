package campus.eventflow.ingestion;
import campus.eventflow.contracts.Actor;
import java.time.Clock;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** One atomic approval writes the revision, audit record and notification/index event intent. */
public final class EditorialService {
    public interface Store {
        RevisionPolicy.Approval approve(String actorId, String commandKey, String activityId,
                long expectedVersion, Map<String, RevisionPolicy.Field> proposed, String reason,
                java.util.function.Function<RevisionPolicy.Snapshot, RevisionPolicy.Approval> decision);
    }
    private final Store store;
    private final Clock clock;
    private final RevisionPolicy policy = new RevisionPolicy();
    public EditorialService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store); this.clock = Objects.requireNonNull(clock);
    }
    public RevisionPolicy.Approval approve(Actor actor, String commandKey, String activityId,
            long expectedVersion, Map<String, RevisionPolicy.Field> proposed, String reason) {
        actor.editorial();
        var changes = Map.copyOf(proposed);
        String target = text(activityId, "activityId"), auditReason = text(reason, "reason");
        String key = text(commandKey, "commandKey");
        require(key.length() <= 128 && auditReason.length() <= 2000, Code.INVALID_INPUT, "Input too long");
        return store.approve(actor.userId(), key, target, expectedVersion, changes, auditReason, current -> {
            require(current.activityId().equals(target), Code.INVALID_INPUT, "Wrong activity snapshot");
            return policy.approve(actor, current, expectedVersion, changes, auditReason, clock.instant());
        });
    }
}

