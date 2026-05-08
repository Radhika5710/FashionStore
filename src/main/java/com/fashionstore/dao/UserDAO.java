package com.fashionstore.dao;

import com.fashionstore.model.User;
import java.util.List;

public interface UserDAO {

    int registerUser(User user);

    User loginUser(String email, String password);

    User getUserById(int userId);

    User getUserByEmail(String email);

    boolean isEmailExists(String email);

    boolean updateUser(User user);

    boolean changePassword(int userId, String newPassword);

    int getTotalUserCount();
    List<User> getAllUsers();
    boolean updateUserRole(int userId, String role);
}