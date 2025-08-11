# Spring Boot RabbitMQ Message Redrive Pattern

This project is a sample Spring Boot application that demonstrates the message redrive pattern (also known as a retry pattern) using RabbitMQ. It includes a mechanism to automatically retry processing a message a few times before moving it to a failed queue for manual inspection.

## Design

The application is designed with several key components to achieve the redrive mechanism:

### RabbitMQ Topology

The RabbitMQ setup is configured in `RabbitConfig.java` and consists of the following:

- **Main Exchange (`main-exchange`)**: A `DirectExchange` that routes messages to the appropriate queues.
- **Main Queue (`main-queue`)**: The primary queue where new messages are sent for processing.
- **Retry Queue (`retry-queue`)**: When processing a message from the `main-queue` fails, it is sent here. This queue has two important properties:
    - **Time-To-Live (TTL)**: Messages in this queue have a TTL (e.g., 5 seconds). After the TTL expires, RabbitMQ will automatically move the message.
    - **Dead-Letter Exchange (DLX)**: The dead-letter exchange is set to our `main-exchange`. When a message's TTL expires, it is "dead-lettered" and sent back to the `main-exchange` with its original routing key (`main-queue`), effectively re-queuing it for another processing attempt.
- **Failed Queue (`failed-queue`)**: If a message fails processing more than the maximum number of retry attempts, it is sent to this queue. This isolates problematic messages for later analysis without blocking the main processing flow.

### Application Components

- **`MessageListenerService`**: A Spring `@Service` that contains a `@RabbitListener` for the `main-queue`.
    - It simulates a processing failure for every message it receives.
    - It uses a custom message header (`x-retry-count`) to track the number of processing attempts.
    - If the retry count is below the maximum, it increments the count and forwards the message to the `retry-queue`.
    - If the maximum retry count is reached, it moves the message to the `failed-queue`.
- **`QueueHealthIndicator`**: A custom Spring Boot Actuator `HealthIndicator`.
    - It exposes the current message counts for the `main`, `retry`, and `failed` queues via the `/actuator/health` endpoint.
    - If the `failed-queue` contains one or more messages, it reports the application's health status as `DOWN`.
- **`TestController`**: A simple REST controller with an endpoint (`POST /test/send`) to easily publish a message to the `main-queue` for manual testing.

## How to Run and Test

### Prerequisites

- Java 11 or later
- Maven 3.6 or later
- Docker

### 1. Start RabbitMQ

Run a RabbitMQ instance with the management plugin enabled using Docker.

```bash
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

This will start RabbitMQ and expose the standard AMQP port on `5672` and the management UI on `15672`.

### 2. Run the Application

Navigate to the project root and run the application using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

The application will connect to the local RabbitMQ instance.

### 3. Send a Test Message

Open a new terminal and use `curl` to send a sample JSON message to the `TestController`'s endpoint.

```bash
curl -X POST -H "Content-Type: application/json" -d '{"id": "123", "payload": "test-data"}' http://localhost:8080/test/send
```

### 4. Observe the Logs

Watch the console where the Spring Boot application is running. You will see logs indicating:
1. The message is received from the `main-queue`.
2. Processing fails, and the message is sent to the `retry-queue`.
3. After 5 seconds (the TTL), the message is dead-lettered back to the `main-queue` and processing is attempted again.
4. This cycle repeats until the maximum retry count (3) is reached.
5. Finally, the message is sent to the `failed-queue`.

### 5. Check the Health Endpoint

While the application is running, you can check the custom health status:

```bash
curl http://localhost:8080/actuator/health
```

Initially, the status will be `UP`. After the message has been moved to the `failed-queue`, the status will change to `DOWN`, and the response will show the message counts in all queues.

### 6. Verify in RabbitMQ Management UI

Open your web browser and navigate to `http://localhost:15672`.
- **Username**: `guest`
- **Password**: `guest`

In the "Queues" tab, you can observe the messages as they move from `main-queue` to `retry-queue` and finally land in `failed-queue`.
