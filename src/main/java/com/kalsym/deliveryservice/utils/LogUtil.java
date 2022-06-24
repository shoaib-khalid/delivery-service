package com.kalsym.deliveryservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sarosh
 */
public class LogUtil {

    private static final org.slf4j.Logger application = LoggerFactory.getLogger("application");
    public static final org.slf4j.Logger cdr = LoggerFactory.getLogger("cdr");

    public static void info(String prefix, String location, String message, String postfix) {
        application.info(prefix + " " + location + " " + message + " " + postfix + " ");
    }

    public static void warn(String prefix, String location, String message, String postfix) {
        application.warn(prefix + " " + location + " " + message + " " + postfix + " ");
    }

    public static void error(String prefix, String location, String message, String postfix, Exception e) {
        application.error(prefix + " " + location + " " + message + " " + postfix, e);
    }
}
