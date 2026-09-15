package campus.eventflow.agent;
import campus.eventflow.contracts.Actor;
import java.time.*;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Only read tools are reachable from model planning. No enrollment or subscription execution. */
public final class ReadOnlyAssistant {
    public enum State { ANSWERED, NEEDS_CLARIFICATION, INSUFFICIENT_EVIDENCE }
    public record Reply(State state, String text, List<AgentPorts.Evidence> citations, String coverage) {
        public Reply { citations = List.copyOf(citations); }
    }
    private final AgentPorts.Planner planner;
    private final AgentPorts.ActivityTools activities;
    private final AgentPorts.Retriever retriever;
    private final AgentPorts.Model model;
    private final AgentPorts.Conversations conversations;
    private final Clock clock;
    private final EvidenceGuard guard = new EvidenceGuard();
    public ReadOnlyAssistant(AgentPorts.Planner planner, AgentPorts.ActivityTools activities,
                             AgentPorts.Retriever retriever, AgentPorts.Model model,
                             AgentPorts.Conversations conversations, Clock clock) {
        this.planner = Objects.requireNonNull(planner); this.activities = Objects.requireNonNull(activities);
        this.retriever = Objects.requireNonNull(retriever); this.model = Objects.requireNonNull(model);
        this.conversations = Objects.requireNonNull(conversations); this.clock = Objects.requireNonNull(clock);
    }
    public Reply ask(Actor actor, String conversationId, long expectedVersion, String messageId, String question, ZoneId zone) {
        question = text(question, "question"); messageId = text(messageId, "messageId");
        require(question.length() <= 4000 && messageId.length() <= 128, Code.INVALID_INPUT, "Input too long");
        Objects.requireNonNull(zone);
        var conversation = conversations.loadOwned(actor, text(conversationId, "conversationId"));
        actor.owns(conversation.ownerId());
        var previous = conversations.findOwnedExchange(actor, conversation.id(), messageId);
        if (previous.isPresent()) {
            var saved = previous.get();
            require(question.equals(saved.question()) && zone.equals(saved.zone()), Code.IDEMPOTENCY_CONFLICT, "Message ID reused with different content");
            return saved.reply();
        }
        require(conversation.version() == expectedVersion, Code.VERSION_CONFLICT, "Conversation changed; refresh first");
        var plan = planner.plan(question, conversation.messages(), zone, clock.instant());
        // The timezone comes from caller context, not an unchecked model override.
        require(zone.equals(plan.query().zone()), Code.INVALID_INPUT, "Planner changed timezone");
        if (plan.intent() == AgentPorts.Intent.CLARIFY) {
            var answer = new AgentPorts.Answer(plan.clarification().orElseThrow(), List.of());
            return conversations.appendExchange(actor, conversation.id(), conversation.version(), messageId, question, zone,
                    new Reply(State.NEEDS_CLARIFICATION, answer.text(), List.of(), ""));
        }
        AgentPorts.Context context = switch (plan.intent()) {
            case FIND_ACTIVITIES -> activities.search(plan.query());
            case MY_REGISTRATIONS -> activities.ownRegistrations(actor);
            case EXPLAIN_ACTIVITY -> retriever.retrieve(actor, plan.query());
            case CLARIFY -> throw new IllegalStateException("Handled above");
        };
        if (context.evidence().isEmpty()) {
            var answer = new AgentPorts.Answer("在已收录范围内没有足够依据，请调整条件或查看主办方原始通知。", List.of());
            return conversations.appendExchange(actor, conversation.id(), conversation.version(), messageId, question, zone,
                    new Reply(State.INSUFFICIENT_EVIDENCE, answer.text(), List.of(), context.coverage()));
        }
        var answer = guard.validate(model.answer(question, conversation.messages(), context), context);
        var citations = context.evidence().stream().filter(e -> answer.evidenceIds().contains(e.id())).toList();
        return conversations.appendExchange(actor, conversation.id(), conversation.version(), messageId, question, zone,
                new Reply(State.ANSWERED, answer.text(), citations, context.coverage()));
    }
}
