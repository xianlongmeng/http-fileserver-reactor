package com.ly.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateFormatUtils {

    public static final Logger logger = LoggerFactory.getLogger(DateFormatUtils.class);
    public static final DateTimeFormatter DateDirFormatter = DateTimeFormatter.ofPattern("yyyy/MMdd");
    public static final DateTimeFormatter SimpleDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter SimpleDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter SimpleDateTimeMillSecondFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter NoDelimiterDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter NoDelimiterDateTimeMillsFormatter = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmssSSS");
    public static final DateTimeFormatter NoDelimiterTimeFormatter = DateTimeFormatter.ofPattern("HHmmssSSS");
    protected static final Map<String, DateTimeFormatter> DATE_TIME_FORMATTER_MAP = new ConcurrentHashMap<>();

    private DateFormatUtils() {
    }

    /**
     * 将日期解析为字符串
     *
     * @param date
     *        日期
     * @param pattern
     *        转换格式
     * @return 日期字符串
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    /**
     * 将字符串解析为日期
     *
     * @param dateTime
     *        日期字符串
     * @param pattern
     *        格式
     * @return 转换后的日期
     * @throws Exception
     */
    public static Date parseDate(String dateTime, String pattern) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            if (StringUtils.isNotBlank(dateTime)) {
                date = dateTimeFormat.parse(dateTime);
            }
        } catch (ParseException e) {
            logger.error(String.format(
                    "Exception happened when try to convert String:%s, pattern is:%s, excepton message is:%s", dateTime,
                    pattern, e.getMessage()));
            logger.error(e.getMessage(), e);
        }
        return date;

    }

    public static String buildDirFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, DateDirFormatter);
    }

    public static String buildSimpleDateFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, SimpleDateFormatter);
    }

    public static String buildSimpleDateFormatter(LocalDate localDate) {
        return buildDateFormatter(localDate, SimpleDateFormatter);
    }

    public static String buildSimpleDateTimeFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, SimpleDateTimeFormatter);
    }

    public static String buildSimpleDateTimeMillSecondFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, SimpleDateTimeMillSecondFormatter);
    }

    public static String buildNoDelimiterDateTimeFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, NoDelimiterDateTimeFormatter);
    }

    public static String buildNoDelimiterDateTimeMillsFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, NoDelimiterDateTimeMillsFormatter);
    }

    public static String buildNoDelimiterTimeFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, NoDelimiterTimeFormatter);
    }

    public static String buildGMTFormatter(LocalDateTime localDateTime) {
        return buildDateTimeFormatter(localDateTime, DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static LocalDate parseLocalDate4Simple(String datetimeStr) {
        return parseLocalDate(datetimeStr, SimpleDateFormatter);
    }

    public static LocalDateTime parseLocalDateTime4Simple(String datetimeStr) {
        return parseLocalDateTime(datetimeStr, SimpleDateTimeFormatter);
    }

    public static LocalDateTime parseLocalDateTime4SimpleMillSecond(String datetimeStr) {
        return parseLocalDateTime(datetimeStr, SimpleDateTimeMillSecondFormatter);
    }

    public static LocalDateTime parseLocalDateTime4NoDelimiter(String datetimeStr) {
        return parseLocalDateTime(datetimeStr, NoDelimiterDateTimeFormatter);
    }

    public static LocalDateTime parseLocalDateTime4GMT(String datetimeStr) {
        return parseLocalDateTime(datetimeStr, DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static String buildDateFormatter(LocalDate localDate, DateTimeFormatter dateTimeFormatter) {
        if (localDate == null) {
            return "";
        }
        if (dateTimeFormatter == null) {
            return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return localDate.format(dateTimeFormatter);
        }
    }

    public static String buildDateTimeFormatter(LocalDateTime localDateTime, DateTimeFormatter dateTimeFormatter) {
        if (localDateTime == null) {
            return "";
        }
        if (dateTimeFormatter == null) {
            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return localDateTime.format(dateTimeFormatter);
        }
    }

    public static String buildDateTimeFormatter(LocalDateTime localDateTime, String pattern) {
        if (localDateTime == null) {
            return null;
        }
        return buildDateTimeFormatter(localDateTime, findDateTimeFormatter(pattern));
    }

    public static DateTimeFormatter findDateTimeFormatter(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        } else {
            return DATE_TIME_FORMATTER_MAP.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
        }
    }

    public static LocalDateTime parseLocalDateTime(String datetimeStr, DateTimeFormatter dateTimeFormatter) {
        if (StringUtils.isEmpty(datetimeStr)) {
            return null;
        }
        if (dateTimeFormatter == null) {
            return LocalDateTime.parse(datetimeStr);
        } else {
            return LocalDateTime.parse(datetimeStr, dateTimeFormatter);
        }
    }

    public static LocalDateTime parseLocalDateTime(String datetimeStr, String pattern) {
        return parseLocalDateTime(datetimeStr, findDateTimeFormatter(pattern));
    }

    public static LocalDate parseLocalDate(String dateStr, DateTimeFormatter dateTimeFormatter) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        if (dateTimeFormatter == null) {
            return LocalDate.parse(dateStr);
        } else {
            return LocalDate.parse(dateStr, dateTimeFormatter);
        }
    }

    public static LocalDate parseLocalDate(String dateStr, String pattern) {
        return parseLocalDate(dateStr, findDateTimeFormatter(pattern));
    }

    public static LocalTime parseLocalTime(String timeStr, DateTimeFormatter dateTimeFormatter) {
        if (StringUtils.isEmpty(timeStr)) {
            return null;
        }
        if (dateTimeFormatter == null) {
            return LocalTime.parse(timeStr);
        } else {
            return LocalTime.parse(timeStr, dateTimeFormatter);
        }
    }

    public static LocalTime parseLocalTime(String timeStr, String pattern) {
        return parseLocalTime(timeStr, findDateTimeFormatter(pattern));
    }

    public static long getTimeStampByDateTime(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zoneId).toInstant();
        return instant.toEpochMilli();
    }

    public static Date getDateByLocalDateTime(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        return Date.from(zdt.toInstant());
    }
}
