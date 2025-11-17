package com.distributedFMS.grpc;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import io.grpc.stub.StreamObserver;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryUpdatedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlarmServiceImpl extends AlarmServiceGrpc.AlarmServiceImplBase {
    
    private final Ignite ignite;
    private final Map<StreamObserver<AlarmMessage>, QueryCursor<Cache.Entry<String, Alarm>>> activeQueries;

    public AlarmServiceImpl(Ignite ignite) {
        this.ignite = ignite;
        this.activeQueries = new ConcurrentHashMap<>();
    }

    @Override
    public void queryAlarms(QueryAlarmsRequest request, StreamObserver<AlarmMessage> responseObserver) {
        System.out.printf("[GRPC] === Query Alarms Request Started ===%n");
        IgniteCache<String, Alarm> cache = ignite.cache(FMSIgniteConfig.getAlarmsCacheName());
        
        if (cache == null) {
            System.err.printf("[GRPC] ERROR: Alarms cache is NULL!%n");
            responseObserver.onError(new RuntimeException("Alarms cache not available"));
            return;
        }
        
        System.out.printf("[GRPC] Cache size: %d%n", cache.size());

        // Build initial SQL query with filters
        StringBuilder sql = new StringBuilder("SELECT * FROM Alarm");
        List<Object> args = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (request.hasDeviceId()) {
            conditions.add("deviceId = ?");
            args.add(request.getDeviceId());
        }
        if (request.hasSeverity()) {
            conditions.add("severity = ?");
            args.add(request.getSeverity());
        }
        if (request.hasEventType()) {
            conditions.add("eventType = ?");
            args.add(request.getEventType());
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        
        System.out.printf("[GRPC] SQL Query: %s%n", sql.toString());
        System.out.printf("[GRPC] Query args: %s%n", args);

        // Send initial snapshot of alarms
        SqlQuery<String, Alarm> initialQuery = new SqlQuery<>(Alarm.class, sql.toString());
        initialQuery.setArgs(args.toArray());
        
        int alarmCount = 0;
        try (var cursor = cache.query(initialQuery)) {
            System.out.printf("[GRPC] Executing SQL query...%n");
            for (Cache.Entry<String, Alarm> entry : cursor) {
                Alarm alarm = entry.getValue();
                alarmCount++;
                System.out.printf("[GRPC] Sending alarm #%d: %s - %s%n", alarmCount, alarm.getAlarmId(), alarm.getDescription());
                AlarmMessage message = toAlarmMessage(alarm);
                responseObserver.onNext(message);
            }
            System.out.printf("[GRPC] === Sent %d alarms in initial snapshot ===%n", alarmCount);
        } catch (Exception e) {
            System.err.printf("[GRPC] Error sending initial alarms: %s%n", e.getMessage());
            e.printStackTrace();
            responseObserver.onError(e);
            return;
        }

        // Set up Continuous Query for real-time updates
        ContinuousQuery<String, Alarm> continuousQuery = new ContinuousQuery<>();
        
        continuousQuery.setLocalListener(new CacheEntryUpdatedListener<String, Alarm>() {
            @Override
            public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends Alarm>> events) {
                for (CacheEntryEvent<? extends String, ? extends Alarm> event : events) {
                    try {
                        Alarm alarm = event.getValue();
                        
                        // Apply filters
                        if (request.hasDeviceId() && !alarm.getDeviceId().equals(request.getDeviceId())) {
                            continue;
                        }
                        if (request.hasSeverity() && !alarm.getSeverity().equals(request.getSeverity())) {
                            continue;
                        }
                        if (request.hasEventType() && !alarm.getEventType().equals(request.getEventType())) {
                            continue;
                        }

                        AlarmMessage message = toAlarmMessage(alarm);
                        responseObserver.onNext(message);
                        System.out.printf("[GRPC] Pushed update for alarm: %s%n", alarm.getAlarmId());
                    } catch (Exception e) {
                        System.err.printf("[GRPC] Error pushing alarm update: %s%n", e.getMessage());
                    }
                }
            }
        });

        try {
            QueryCursor<Cache.Entry<String, Alarm>> cursor = cache.query(continuousQuery);
            activeQueries.put(responseObserver, cursor);
            System.out.printf("[GRPC] Continuous query started for client%n");
        } catch (Exception e) {
            System.err.printf("[GRPC] Error starting continuous query: %s%n", e.getMessage());
            responseObserver.onError(e);
        }
    }

    private AlarmMessage toAlarmMessage(Alarm alarm) {
        return AlarmMessage.newBuilder()
                .setAlarmId(alarm.getAlarmId())
                .setTimestamp(alarm.getTimestamp())
                .setDeviceId(alarm.getDeviceId())
                .setSeverity(alarm.getSeverity())
                .setEventType(alarm.getEventType())
                .setDescription(alarm.getDescription())
                .setGeographicRegion(alarm.getGeographicRegion())
                .setStatus(alarm.getStatus().name())
                .setTallyCount(alarm.getTallyCount())
                .setFirstOccurrence(alarm.getFirstOccurrence())
                .setLastOccurrence(alarm.getLastOccurrence())
                .setCorrelationId(alarm.getCorrelationId() != null ? alarm.getCorrelationId() : "")
                .setRootCauseAlarmId(alarm.getRootCauseAlarmId() != null ? alarm.getRootCauseAlarmId() : "")
                .build();
    }

    public void cleanup() {
        System.out.printf("[GRPC] Cleaning up %d active queries%n", activeQueries.size());
        activeQueries.values().forEach(cursor -> {
            try {
                cursor.close();
            } catch (Exception e) {
                System.err.printf("[GRPC] Error closing query cursor: %s%n", e.getMessage());
            }
        });
        activeQueries.clear();
    }
}
