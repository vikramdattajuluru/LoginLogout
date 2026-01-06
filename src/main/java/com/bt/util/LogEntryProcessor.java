package com.bt.util;


import java.time.LocalDateTime;

@FunctionalInterface
public interface LogEntryProcessor {

    void process(String username, LocalDateTime timestamp) throws Exception;

    default void processLogin(String username, LocalDateTime timestamp) throws Exception {
        process(username, timestamp);
    }


    default void processLogout(String username, LocalDateTime timestamp) throws Exception {
        process(username, timestamp);
    }
}