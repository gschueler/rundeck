package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import rundeck.services.execution.ValueHolder

/**
 * Counts line per-node and returns the largest so far
 */
class NodeCountingLogWriter extends FilterStreamingLogWriter implements ValueHolder<Long> {
    MaximumNodeLines counter = new MaximumNodeLines()

    NodeCountingLogWriter(StreamingLogWriter writer) {
        super(writer)
    }

    @Override
    void addEvent(final LogEvent event) {
        getWriter().addEvent(event)
        if (event.eventType == LogUtil.EVENT_TYPE_LOG && event.metadata?.node && event.message) {
            counter.nodeLogged(event.metadata.node, event.message.split('\n').length)
        }
    }

    @Override
    Long getValue() {
        return counter.value
    }
}
