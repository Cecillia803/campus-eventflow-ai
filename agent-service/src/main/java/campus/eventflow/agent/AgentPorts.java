package campus.eventflow.agent;
import campus.eventflow.contracts.Actor;
import java.time.*;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Provider-independent ports. Never expose SQL, credentials or arbitrary URLs as model tools. */
public final class AgentPorts {
    private AgentPorts() {}
    public enum Intent { FIND_ACTIVITIES, EXPLAIN_ACTIVITY, MY_REGISTRATIONS, CLARIFY }
    public record Query(String text, Optional<LocalDate> from, Optional<LocalDate> untilExclusive,
                        ZoneId zone, Optional<String> campus, Optional<String> category,
                        Optional<String> activityId) {
        public Query {
            text = campus.eventflow.contracts.Failure.text(text, "query");
            require(text.length() <= 2000, Code.INVALID_INPUT, "Query too long");
            Objects.requireNonNull(from); Objects.requireNonNull(untilExclusive); Objects.requireNonNull(zone);
            Objects.requireNonNull(campus); Objects.requireNonNull(category); Objects.requireNonNull(activityId);
            if (from.isPresent() && untilExclusive.isPresent())
                require(from.get().isBefore(untilExclusive.get()), Code.INVALID_INPUT, "Invalid dates");
        }
    }
    public record Plan(Intent intent, Query query, Optional<String> clarification) {
        public Plan {
            Objects.requireNonNull(intent); Objects.requireNonNull(query); Objects.requireNonNull(clarification);
            if (intent == Intent.CLARIFY) require(clarification.filter(s -> !s.isBlank()).isPresent(),
                    Code.INVALID_INPUT, "Clarification required");
            if (intent == Intent.EXPLAIN_ACTIVITY)
                require(query.activityId().filter(s -> !s.isBlank()).isPresent(),
                        Code.INVALID_INPUT, "An explicit activity is required");
        }
    }
    public record Evidence(String id, String activityId, long revision, String sourceLabel,
                           String locator, String content) {
        public Evidence {
            id = text(id, "evidenceId"); activityId = text(activityId, "activityId");
            require(revision >= 0, Code.INVALID_INPUT, "Invalid revision");
            sourceLabel = text(sourceLabel, "sourceLabel"); locator = text(locator, "locator");
            content = text(content, "content");
        }
    }
    public record Context(List<Evidence> evidence, String coverage) {
        public Context { evidence = List.copyOf(evidence); coverage = text(coverage, "coverage"); }
    }
    public record Answer(String text, List<String> evidenceIds) {
        public Answer { text = campus.eventflow.contracts.Failure.text(text, "answer"); evidenceIds = List.copyOf(evidenceIds); }
    }
    public record Message(String role, String text) {}
    public record Conversation(String id, String ownerId, long version, List<Message> messages) {
        public Conversation {
            id = text(id, "conversationId"); ownerId = text(ownerId, "ownerId");
            require(version >= 0, Code.INVALID_INPUT, "Invalid conversation version");
            messages = List.copyOf(messages);
        }
    }
    public interface Planner {
        Plan plan(String userText, List<Message> ownedHistory, ZoneId zone, Instant now);
    }
    public interface ActivityTools {
        Context search(Query query);
        Context ownRegistrations(Actor actor);
    }
    public interface Retriever {
        /** Must constrain school/visibility/activity and return only authorized source versions. */
        Context retrieve(Actor actor, Query query);
    }
    public interface Model {
        /** Context content is untrusted material, not instructions; results must be plain text. */
        Answer answer(String question, List<Message> ownedHistory, Context context);
    }
    public interface Conversations {
        Conversation loadOwned(Actor actor, String conversationId);
        record Exchange(String question, ZoneId zone, ReadOnlyAssistant.Reply reply) {}
        Optional<Exchange> findOwnedExchange(Actor actor, String conversationId, String messageId);
        /**
         * Must be owner-scoped, atomic, idempotent by messageId and compare expectedVersion.
         * An identical replay returns its stored exchange; changed payload must conflict.
         */
        ReadOnlyAssistant.Reply appendExchange(Actor actor, String conversationId, long expectedVersion,
                            String messageId, String question, ZoneId zone, ReadOnlyAssistant.Reply reply);
    }
}
