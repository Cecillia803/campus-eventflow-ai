package campus.eventflow.registration;
import java.time.Instant;
import java.util.*;
import static campus.eventflow.contracts.Failure.*;

/**
 * Pure per-session transitions. A storage adapter must execute these under an atomic
 * session boundary. This immutable snapshot is not a scalable database implementation.
 */
public final class RegistrationPolicy {
    public enum Status { CONFIRMED, WAITING, CANCELLED, CLOSED }
    public enum EventType { CONFIRMED, WAITLISTED, CANCELLED, PROMOTED, WAITLIST_CLOSED }
    public record Entry(String userId, Status status, long sequence) {
        public Entry {
            userId = text(userId, "userId"); Objects.requireNonNull(status);
            require(sequence > 0, Code.INVALID_INPUT, "Invalid queue sequence");
        }
    }
    public record Session(String id, long version, int capacity, Instant opensAt, Instant closesAt,
                          Instant cancellationDeadline, Instant promotionDeadline,
                          boolean cancelled, long lastSequence, Map<String, Entry> entries) {
        public Session {
            id = text(id, "sessionId");
            Objects.requireNonNull(opensAt); Objects.requireNonNull(closesAt);
            Objects.requireNonNull(cancellationDeadline); Objects.requireNonNull(promotionDeadline);
            require(version >= 0 && capacity > 0 && lastSequence >= 0, Code.INVALID_INPUT, "Invalid session");
            require(opensAt.isBefore(closesAt) && !promotionDeadline.isBefore(closesAt)
                    && !cancellationDeadline.isBefore(opensAt), Code.INVALID_INPUT, "Invalid deadlines");
            entries = Map.copyOf(entries);
            Set<Long> sequences = new HashSet<>();
            for (var item : entries.entrySet()) {
                Entry entry = item.getValue();
                require(item.getKey().equals(entry.userId()) && entry.sequence() <= lastSequence
                        && sequences.add(entry.sequence()), Code.INVALID_INPUT, "Inconsistent participant snapshot");
            }
            require(entries.values().stream().filter(e -> e.status() == Status.CONFIRMED).count() <= capacity,
                    Code.INVALID_INPUT, "Over-capacity snapshot");
            require(!cancelled || entries.values().stream().noneMatch(e -> e.status() == Status.CONFIRMED || e.status() == Status.WAITING),
                    Code.INVALID_INPUT, "Cancelled session still has active participants");
        }
    }
    public record Event(EventType type, String userId) {}
    public record Change(Session next, List<Event> events, Optional<Status> result) {
        public Change { events = List.copyOf(events); Objects.requireNonNull(result); }
    }

    public Change register(Session s, String userId, boolean eligible, Instant now) {
        userId = text(userId, "userId"); Objects.requireNonNull(now);
        require(!s.cancelled(), Code.CANCELLED, "Session cancelled");
        require(!now.isBefore(s.opensAt()), Code.NOT_OPEN, "Registration not open");
        require(now.isBefore(s.closesAt()), Code.CLOSED, "Registration closed");
        require(eligible, Code.FORBIDDEN, "Eligibility check failed");
        Entry old = s.entries().get(userId);
        require(old == null || old.status() == Status.CANCELLED || old.status() == Status.CLOSED,
                Code.ALREADY_REGISTERED, "An active registration already exists");
        long used = count(s.entries(), Status.CONFIRMED);
        // Existing waiters must be processed first, even when there is free capacity.
        Status status = used < s.capacity() && count(s.entries(), Status.WAITING) == 0
                ? Status.CONFIRMED : Status.WAITING;
        long sequence = Math.incrementExact(s.lastSequence());
        Map<String, Entry> entries = new HashMap<>(s.entries());
        entries.put(userId, new Entry(userId, status, sequence));
        return new Change(next(s, entries, sequence),
                List.of(new Event(status == Status.CONFIRMED ? EventType.CONFIRMED : EventType.WAITLISTED, userId)), Optional.of(status));
    }

    public Change cancel(Session s, String userId, long expectedSequence, Instant now) {
        Objects.requireNonNull(now); userId = text(userId, "userId");
        Entry old = s.entries().get(userId);
        require(old != null, Code.NOT_FOUND, "Registration not found");
        require(old.sequence() == expectedSequence, Code.VERSION_CONFLICT, "Registration changed; reload it");
        if (old.status() == Status.CANCELLED || old.status() == Status.CLOSED)
            return new Change(s, List.of(), Optional.of(old.status()));
        // Waiting users can always leave the queue; occupied places have a cancellation deadline.
        if (old.status() == Status.CONFIRMED && !s.cancelled())
            require(now.isBefore(s.cancellationDeadline()), Code.CLOSED, "Cancellation deadline passed");
        Map<String, Entry> entries = new HashMap<>(s.entries());
        entries.put(userId, new Entry(userId, Status.CANCELLED, old.sequence()));
        List<Event> events = new ArrayList<>();
        events.add(new Event(EventType.CANCELLED, userId));
        settle(s, entries, now, events);
        return new Change(next(s, entries, s.lastSequence()), events, Optional.of(Status.CANCELLED));
    }

    /** Scheduled drain/close; callers must persist it atomically and repeat safely. */
    public Change reconcile(Session s, Instant now) {
        Map<String, Entry> entries = new HashMap<>(s.entries());
        List<Event> events = new ArrayList<>();
        settle(s, entries, Objects.requireNonNull(now), events);
        if (events.isEmpty()) return new Change(s, events, Optional.empty());
        return new Change(next(s, entries, s.lastSequence()), events, Optional.empty());
    }

    /** Caller must authenticate an administrator and persist cancellation + notices atomically. */
    public Change cancelSession(Session s) {
        if (s.cancelled()) return new Change(s, List.of(), Optional.empty());
        Map<String, Entry> entries = new HashMap<>(s.entries());
        List<Event> events = new ArrayList<>();
        for (Entry e : s.entries().values().stream().sorted(Comparator.comparingLong(Entry::sequence)).toList()) {
            if (e.status() == Status.CONFIRMED || e.status() == Status.WAITING) {
                boolean waiting = e.status() == Status.WAITING;
                entries.put(e.userId(), new Entry(e.userId(), waiting ? Status.CLOSED : Status.CANCELLED, e.sequence()));
                events.add(new Event(waiting ? EventType.WAITLIST_CLOSED : EventType.CANCELLED, e.userId()));
            }
        }
        Session next = new Session(s.id(), Math.incrementExact(s.version()), s.capacity(), s.opensAt(), s.closesAt(),
                s.cancellationDeadline(), s.promotionDeadline(), true, s.lastSequence(), entries);
        return new Change(next, events, Optional.empty());
    }

    private void settle(Session s, Map<String, Entry> entries, Instant now, List<Event> events) {
        List<Entry> waiting = entries.values().stream().filter(e -> e.status() == Status.WAITING)
                .sorted(Comparator.comparingLong(Entry::sequence)).toList();
        if (s.cancelled() || !now.isBefore(s.promotionDeadline())) {
            for (Entry e : waiting) {
                entries.put(e.userId(), new Entry(e.userId(), Status.CLOSED, e.sequence()));
                events.add(new Event(EventType.WAITLIST_CLOSED, e.userId()));
            }
            return;
        }
        long free = s.capacity() - count(entries, Status.CONFIRMED);
        for (Entry e : waiting) {
            if (free-- <= 0) break;
            entries.put(e.userId(), new Entry(e.userId(), Status.CONFIRMED, e.sequence()));
            events.add(new Event(EventType.PROMOTED, e.userId()));
        }
    }
    private long count(Map<String, Entry> entries, Status status) {
        return entries.values().stream().filter(e -> e.status() == status).count();
    }
    private Session next(Session s, Map<String, Entry> entries, long sequence) {
        return new Session(s.id(), Math.incrementExact(s.version()), s.capacity(), s.opensAt(), s.closesAt(),
                s.cancellationDeadline(), s.promotionDeadline(), s.cancelled(), sequence, entries);
    }
}
