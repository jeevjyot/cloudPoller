package cloudTrail.poller;

import cloudTrail.client.CTAsyncClient;
import lombok.extern.slf4j.Slf4j;
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

import static java.lang.Math.toIntExact;

@Slf4j
public class CloudTrailPoller {
    private static final int MAX_MESSAGE_PER_REQUEST = 50;
    private final OnMessageReceived onMessageReceived;
    private final Integer maxConcurrentReceiveOperations;
    private final long maxSleepTimeMs;

    private final AtomicLong requested = new AtomicLong();
    private final AtomicBoolean continueProcessing = new AtomicBoolean();
    private final Scheduler pollingScheduler = Schedulers.newSingle("polling-handler");
    private final AtomicInteger concurrentCalls = new AtomicInteger();
    private final Object lock = new Object();
    private final CTAsyncClient ctAsyncClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static String next = "NO_NEXT";

    public CloudTrailPoller(
            OnMessageReceived onMessageReceived,
            CTAsyncClient ctAsyncClient
    ) {
        this.ctAsyncClient = ctAsyncClient;
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
                          //  log.info("Next Token = " + nextToken + " Next Requesting " + s);
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
