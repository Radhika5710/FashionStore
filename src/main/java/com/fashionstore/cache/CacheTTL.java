package com.fashionstore.cache;

import java.util.concurrent.TimeUnit;

public class CacheTTL {
    public static final long SHORT = TimeUnit.MINUTES.toMinutes(5);
    public static final long MEDIUM = TimeUnit.HOURS.toHours(1);
    public static final long LONG = TimeUnit.HOURS.toHours(6);
    public static final long VERY_LONG = TimeUnit.HOURS.toHours(24);
    
    private CacheTTL() {}
}
