package com.distributedFMS.grpc;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import io.grpc.stub.StreamObserver;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;

import javax.cache.Cache;

public class AlarmServiceImpl extends AlarmServiceGrpc.AlarmServiceImplBase {

    private final Ignite ignite;

    public AlarmServiceImpl(Ignite ignite) {
        this.ignite = ignite;
    }

    @Override
    public void queryAlarms(QueryAlarmsRequest request, StreamObserver<AlarmMessage> responseObserver) {
        IgniteCache<String, Alarm> cache = ignite.cache(FMSIgniteConfig.getAlarmsCacheName());

        // For now, we will ignore the request filters and stream all alarms.
        // We will add filtering logic in a subsequent step.
        try (var cursor = cache.query(new ScanQuery<String, Alarm>())) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                Alarm alarm = entry.getValue();
                AlarmMessage message = AlarmMessage.newBuilder()
                        .setAlarmId(alarm.getAlarmId())
                        .setTimestamp(alarm.getTimestamp())
                        .setDeviceId(alarm.getDeviceId())
                        .setSeverity(alarm.getSeverity().name())
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
