package com.fashionstore.dao;

import com.fashionstore.model.PasswordResetToken;

public interface PasswordResetTokenDAO {
    int createToken(PasswordResetToken token);
    PasswordResetToken getTokenByToken(String token);
    PasswordResetToken getTokenByUserId(int userId);
    boolean markTokenAsUsed(int tokenId);
    boolean invalidateToken(String token);
    boolean deleteExpiredTokens();
    boolean deleteToken(int tokenId);
}
