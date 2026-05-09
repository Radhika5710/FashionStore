package com.fashionstore.controller;

import com.fashionstore.dao.PasswordResetTokenDAO;
import com.fashionstore.daoimpl.PasswordResetTokenDAOImpl;
import com.fashionstore.model.PasswordResetToken;
import com.fashionstore.model.User;
import com.fashionstore.service.EmailService;
import com.fashionstore.service.UserService;
import com.fashionstore.util.AuditLogger;
import com.fashionstore.util.NullSafetyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@WebServlet({"/forgot-password", "/reset-password"})
public class PasswordResetController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 1;

    private UserService userService;
    private PasswordResetTokenDAO tokenDAO;

    @Override
    public void init() {
        userService = new UserService();
        tokenDAO = new PasswordResetTokenDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/forgot-password".equals(path)) {
            request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
        } else if ("/reset-password".equals(path)) {
            String token = request.getParameter("token");
            
            if (token == null || token.isBlank()) {
                request.setAttribute("error", "Invalid reset link. Please request a new password reset.");
                request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
                return;
            }

            PasswordResetToken resetToken = tokenDAO.getTokenByToken(token);
            
            if (resetToken == null || !resetToken.isValid()) {
                request.setAttribute("error", "Invalid or expired reset link. Please request a new password reset.");
                request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
                return;
            }

            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/forgot-password".equals(path)) {
            handleForgotPassword(request, response);
        } else if ("/reset-password".equals(path)) {
            handleResetPassword(request, response);
        }
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = NullSafetyUtil.safeString(request.getParameter("email"), null);

        if (NullSafetyUtil.isNullOrEmpty(email)) {
            request.setAttribute("error", "Email address is required");
            request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
            return;
        }

        User user = userService.getUserByEmail(email);

        if (user == null) {
            request.setAttribute("error", "No account found with this email address");
            request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
            return;
        }

        try {
            tokenDAO.deleteExpiredTokens();

            PasswordResetToken existingToken = tokenDAO.getTokenByUserId(user.getUserId());
            if (existingToken != null) {
                tokenDAO.invalidateToken(existingToken.getToken());
            }

            String tokenString = generateSecureToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

            PasswordResetToken token = new PasswordResetToken(user.getUserId(), tokenString, expiresAt);
            int tokenId = tokenDAO.createToken(token);

            if (tokenId > 0) {
                String resetLink = request.getRequestURL().toString().replace("forgot-password", "reset-password?token=" + tokenString);
                
                logger.info("Password reset link generated for user {}: {}", user.getEmail(), resetLink);
                AuditLogger.log("PASSWORD_RESET_REQUESTED", "Password reset requested for user: " + user.getEmail(), 
                               String.valueOf(user.getUserId()), request);

                // Send email with reset link
                EmailService.getInstance().sendPasswordResetEmail(user.getEmail(), resetLink, user.getFullName());

                request.setAttribute("success", "Password reset link has been sent to your email. The link will expire in 1 hour.");
                request.setAttribute("resetLink", resetLink);
            } else {
                request.setAttribute("error", "Failed to generate reset link. Please try again.");
            }

        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage(), e);
            request.setAttribute("error", "An error occurred. Please try again.");
        }

        request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = NullSafetyUtil.safeString(request.getParameter("token"), null);
        String password = NullSafetyUtil.safeString(request.getParameter("password"), null);
        String confirmPassword = NullSafetyUtil.safeString(request.getParameter("confirmPassword"), null);

        if (NullSafetyUtil.isNullOrEmpty(token)) {
            request.setAttribute("error", "Invalid reset link");
            request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
            return;
        }

        if (NullSafetyUtil.isNullOrEmpty(password)) {
            request.setAttribute("error", "Password is required");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        if (!NullSafetyUtil.safeEquals(password, confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        if (NullSafetyUtil.safeLength(password) < 8) {
            request.setAttribute("error", "Password must be at least 8 characters long");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        try {
            PasswordResetToken resetToken = tokenDAO.getTokenByToken(token);

            if (resetToken == null || !resetToken.isValid()) {
                request.setAttribute("error", "Invalid or expired reset link. Please request a new password reset.");
                request.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(request, response);
                return;
            }

            boolean passwordUpdated = userService.changePassword(resetToken.getUserId(), password);

            if (passwordUpdated) {
                tokenDAO.markTokenAsUsed(resetToken.getTokenId());
                
                logger.info("Password reset successful for user ID: {}", resetToken.getUserId());
                AuditLogger.log("PASSWORD_RESET_SUCCESS", "Password reset successful for user ID: " + resetToken.getUserId(), 
                               String.valueOf(resetToken.getUserId()), request);

                response.sendRedirect(request.getContextPath() + "/login?reset=success");
                return;
            } else {
                request.setAttribute("error", "Failed to update password. Please try again.");
                request.setAttribute("token", token);
                request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            }

        } catch (Exception e) {
            logger.error("Error in reset password: {}", e.getMessage(), e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
