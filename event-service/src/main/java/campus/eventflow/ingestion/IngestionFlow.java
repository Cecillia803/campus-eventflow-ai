package campus.eventflow.ingestion;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/** No network fetching/extraction; retry creates a new attempt, not a published-record rewind. */
public final class IngestionFlow {
    public enum Stage { RECEIVED, NORMALIZED, EXTRACTED, REVIEW_REQUIRED, PUBLISHED, FAILED }
    public static Stage advance(Stage current, Stage next) {
        Objects.requireNonNull(current); Objects.requireNonNull(next);
        boolean allowed = switch (current) {
            case RECEIVED -> next == Stage.NORMALIZED || next == Stage.FAILED;
            case NORMALIZED -> next == Stage.EXTRACTED || next == Stage.FAILED;
            case EXTRACTED -> next == Stage.REVIEW_REQUIRED || next == Stage.FAILED;
            case REVIEW_REQUIRED -> next == Stage.PUBLISHED || next == Stage.FAILED;
            case FAILED, PUBLISHED -> false;
        };
        require(allowed, Code.INVALID_INPUT, "Illegal ingestion transition");
        return next;
    }
    public record SourceDocument(String sourceId, String revision, String body) {
        public SourceDocument {
            sourceId = text(sourceId, "sourceId"); revision = text(revision, "revision");
            body = text(body, "body");
        }
    }
    public record Candidate(String title, Map<String, RevisionPolicy.Field> fields, List<String> warnings) {
        public Candidate { title = text(title, "title"); fields = Map.copyOf(fields); warnings = List.copyOf(warnings); }
    }
    public record SourceBatch(List<SourceDocument> documents, Optional<String> nextCursor) {
        public SourceBatch { documents = List.copyOf(documents); Objects.requireNonNull(nextCursor); }
    }
    public interface SourceReader { SourceBatch read(String authorizedSourceId, Optional<String> cursor); }
    public interface Extractor { List<Candidate> extract(SourceDocument document); }
    public interface DuplicateMatcher { List<String> suggestActivityIds(Candidate candidate); }
}
