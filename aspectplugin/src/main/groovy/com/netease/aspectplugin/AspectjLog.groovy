package com.netease.aspectplugin

/**
 * Created by bjwangmingxian on 17/6/23.
 */
public class AspectjLog {
    static final LOG_TAG = "AspectPlugin"
    static boolean DEBUG = true

    public static configDebug(boolean debug) {
        DEBUG = debug
    }

    public static i(def logMessage) {
        if (DEBUG) {
            System.out.println "${LOG_TAG}: $logMessage"
        }
    }

    public static i(String tag, def logMessage) {
        if (DEBUG) {
            System.out.println "${tag}: $logMessage"
        }
    }
}
