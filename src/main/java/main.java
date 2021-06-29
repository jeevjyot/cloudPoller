
import cloudTrail.factory.CloutTrailConfigFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class main {

    public static void main(String args[]) throws Exception {
        //This subscribes the pipeline which calls the pollers and processes each event
        //it is subscribed on a thread which is different than a main thread.
        CloutTrailConfigFactory.getCloudTrailPipelineFactory()
                .get()
                .doOnError(throwable -> log.error("Something went wrong"))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        //This is a workaround to keep the main thread running (only needed in console application). This is nothing but creating a deadlock sitaution.
        // currentThread is a main thread which is infact for a main thread and boom deadlock. Should we have created a server, we would not need this
        Thread.currentThread().join();
    }
}
