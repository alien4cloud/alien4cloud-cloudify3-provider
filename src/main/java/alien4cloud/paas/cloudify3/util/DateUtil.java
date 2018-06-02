package alien4cloud.paas.cloudify3.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Date;

public class DateUtil {

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
        return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(date);
    }

    public static String logDate(Instant instant) {
        return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(Date.from(instant));
    }

}
