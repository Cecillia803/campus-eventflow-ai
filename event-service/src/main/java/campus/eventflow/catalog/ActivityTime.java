package campus.eventflow.catalog;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import static campus.eventflow.contracts.Failure.*;

public final class ActivityTime {
    private ActivityTime() {}
    public enum Status { UNKNOWN, UPCOMING, ACTIVE, ENDED, CANCELLED, POSTPONED }
    public enum Notice { NORMAL, CANCELLED, POSTPONED }
    /** Exact instants only; never guess ambiguous date-only source values into this type. */
    public record Window(Optional<Instant> start, Optional<Instant> end) {
        public Window {
            Objects.requireNonNull(start); Objects.requireNonNull(end);
            if (start.isPresent() && end.isPresent())
                require(start.get().isBefore(end.get()), Code.INVALID_INPUT, "Window must have positive duration");
        }
        public boolean fullyKnown() { return start.isPresent() && end.isPresent(); }
        public boolean contains(Instant now) {
            return fullyKnown() && !now.isBefore(start.get()) && now.isBefore(end.get());
        }
    }
    public static Status status(Window window, Instant now, Notice notice) {
        Objects.requireNonNull(window); Objects.requireNonNull(now); Objects.requireNonNull(notice);
        if (notice == Notice.CANCELLED) return Status.CANCELLED;
        if (notice == Notice.POSTPONED) return Status.POSTPONED;
        if (window.end().isPresent() && !now.isBefore(window.end().get())) return Status.ENDED;
        if (window.start().isPresent() && now.isBefore(window.start().get())) return Status.UPCOMING;
        return window.fullyKnown() ? Status.ACTIVE : Status.UNKNOWN;
    }
}

