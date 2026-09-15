package campus.eventflow.agent;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

public final class EvidenceGuard {
    public AgentPorts.Answer validate(AgentPorts.Answer answer, AgentPorts.Context context) {
        Set<String> available = new HashSet<>();
        for (var item : context.evidence())
            require(available.add(item.id()), Code.INVALID_INPUT, "Duplicate evidence identity");
        require(!answer.evidenceIds().isEmpty(), Code.INSUFFICIENT_EVIDENCE, "Answer needs evidence");
        require(available.containsAll(answer.evidenceIds()), Code.INSUFFICIENT_EVIDENCE, "Invented citation");
        return answer;
    }
    // Identity matching is necessary but does NOT verify semantic truth. Real evaluation is still required.
}

