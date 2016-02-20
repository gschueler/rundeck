package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil

/**
 * State of a thread-local logger used by {@link ThreadBoundLogOutputStream}
 */
class LogEventBuffer implements Comparable {
    Date created
    Date time
    Map context
    Boolean crchar
    ByteArrayOutputStream baos

    LogEventBuffer(final Map context) {
        this.context = context
        created = new Date()
        time = new Date()
        baos = new ByteArrayOutputStream()
    }

    boolean isEmpty() {
        time == null
    }

    void clear() {
        this.time = null
        this.context = null
        this.baos = new ByteArrayOutputStream()
        this.crchar = false
    }

    void reset(final Map<String, String> context) {
        this.time = new Date()
        this.context = context
        this.baos = new ByteArrayOutputStream()
    }

    LogEvent createEvent(LogLevel level) {
        return new DefaultLogEvent(
                loglevel: level,
                metadata: context ?: [:],
                message: baos ? new String(baos.toByteArray()) : '',
                datetime: time ?: new Date(),
                eventType: LogUtil.EVENT_TYPE_LOG
        )
    }

    @Override
    int compareTo(final Object o) {
        if (!(o instanceof LogEventBuffer)) {
            return -1;
        }
        LogEventBuffer other = (LogEventBuffer) o
        def td = compareDates(time, other.time)
        if(td!=0) {
            return td
        }
        return compareDates(created, other.created)
    }

    private static int compareDates(Date a, Date b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a != null && b != null) {
            return a.compareTo(b)
        }

        return a != null ? -1 : 1

    }
}
