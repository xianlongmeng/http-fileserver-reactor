package com.ly.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.util.StringUtils;

public class ConvertUtil {

    public static int parseInt(String s, int defaultValue) {
        if (StringUtils.isEmpty(s))
            return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long parseLong(String s, long defaultValue) {
        if (StringUtils.isEmpty(s))
            return defaultValue;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    public static LocalDateTime toGMTLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }
    public static LocalDateTime toLocalDateTime(long timestamp){
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static long toTimestamp(LocalDateTime localDateTime){
        if (localDateTime==null)
            return 0;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static Instant toInstance(LocalDateTime localDateTime){
        if (localDateTime==null)
            return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
