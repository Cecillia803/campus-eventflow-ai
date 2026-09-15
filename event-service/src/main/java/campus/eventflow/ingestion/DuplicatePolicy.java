package campus.eventflow.ingestion;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** Conservative matching signals; no tuned similarity score or automatic cross-source merge. */
public final class DuplicatePolicy {
    public enum Decision { SAME_SOURCE_REVISION, SAME_SOURCE_UPDATED, POSSIBLE_SAME_ACTIVITY, DISTINCT_OCCURRENCE, NEEDS_REVIEW }
    public record Candidate(String sourceId, String sourceItemId, String sourceRevision, String title,
                            Optional<String> organizer, Optional<Instant> exactStart) {
        public Candidate {
            sourceId = text(sourceId, "sourceId"); sourceItemId = text(sourceItemId, "sourceItemId");
            sourceRevision = text(sourceRevision, "sourceRevision"); title = text(title, "title");
            Objects.requireNonNull(organizer); Objects.requireNonNull(exactStart);
        }
    }
    public Decision compare(Candidate left, Candidate right) {
        if (left.sourceId().equals(right.sourceId()) && left.sourceItemId().equals(right.sourceItemId()))
            return left.sourceRevision().equals(right.sourceRevision()) ? Decision.SAME_SOURCE_REVISION : Decision.SAME_SOURCE_UPDATED;
        if (!normalize(left.title()).equals(normalize(right.title()))) return Decision.NEEDS_REVIEW;
        if (left.exactStart().isPresent() && right.exactStart().isPresent()
                && !left.exactStart().equals(right.exactStart())) return Decision.DISTINCT_OCCURRENCE;
        if (left.organizer().isPresent() && right.organizer().isPresent()
                && normalize(left.organizer().get()).equals(normalize(right.organizer().get())))
            return Decision.POSSIBLE_SAME_ACTIVITY;
        return Decision.NEEDS_REVIEW;
    }
    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}

