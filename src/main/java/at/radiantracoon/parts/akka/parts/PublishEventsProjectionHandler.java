package at.radiantracoon.parts.akka.parts;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.grpc.GrpcServiceException;
import akka.projection.eventsourced.EventEnvelope;
import akka.projection.javadsl.Handler;
import at.radiantracoon.parts.akka.devices.Command;
import at.radiantracoon.parts.akka.devices.DeviceAggregate;
import at.radiantracoon.parts.akka.devices.DeviceStateSummary;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class PublishEventsProjectionHandler extends Handler<EventEnvelope<Event>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClusterSharding sharing;
    private final Duration timeout;
    private final String tag;

    public PublishEventsProjectionHandler(ActorSystem<?> system, String tag) {
        this.sharing = ClusterSharding.get(system);
        this.timeout = Duration.ofSeconds(5);
        this.tag = tag;
    }

    @Override
    public CompletionStage<Done> process(EventEnvelope<Event> envelope) throws Exception, Exception {
        logger.info("PublishEventsProjectionHandler.process called for tag {}", tag);
        var event = envelope.event();
        if (event instanceof Event.FirstPartOfDeviceReceived) {
            var firstPartOfDeviceReceived = (Event.FirstPartOfDeviceReceived) event;

            var deviceId = "device-" + firstPartOfDeviceReceived.partId;
            var entityRef = sharing.entityRefFor(DeviceAggregate.ENTITY_KEY, deviceId);

            CompletionStage<DeviceStateSummary> reply = entityRef.askWithStatus(
                    replyTo -> new Command.CreateDevice(replyTo, deviceId, firstPartOfDeviceReceived.partId),
                    timeout
            );

            CompletionStage<Done> response = reply.thenApply(summary -> Done.done());
            return convertError(response);
        }
        return CompletableFuture.completedFuture(Done.done());
    }

    private static <T> CompletionStage<T> convertError(CompletionStage<T> response) {
        return response.exceptionally(
                ex -> {
                    if (ex instanceof TimeoutException) {
                        throw new GrpcServiceException(
                                Status.UNAVAILABLE.withDescription("Operation timed out"));
                    } else {
                        throw new GrpcServiceException(
                                Status.INVALID_ARGUMENT.withDescription(ex.getMessage()));
                    }
                });
    }
}
