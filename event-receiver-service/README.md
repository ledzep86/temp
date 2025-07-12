# Event Receiver Service

## Overview
The Event Receiver Service is a high-throughput backend service designed to receive, process, and store events in AWS S3. It is built using Spring Boot and leverages asynchronous processing, batching, and thread-safe mechanisms to handle a large volume of incoming events efficiently.

## Key Features
- **High Throughput**: The service is optimized to handle a large number of concurrent requests.
- **Batching**: Events are accumulated in memory and stored in S3 in batches, reducing the number of S3 write operations.
- **Thread Safety**: Thread-safe data structures and mechanisms ensure consistent and reliable processing.
- **Observability**: Metrics are exposed using Micrometer for monitoring the number of successfully processed and failed batches.

## How High Throughput is Achieved
1. **Asynchronous Processing**:
   - The `@Async` annotation in the `EventService` allows the `processEvent` method to run in a separate thread.
   - This ensures that the main thread is not blocked while processing events, allowing the service to handle more incoming requests concurrently.

2. **Batching**:
   - Events are accumulated in a `ConcurrentLinkedQueue`, a thread-safe, non-blocking queue.
   - Batches are flushed to S3 when the total size exceeds 5MB or after a maximum delay of 5 seconds.
   - This reduces the number of S3 write operations, improving overall performance.

3. **Thread-Safe Mechanisms**:
   - The `ConcurrentLinkedQueue` ensures that multiple threads can enqueue events without explicit locking.
   - An `AtomicLong` is used to track the current batch size, ensuring thread-safe updates.
   - The `flushBatch` method is synchronized to ensure that only one thread writes a batch to S3 at a time.

4. **Scheduled Flushing**:
   - A scheduled task runs every 5 seconds to flush any remaining events in the queue, ensuring timely processing even during low traffic.

## How Threads are Handled
- **Asynchronous Threads**:
  - The `@Async` annotation uses Spring's default thread pool to execute the `processEvent` method in separate threads.
  - This allows multiple events to be processed concurrently without blocking the main thread.

- **Thread Safety**:
  - The `ConcurrentLinkedQueue` and `AtomicLong` eliminate the need for explicit locks, allowing threads to safely enqueue events and update the batch size.
  - The `flushBatch` method is synchronized to prevent race conditions during batch flushing.

- **Scheduled Task**:
  - The `@Scheduled` annotation ensures that the `flushBatchOnSchedule` method runs periodically in a separate thread, managed by Spring's task scheduler.

## Configuration
### AWS Credentials
The service uses static credentials for AWS S3 access. These credentials are configured in the application's properties file:
```properties
aws.accessKeyId=your-access-key-id
aws.secretAccessKey=your-secret-access-key
aws.s3.bucketName=your-bucket-name
```

### Valid Customer Tiers
The valid values for the `X-Customer-Tier` HTTP header are loaded from the application's properties file:
```properties
valid.customer.tiers=free,pro,enterprise
```

## Running the Service
1. Ensure that the required AWS credentials and configuration properties are set in the `application.properties` file.
2. Build the project using Maven:
   ```bash
   mvn clean install
   ```
3. Run the service:
   ```bash
   java -jar target/event-receiver-service-1.0-SNAPSHOT.jar
   ```

## Monitoring
The service exposes metrics using Micrometer. These metrics can be integrated with monitoring tools like Prometheus and Grafana to track the number of successfully processed and failed batches.

## Conclusion
The Event Receiver Service is designed to handle high-throughput scenarios efficiently by leveraging asynchronous processing, batching, and thread-safe mechanisms. Its architecture ensures scalability, reliability, and observability, making it suitable for processing large volumes of events in real-time.