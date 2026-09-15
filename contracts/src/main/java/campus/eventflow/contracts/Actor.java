package campus.eventflow.contracts;
import java.util.Set;
import static campus.eventflow.contracts.Failure.*;

/** Construct only from trusted authentication, never from model or request userId. */
public record Actor(String userId, Set<Role> roles) {
    public enum Role { STUDENT, EDITOR, ADMIN }
    public Actor {
        userId = text(userId, "userId");
        roles = Set.copyOf(roles);
    }
    public void owns(String owner) {
        require(userId.equals(owner), Code.FORBIDDEN, "Resource belongs to another user");
    }
    public void editorial() {
        require(roles.contains(Role.EDITOR) || roles.contains(Role.ADMIN),
                Code.FORBIDDEN, "Editorial permission required");
    }
}

