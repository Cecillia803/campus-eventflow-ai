package campus.eventflow.subscription;
import java.time.Instant;

/**
 * Durable adapter must load current versions, call policy, deduplicate by reminder identity,
 * persist the inbox item and record delivery atomically. No sending is performed by this port.
 */
public interface NotificationDelivery {
    ReminderPolicy.Decision deliver(ReminderPolicy.Reminder reminder, Instant now);
}

