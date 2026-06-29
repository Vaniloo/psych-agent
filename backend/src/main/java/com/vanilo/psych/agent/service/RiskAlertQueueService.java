package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.entity.RiskAlertEvent;
import com.vanilo.psych.agent.repository.RiskAlertEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskAlertQueueService {
    private static final String STREAM_KEY = "risk:alert:stream";
    private static final String CONSUMER_GROUP = "psych-agent-alerts";
    private static final String CONSUMER_NAME = "alert-worker";

    private final StreamOperations<String, Object, Object> streamOperations;
    private final RiskAlertDispatcher dispatcher;
    private final RiskAlertEventRepository eventRepository;
    private final boolean streamEnabled;
    private volatile boolean groupReady;

    public RiskAlertQueueService(StringRedisTemplate redisTemplate,
                                 RiskAlertDispatcher dispatcher,
                                 RiskAlertEventRepository eventRepository,
                                 @Value("${psych.alerts.redis-stream-enabled:true}") boolean streamEnabled) {
        this.streamOperations = redisTemplate.opsForStream();
        this.dispatcher = dispatcher;
        this.eventRepository = eventRepository;
        this.streamEnabled = streamEnabled;
    }

    public void publish(Long eventId) {
        if (!streamEnabled) {
            dispatcher.dispatch(eventId);
            return;
        }
        try {
            ensureGroup();
            streamOperations.add(STREAM_KEY, Map.of("eventId", eventId.toString()));
        } catch (RuntimeException ignored) {
            dispatcher.dispatch(eventId);
        }
    }

    @Scheduled(fixedDelayString = "${psych.alerts.poll-delay-ms:1000}")
    @SuppressWarnings("unchecked")
    public void consume() {
        if (!streamEnabled) {
            return;
        }
        try {
            ensureGroup();
            List<MapRecord<String, Object, Object>> records = streamOperations.read(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(10),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
            );
            if (records == null) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                Object eventId = record.getValue().get("eventId");
                if (eventId != null) {
                    dispatcher.dispatch(Long.valueOf(eventId.toString()));
                }
                streamOperations.acknowledge(CONSUMER_GROUP, record);
            }
        } catch (RuntimeException ignored) {
            groupReady = false;
        }
    }

    @Scheduled(fixedDelayString = "${psych.alerts.recovery-delay-ms:60000}")
    public void recoverPendingEvents() {
        eventRepository.findTop20ByStatusAndAttemptsLessThanOrderByCreatedAtAsc("PENDING", 3)
                .stream()
                .map(RiskAlertEvent::getId)
                .forEach(this::publish);
    }

    private synchronized void ensureGroup() {
        if (groupReady) {
            return;
        }
        try {
            streamOperations.createGroup(
                    STREAM_KEY, ReadOffset.from("0-0"), CONSUMER_GROUP
            );
        } catch (RuntimeException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("BUSYGROUP")) {
                throw exception;
            }
        }
        groupReady = true;
    }
}
