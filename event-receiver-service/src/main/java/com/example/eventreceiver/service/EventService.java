package com.example.eventreceiver.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.core.sync.RequestBody;

@Service
public class EventService {

    private final S3Client s3Client;
    private final MeterRegistry meterRegistry;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    private final ConcurrentLinkedQueue<String> eventQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong currentBatchSize = new AtomicLong(0);
    private static final long MAX_BATCH_SIZE = 5 * 1024 * 1024; // 5MB

    public EventService(S3Client s3Client, MeterRegistry meterRegistry) {
        this.s3Client = s3Client;
        this.meterRegistry = meterRegistry;
    }

    @Async
    public void processEvent(String eventPayload) {
        eventQueue.add(eventPayload);
        currentBatchSize.addAndGet(eventPayload.getBytes().length);

        if (currentBatchSize.get() >= MAX_BATCH_SIZE) {
            flushBatch();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void flushBatchOnSchedule() {
        if (!eventQueue.isEmpty()) {
            flushBatch();
        }
    }

    private synchronized void flushBatch() {
        if (eventQueue.isEmpty()) {
            return;
        }

        try {
            List<String> batch = new ArrayList<>();
            long batchSize = 0;

            while (!eventQueue.isEmpty() && batchSize < MAX_BATCH_SIZE) {
                String event = eventQueue.poll();
                if (event != null) {
                    batch.add(event);
                    batchSize += event.getBytes().length;
                }
            }

            String fileName = "batch-" + System.currentTimeMillis() + ".json";
            String batchContent = String.join("\n", batch);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(batchContent));
            System.out.println("Batch stored in S3: " + fileName);

            // Increment success counter
            meterRegistry.counter("batches.processed.success").increment();
        } catch (Exception e) {
            System.err.println("Error storing batch in S3: " + e.getMessage());

            // Increment error counter
            meterRegistry.counter("batches.processed.error").increment();
        } finally {
            currentBatchSize.set(0);
        }
    }
}