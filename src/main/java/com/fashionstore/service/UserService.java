package com.fashionstore.service;

import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.User;

import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAOImpl();
    }

    public int registerUser(User user) {
        return userDAO.registerUser(user);
    }

    public User loginUser(String email, String password) {
        return userDAO.loginUser(email, password);
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
        return userDAO.changePassword(userId, newPassword);
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
