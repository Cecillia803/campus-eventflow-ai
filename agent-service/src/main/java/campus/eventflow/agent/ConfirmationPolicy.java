package campus.eventflow.agent;
import campus.eventflow.contracts.Actor;
import java.time.Instant;
import java.util.Objects;
import static campus.eventflow.contracts.Failure.*;

/** A draft is not a reservation. It never holds a place and never permits model-side confirmation. */
public final class ConfirmationPolicy {
    public enum Status { PENDING, SUBMITTED, REVOKED }
    public record Draft(String id, String ownerId, String sessionId, long ruleVersion,
                        Instant expiresAt, Status status) {
        public Draft {
            id = text(id, "draftId"); ownerId = text(ownerId, "ownerId");
            sessionId = text(sessionId, "sessionId"); Objects.requireNonNull(expiresAt);
            Objects.requireNonNull(status); require(ruleVersion >= 0, Code.INVALID_INPUT, "Invalid rule version");
        }
    }
    public enum Decision { SUBMIT_ONCE, RETURN_ORIGINAL_RESULT }
    public Decision inspect(Actor actor, Draft draft, long currentRuleVersion, Instant now) {
        actor.owns(draft.ownerId()); Objects.requireNonNull(now);
        if (draft.status() == Status.SUBMITTED) return Decision.RETURN_ORIGINAL_RESULT;
        require(draft.status() == Status.PENDING, Code.CANCELLED, "Draft revoked");
        require(now.isBefore(draft.expiresAt()), Code.EXPIRED, "Draft expired");
        require(draft.ruleVersion() == currentRuleVersion, Code.VERSION_CONFLICT, "Rules changed; reconfirm");
        return Decision.SUBMIT_ONCE;
    }
    /**
     * Must use a deterministic enrollment command identity derived from owner + draftId,
     * replay original result, and durably coordinate draft state with registration submission.
     * Checking the policy then independently writing two services is NOT an implementation.
     */
    public interface Executor {
        record Result(String requestId, String status) {}
        Result confirmOwned(Actor actor, String draftId);
    }
}

