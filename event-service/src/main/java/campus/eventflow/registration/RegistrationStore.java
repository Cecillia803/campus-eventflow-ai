package campus.eventflow.registration;
import java.util.OptionalLong;

/**
 * Required atomic adapter contract, NOT implemented here.
 * Within one transaction/serialized session boundary:
 * 1. Scope idempotency by actor + key; compare canonical command payload.
 * 2. Replay the committed original result before running eligibility/time checks.
 * 3. Lock/load the session, apply change, persist next state, result and event intents.
 * 4. Failure must commit neither capacity nor result. Same key/different payload conflicts.
 * Event IDs derive from committed session version + event position, not random retries.
 * No side effects inside the callback; an adapter may retry it on a version conflict.
 */
public interface RegistrationStore {
    record Command(String actorId, String key, String sessionId, String operation, OptionalLong targetSequence) {}
    RegistrationPolicy.Change transact(Command command,
            java.util.function.Function<RegistrationPolicy.Session, RegistrationPolicy.Change> change);
}
