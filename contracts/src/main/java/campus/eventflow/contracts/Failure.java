package campus.eventflow.contracts;
import java.util.Objects;

/** Stable business errors. Transport mapping belongs to adapters. */
public final class Failure extends RuntimeException {
    public enum Code {
        INVALID_INPUT, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, NOT_CONNECTED,
        VERSION_CONFLICT, IDEMPOTENCY_CONFLICT, ALREADY_REGISTERED,
        NOT_OPEN, CLOSED, FULL, CANCELLED, EXPIRED, INSUFFICIENT_EVIDENCE,
        DEPENDENCY_UNAVAILABLE
    }
    private final Code code;
    public Failure(Code code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code);
    }
    public Code code() { return code; }
    public static void require(boolean condition, Code code, String message) {
        if (!condition) throw new Failure(code, message);
    }
    public static String text(String value, String field) {
        require(value != null && !value.isBlank(), Code.INVALID_INPUT, field + " is required");
        return value.strip();
    }
}

