package campus.eventflow.subscription;
import java.time.Instant;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Decisions only; the notification adapter must atomically deduplicate and recheck versions. */
public final class ReminderPolicy {
    public enum Kind { DEADLINE, CHANGE }
    public enum Decision { SEND, NOT_DUE, RETIRED, ALREADY_SENT, NEEDS_REVIEW }
    public record Subscription(String id, String userId, boolean active, long revision,
                               Set<String> campuses, Set<String> categories, Set<String> sources) {
        public Subscription {
            id = text(id, "subscriptionId"); userId = text(userId, "userId");
            require(revision >= 0, Code.INVALID_INPUT, "Invalid revision");
            campuses = Set.copyOf(campuses); categories = Set.copyOf(categories); sources = Set.copyOf(sources);
        }
    }
    public record Activity(String id, long revision, String campus, String category, Set<String> sources,
                           boolean cancelled, boolean verifiedFresh, Optional<Instant> deadline) {
        public Activity {
            id = text(id, "activityId"); sources = Set.copyOf(sources); Objects.requireNonNull(deadline);
            require(revision >= 0, Code.INVALID_INPUT, "Invalid activity revision");
        }
    }
    public record Reminder(String subscriptionId, long subscriptionRevision, String activityId,
                           long activityRevision, Kind kind, Instant dueAt) {}
    public boolean matches(Subscription s, Activity a) {
        return s.active() && (s.campuses().isEmpty() || s.campuses().contains(a.campus()))
                && (s.categories().isEmpty() || s.categories().contains(a.category()))
                && (s.sources().isEmpty() || !Collections.disjoint(s.sources(), a.sources()));
    }
    public Decision evaluate(Reminder r, Subscription s, Activity a, Instant now, boolean alreadySent) {
        Objects.requireNonNull(now); Objects.requireNonNull(r.kind()); Objects.requireNonNull(r.dueAt());
        if (!r.subscriptionId().equals(s.id()) || !r.activityId().equals(a.id())
                || r.subscriptionRevision() != s.revision() || r.activityRevision() != a.revision()
                || !matches(s, a)) return Decision.RETIRED;
        if (alreadySent) return Decision.ALREADY_SENT;
        if (!a.verifiedFresh()) return Decision.NEEDS_REVIEW;
        if (r.kind() == Kind.DEADLINE) {
            if (a.cancelled()) return Decision.RETIRED;
            if (a.deadline().isEmpty()) return Decision.NEEDS_REVIEW;
            if (!now.isBefore(a.deadline().get())) return Decision.RETIRED;
        }
        return now.isBefore(r.dueAt()) ? Decision.NOT_DUE : Decision.SEND;
    }
}
