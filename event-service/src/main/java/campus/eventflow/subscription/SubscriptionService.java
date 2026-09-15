package campus.eventflow.subscription;
import campus.eventflow.contracts.Actor;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Owned subscription changes; no scheduler, database or notification sender is instantiated. */
public final class SubscriptionService {
    public record Criteria(Set<String> campuses, Set<String> categories, Set<String> sources) {
        public Criteria {
            campuses = clean(campuses); categories = clean(categories); sources = clean(sources);
            require(!campuses.isEmpty() || !categories.isEmpty() || !sources.isEmpty(),
                    Code.INVALID_INPUT, "At least one subscription condition is required");
        }
        private static Set<String> clean(Set<String> values) {
            Set<String> cleaned = new TreeSet<>();
            for (String value : values) {
                String field = text(value, "criterion");
                require(field.length() <= 100, Code.INVALID_INPUT, "Criterion too long");
                cleaned.add(field);
            }
            require(cleaned.size() <= 30, Code.INVALID_INPUT, "Too many conditions");
            return Set.copyOf(cleaned);
        }
    }
    public interface Store {
        // Bind actor + key to canonical payload; replay original result. Enforce one active
        // subscription per owner + normalized criteria even across different request keys.
        ReminderPolicy.Subscription addOwned(Actor actor, String key, Criteria criteria);
        ReminderPolicy.Subscription cancelOwned(Actor actor, String key, String subscriptionId,
                long expectedRevision, java.util.function.UnaryOperator<ReminderPolicy.Subscription> change);
    }
    private final Store store;
    public SubscriptionService(Store store) { this.store = Objects.requireNonNull(store); }
    public ReminderPolicy.Subscription add(Actor actor, String key, Criteria criteria) {
        Objects.requireNonNull(actor); Objects.requireNonNull(criteria);
        return store.addOwned(actor, checkedKey(key), criteria);
    }
    public ReminderPolicy.Subscription cancel(Actor actor, String key, String id, long expectedRevision) {
        String target = text(id, "subscriptionId");
        return store.cancelOwned(actor, checkedKey(key), target, expectedRevision, current -> {
            actor.owns(current.userId());
            require(current.id().equals(target), Code.INVALID_INPUT, "Wrong subscription");
            if (!current.active()) return current;
            require(current.revision() == expectedRevision, Code.VERSION_CONFLICT, "Subscription changed");
            return new ReminderPolicy.Subscription(current.id(), current.userId(), false,
                    Math.incrementExact(current.revision()), current.campuses(), current.categories(), current.sources());
        });
    }
    private String checkedKey(String key) {
        key = text(key, "commandKey");
        require(key.length() <= 128, Code.INVALID_INPUT, "Command key too long");
        return key;
    }
}
