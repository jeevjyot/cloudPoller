package cloudTrail.poller;

import cloudTrail.client.LookupEvents;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.lang.Math.toIntExact;

/**
 * Create a polling operation to receive messages from CloudTrailer.
 *
 * This can be used for SQS events as also (have written this keeping SQS keeping in mind, which would poll events from SQS
 * (cloud trail queue)
 * <p>This class will create a long running thread to collect messages from CloudTrail.
 * The number of messages processed by this thread is limited to number of messages requested.
 */
@Slf4j
public class CloudTrailPoller {
    private static final int MAX_MESSAGE_PER_REQUEST = 50;
    private final OnMessageReceived onMessageReceived;
    private final Integer maxConcurrentReceiveOperations;
    private final long maxSleepTimeMs;

    /**
     * Special counter for the number of the requested messages.
     * There is a need in this counter because we can have concurrent requests to AWS
     * so that {@link FluxSink#requestedFromDownstream()} won't returned actual number of requested messages
     * until the downstream sees those messages.
     * Limited automatically by subscribers (reacting on their demand).
     * Can be throttled by flatMap's concurrency limit {@link reactor.core.publisher.Flux#flatMap(Function,int)}.
     */
    
    private final AtomicLong requested = new AtomicLong();
    private final AtomicBoolean continueProcessing = new AtomicBoolean();
    private final Scheduler pollingScheduler = Schedulers.newSingle("polling-handler");

    /**
     * Limits the number of concurrent calls to AWS,
     * which are needed for fast processing pipelines,
     * where the producer itself might become a bottle-neck.
     *
     * ----- PLEASE NOTE -----
     * We are hardcoding to 1 for cloudTrailer, it could get complicate because of the NEXT, we would need to persist the
     * NEXT token (for pagination) value in DB or define as a critical section. Also there are chances of hitting rate-limiting
     * and getting throttled
     */
    private final AtomicInteger concurrentCalls = new AtomicInteger();
    private final Object lock = new Object();
    private final LookupEvents ctAsyncClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static String next = "NO_NEXT";

    public CloudTrailPoller(
            OnMessageReceived onMessageReceived,
            LookupEvents lookupEvents
    ) {
        this.ctAsyncClient = lookupEvents;
        this.onMessageReceived = onMessageReceived;
        this.maxConcurrentReceiveOperations = 1;
        this.maxSleepTimeMs = 3000;
    }

    /**
     * Requests more messages to be consumed.
     *
     * @param numOfMessages number of messages requested.
     */
    public void request(Long numOfMessages) {
        requested.addAndGet(numOfMessages);
        unlock();
    }

    /**
     * Start this polling on a new thread.
     */
    public void runAsync() {
        if (continueProcessing.compareAndSet(false, true)) {
            executor.submit(this::run);
        }
    }

    /**
     * Terminate this polling.
     *
     * <p>Requests for AWS in flight will have their events ignored
     */
    public void terminate() {
        continueProcessing.set(false);
        unlock();
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to terminate CloudTrail poller");
        }
    }

    private void run() {
        while (continueProcessing.get()) {
            try {
                final AtomicInteger toBeRequested = new AtomicInteger(numberOfMessagesToBeRequested());
                if (toBeRequested.get() <= 0) {
                    lock();
                    continue;
                }

                concurrentCalls.incrementAndGet();

                Mono.just(toBeRequested.get())
                        .flatMap(s -> {
                            String nextToken = fetchNext() == "NO_NEXT" ? null : fetchNext();
                            return ctAsyncClient.lookupEvents(nextToken, s);
                        })
                        .onErrorResume(throwable -> {
                            log.error("Error propagated from the cloud trail async client", throwable);
                            return Mono.just(LookupEventsResponse.builder().build());
                        })
                        .defaultIfEmpty(LookupEventsResponse.builder().build())
                        .subscribeOn(pollingScheduler)
                        .subscribe(lookupEventsResponse -> {
                            persistNext(lookupEventsResponse.nextToken());
                            int offset = toBeRequested.get() - lookupEventsResponse.events().size();
                            requested.getAndAdd(offset);
                            concurrentCalls.decrementAndGet();
                            unlock();
                            if (lookupEventsResponse.hasEvents()) {
                                for (Event event : lookupEventsResponse.events()) {
                                    internalOnMessageReceive(event);
                                }
                            }
                        });
            } catch (Throwable ex) {
                log.error("Failed to lookup the messages", ex);
            }
        }
    }

    // TODO: Move to a different class and define as a critical section
    private String fetchNext() {
        return next;
    }

    // TODO: Move to a different class and define as a critical section
    private void persistNext(String next) {
        CloudTrailPoller.next = next;
    }

    /**
     * Get the number of events to be requested to AWS.
     *
     * @return 0 if no messages should be requested or
     * {@link #requested}, limited by {@link #MAX_MESSAGE_PER_REQUEST}.
     */
    private int numberOfMessagesToBeRequested() {
        if (concurrentCalls.get() >= maxConcurrentReceiveOperations || requested.get() <= 0) {
            return 0;
        }

        final int maxConcurrent = toIntExact(requested.getAndUpdate(actual -> actual - Math.min(actual, MAX_MESSAGE_PER_REQUEST)));
        return Math.min(maxConcurrent, MAX_MESSAGE_PER_REQUEST);
    }

    /**
     * Encapsulates the {@link #onMessageReceived} to not propagate exceptions to main thread.
     *
     * @param event {@link Event} to be received.
     */
    private void internalOnMessageReceive(Event event) {
        try {
            if (continueProcessing.get()) {
                onMessageReceived.receive(event);
            }
        } catch (Throwable ex) {
            log.error("onMessageReceive returned an exception", ex);
        }
    }

    /**
     * This method will lock this thread and wait for {@link #maxSleepTimeMs}
     * or until {@link #unlock()} is invoked.
     */
    private void lock() {
        synchronized (lock) {
            try {
                lock.wait(maxSleepTimeMs);
            } catch (InterruptedException e) {
                log.error("Lock wait timeout was interrupted with exception.", e);
            }
        }
    }

    /**
     * Unlock this thread in case its locked.
     */
    private void unlock() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
