package factory;

import model.User;

/**
 * Factory class for creating User instances.
 */
public class UserFactory {
    public static User createUser(String userId, String name, String email, String mobileNumber) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return new User(userId, name, email, mobileNumber);
    }
}

