package com.simsilver.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 *
 */

public class Common {
    public static final String LOCAL_MESSAGE_ACTION = "LOCAL_MESSAGE_ACTION";
    public static final String LOCAL_MESSAGE_DATA = "LOCAL_MESSAGE_DATA";
    public static final String DATA_TYPE = "DATA_TYPE";
    public static final String BIND_ACTION_MESSENGER = "com.example.services.BIND_ACTION_MESSENGER";
    public static final String BIND_ACTION_LOCAL_BINDER = "com.example.services.BIND_ACTION_LOCAL_BINDER";

    public static void copyAndClose(InputStream in, OutputStream out) throws IOException {
        byte[] cache = new byte[4096];
        int size;
        while ((size = in.read(cache)) > 0) {
            out.write(cache, 0, size);
        }
        in.close();
        out.close();
    }

    public static String format(String s, Object... args) {
        return String.format(Locale.getDefault(), s, args);
    }

    public static String combineListArray(List list) {
        StringBuilder sb = new StringBuilder(4096);
        for (Object obj : list) {
            sb.append(obj);
            sb.append("\n");
        }
        return sb.toString();
    }
}
