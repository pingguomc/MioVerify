package org.miowing.mioverify.exception;

/**
 * 请求频率超限异常
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final int retryAfterSeconds;
    
    public RateLimitExceededException() {
        super("Too many requests. Please try again later.");
        this.retryAfterSeconds = 60;
    }
    
    public RateLimitExceededException(int retryAfterSeconds) {
        super("Too many requests. Please try again after " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
