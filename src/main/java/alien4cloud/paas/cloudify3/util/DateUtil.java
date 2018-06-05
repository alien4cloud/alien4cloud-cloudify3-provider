package alien4cloud.paas.cloudify3.util;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Date;

public class DateUtil {

    // SimpleDateFormat is not thread safe
    private final static ThreadLocal<SimpleDateFormat> LOG_DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS");
        }
    };

    public static int compare(Date left, Date right) {
        if (left == null) {
            if (right != null) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (right != null) {
                return left.compareTo(right);
            } else {
                return 1;
            }
        }
    }

    public static String logDate(Date date) {
        return LOG_DATE_FORMAT.get().format(date);
    }

    public static String logDate(Instant instant) {
        return logDate(Date.from(instant));
    }

}
