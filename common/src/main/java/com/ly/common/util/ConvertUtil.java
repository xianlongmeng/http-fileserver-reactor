package com.ly.common.util;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ConvertUtil {
    public static int parseInt(String s,int defaultValue){
        if (StringUtils.isEmpty(s))
            return defaultValue;
        try {
            return Integer.parseInt(s);
        }catch (NumberFormatException e){
            return defaultValue;
        }
    }
    public static LocalDateTime toGMTLocalDateTime(LocalDateTime localDateTime){
        return localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

}
