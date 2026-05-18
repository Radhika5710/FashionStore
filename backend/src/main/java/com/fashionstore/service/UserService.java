package com.fashionstore.service;

import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAOImpl();
    }

    public int registerUser(User user) {
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        
        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "customer";
        }
        user.setRole(role);
        
        return userDAO.registerUser(user);
    }

    public User validateAndLoginUser(String email, String password) throws IllegalArgumentException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        User user = userDAO.getUserByEmail(email);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    public User getUserByEmail(String email) {
        return userDAO.getUserByEmail(email);
    }

    public boolean isEmailExists(String email) {
        return userDAO.isEmailExists(email);
    }

    public boolean updateUser(User user) {
        return userDAO.updateUser(user);
    }

    public boolean changePassword(int userId, String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        return userDAO.changePassword(userId, hashedPassword);
    }

    public int getTotalUserCount() {
        return userDAO.getTotalUserCount();
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public boolean updateUserRole(int userId, String role) {
        return userDAO.updateUserRole(userId, role);
    }
}
