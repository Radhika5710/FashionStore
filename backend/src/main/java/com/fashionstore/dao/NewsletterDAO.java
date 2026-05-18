package com.fashionstore.dao;

import com.fashionstore.model.Newsletter;
import java.util.List;

public interface NewsletterDAO {
    boolean subscribe(String email);
    boolean unsubscribe(String email);
    boolean isSubscribed(String email);
    List<Newsletter> getAllSubscribers();
    List<Newsletter> getActiveSubscribers();
}
