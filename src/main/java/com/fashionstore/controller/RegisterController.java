package com.fashionstore.controller;

import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.User;
import com.fashionstore.util.XSSUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterController extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String fullName = trim(req.getParameter("fullName"));
        String email = trim(req.getParameter("email"));
        String phone = trim(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String gender = trim(req.getParameter("gender"));
        String address = trim(req.getParameter("address"));

        // Input validation
        if (fullName.isEmpty() || email.isEmpty() || password == null || password.isBlank()) {
            req.setAttribute("error", "Please fill all required fields.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            req.setAttribute("error", "Invalid email format.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        // Validate password strength
        if (!isStrongPassword(password)) {
            req.setAttribute("error", "Password must be at least 8 characters with uppercase, lowercase, and number.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        // Validate phone format if provided
        if (!phone.isEmpty() && !isValidPhone(phone)) {
            req.setAttribute("error", "Invalid phone number format.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        // Check for XSS in inputs
        if (XSSUtil.containsXSS(fullName) || XSSUtil.containsXSS(address)) {
            req.setAttribute("error", "Invalid input detected.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        if (userDAO.isEmailExists(email)) {
            req.setAttribute("error", "Unable to register with the provided details.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        User newUser = new User();
        newUser.setFullName(XSSUtil.sanitize(fullName));
        newUser.setEmail(XSSUtil.sanitize(email));
        newUser.setPhone(XSSUtil.sanitize(phone));
        newUser.setPassword(password);
        newUser.setGender(XSSUtil.sanitize(gender));
        newUser.setAddress(XSSUtil.sanitize(address));

        int userId = userDAO.registerUser(newUser);
        if (userId <= 0) {
            req.setAttribute("error", "Unable to create account right now. Please try again.");
            preserveInputs(req, fullName, email, phone, gender, address);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            return;
        }

        User createdUser = userDAO.getUserById(userId);
        
        // Session fixation prevention
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        
        HttpSession session = req.getSession(true);
        session.setAttribute("user", createdUser);
        session.setAttribute("message", "Welcome to FashionStore!");
        
        // Generate CSRF token
        String csrfToken = generateCSRFToken();
        session.setAttribute("csrf_token", csrfToken);
        
        resp.sendRedirect(req.getContextPath() + "/home");
    }

    private void preserveInputs(HttpServletRequest req, String fullName, String email, String phone, String gender, String address) {
        req.setAttribute("fullName", fullName);
        req.setAttribute("email", email);
        req.setAttribute("phone", phone);
        req.setAttribute("gender", gender);
        req.setAttribute("address", address);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        return hasUpper && hasLower && hasDigit;
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        return phone.matches("^[6-9]\\d{9}$");
    }

    private String generateCSRFToken() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
