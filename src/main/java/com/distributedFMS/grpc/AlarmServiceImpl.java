package com.distributedFMS.grpc;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import io.grpc.stub.StreamObserver;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;

public class AlarmServiceImpl extends AlarmServiceGrpc.AlarmServiceImplBase {

    private final Ignite ignite;

    public AlarmServiceImpl(Ignite ignite) {
        this.ignite = ignite;
    }

    @Override
    public void queryAlarms(QueryAlarmsRequest request, StreamObserver<AlarmMessage> responseObserver) {
        IgniteCache<String, Alarm> cache = ignite.cache(FMSIgniteConfig.getAlarmsCacheName());

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

        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, sql.toString());
        query.setArgs(args.toArray());

        try (var cursor = cache.query(query)) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                Alarm alarm = entry.getValue();
                AlarmMessage message = AlarmMessage.newBuilder()
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
                        .build();
                responseObserver.onNext(message);
            }
        }

        responseObserver.onCompleted();
    }
}
