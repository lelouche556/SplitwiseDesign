package service;

import model.User;

import java.util.HashMap;
import java.util.Map;

/**
 Service class for managing users.
 */
public class UserService {
    private final Map<String, User> users;

    public UserService() {
        this.users = new HashMap<>();
    }

    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (users.containsKey(user.getUserId())) {
            throw new IllegalArgumentException("User with ID " + user.getUserId() + " already exists");
        }
        users.put(user.getUserId(), user);
    }

    public User getUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return users.get(userId);
    }

    public boolean userExists(String userId) {
        return userId != null && users.containsKey(userId);
    }

    public Map<String, User> getAllUsers() {
        return new HashMap<>(users);
    }
}

