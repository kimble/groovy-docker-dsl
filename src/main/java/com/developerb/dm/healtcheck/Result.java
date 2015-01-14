package com.developerb.dm.healtcheck;

import org.apache.commons.lang3.StringUtils;

/**
 */
public class Result {

    private final boolean healthy;
    private final String message;
    private final Throwable trouble;

    private Result(boolean healthy, String message, Throwable trouble) {
        if (message != null) {
            message = message.replace("\n", "").trim();
            message = StringUtils.abbreviate(message, 80);
        }

        this.healthy = healthy;
        this.message = message;
        this.trouble = trouble;
    }

    public static Result unhealthy() {
        return new Result(false, null, null);
    }

    public static Result unhealthy(String message) {
        return new Result(false, message, null);
    }

    public static Result unhealthy(Throwable trouble) {
        return new Result(false, trouble.getMessage(), trouble);
    }

    public static Result healthy() {
        return new Result(true, null, null);
    }

    public static Result healthy(String message) {
        return new Result(true, message, null);
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        if (healthy) {
            return message != null ? "Healthy, " + message : "Healthy, no message";
        }
        else {
            return message != null ? "Un-healthy, " + message : "Un-healthy, no message";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        if (healthy != result.healthy) return false;
        if (message != null ? !message.equals(result.message) : result.message != null) return false;
        if (trouble != null ? !trouble.equals(result.trouble) : result.trouble != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (healthy ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (trouble != null ? trouble.hashCode() : 0);
        return result;
    }

}
