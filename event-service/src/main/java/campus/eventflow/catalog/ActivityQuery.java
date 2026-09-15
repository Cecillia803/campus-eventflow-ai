package campus.eventflow.catalog;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import static campus.eventflow.contracts.Failure.*;

/** Implementations must apply stable pagination and accurate coverage metadata. */
public interface ActivityQuery {
    enum DateField { EVENT_START, REGISTRATION_END }
    record Filter(String text, Optional<String> campus, Optional<String> category,
                  Optional<LocalDate> from, Optional<LocalDate> untilExclusive,
                  ZoneId zone, DateField dateField, boolean registrationOpenOnly,
                  Optional<String> cursor, int limit) {
        public Filter {
            text = text == null ? "" : text.strip();
            require(text.length() <= 300 && limit > 0 && limit <= 100,
                    Code.INVALID_INPUT, "Invalid query bounds");
            java.util.Objects.requireNonNull(campus); java.util.Objects.requireNonNull(category);
            java.util.Objects.requireNonNull(from); java.util.Objects.requireNonNull(untilExclusive);
            java.util.Objects.requireNonNull(zone); java.util.Objects.requireNonNull(dateField);
            java.util.Objects.requireNonNull(cursor);
            if (from.isPresent() && untilExclusive.isPresent())
                require(from.get().isBefore(untilExclusive.get()), Code.INVALID_INPUT, "Invalid date range");
        }
    }
    record Item(String id, String title, long version, String campus, String category,
                ActivityTime.Window eventWindow, ActivityTime.Window registrationWindow,
                ActivityTime.Notice notice) {}
    record Page(List<Item> items, Optional<String> nextCursor, String coverageDescription) {
        public Page { items = List.copyOf(items); }
    }
    Page search(Filter filter);
    Optional<Item> find(String activityId);
}

