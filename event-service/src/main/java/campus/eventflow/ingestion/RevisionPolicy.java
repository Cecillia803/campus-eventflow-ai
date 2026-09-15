package campus.eventflow.ingestion;
import campus.eventflow.contracts.Actor;
import java.time.Instant;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Pure diff/approval. Persistence must compare-and-swap expectedVersion. */
public final class RevisionPolicy {
    private static final Set<String> EDITABLE = Set.of("title", "summary", "content", "campus", "category", "organizer",
            "eventTime", "registrationTime", "eligibility", "registrationInstructions", "notice");
    public record Evidence(String sourceId, String sourceRevision, String locator) {
        public Evidence {
            sourceId = text(sourceId, "sourceId"); sourceRevision = text(sourceRevision, "sourceRevision");
            locator = text(locator, "locator");
        }
    }
    public record Field(String value, Evidence evidence, boolean manuallyConfirmed) {
        public Field { value = text(value, "value"); Objects.requireNonNull(evidence); }
    }
    public record Snapshot(String activityId, long version, Map<String, Field> fields) {
        public Snapshot {
            activityId = text(activityId, "activityId");
            require(version >= 0, Code.INVALID_INPUT, "Invalid version");
            fields = Map.copyOf(fields);
        }
    }
    public record Review(Set<String> changedFields, Set<String> protectedFields, boolean reviewRequired) {
        public Review { changedFields = Set.copyOf(changedFields); protectedFields = Set.copyOf(protectedFields); }
    }
    public record Approval(Snapshot next, String reviewerId, String reason, Instant reviewedAt) {}
    public Review inspect(Snapshot current, Map<String, Field> proposed) {
        Set<String> changed = new TreeSet<>(), protectedFields = new TreeSet<>();
        proposed.forEach((key, field) -> {
            text(key, "field"); Objects.requireNonNull(field);
            require(EDITABLE.contains(key), Code.INVALID_INPUT, "Unknown editorial field");
            Field old = current.fields().get(key);
            if (!Objects.equals(old, field)) {
                changed.add(key);
                if (old != null && old.manuallyConfirmed()) protectedFields.add(key);
            }
        });
        return new Review(changed, protectedFields, !changed.isEmpty());
    }
    public Approval approve(Actor actor, Snapshot current, long expectedVersion,
                            Map<String, Field> proposed, String reason, Instant now) {
        actor.editorial();
        require(current.version() == expectedVersion, Code.VERSION_CONFLICT, "Review is stale");
        reason = text(reason, "reason"); Objects.requireNonNull(now);
        require(!inspect(current, proposed).changedFields().isEmpty(), Code.INVALID_INPUT, "No changes");
        Map<String, Field> merged = new HashMap<>(current.fields());
        proposed.forEach((key, field) -> merged.put(key, new Field(field.value(), field.evidence(), true)));
        return new Approval(new Snapshot(current.activityId(), Math.incrementExact(current.version()), merged),
                actor.userId(), reason, now);
    }
}
