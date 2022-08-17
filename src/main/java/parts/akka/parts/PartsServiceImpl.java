package parts.akka.parts;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.grpc.GrpcServiceException;
import parts.akka.proto.PartService;
import parts.akka.proto.PartsService;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class PartsServiceImpl implements PartsService {

    private final ClusterSharding sharding;
    private final Duration timeout;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PartsServiceImpl(ActorSystem<?> system) {
        sharding = ClusterSharding.get(system);

        this.timeout = Duration.ofSeconds(5);
    }

    @Override
    public CompletionStage<PartService.ReceivePartResponse> receivePart(PartService.ReceivePartRequest in) {
        logger.info("receivePart {}", in.toString());

        var entityRef = sharding.entityRefFor(PartsAggregate.ENTITY_KEY, in.getPartId());

        CompletionStage<StateSummary> reply = entityRef.askWithStatus(
                replyTo -> new Command.ReceivePart(
                        replyTo,
                        in.getPartId(),
                        in.getPartName(),
                        in.getPartWeight(),
                        new HashSet<>(in.getReferencesList())
                ),
                timeout
        );

        CompletionStage<PartService.ReceivePartResponse> response = reply.thenApply(this::toReceivePartResponse);

        return convertError(response);
    }

    private PartService.ReceivePartResponse toReceivePartResponse(StateSummary stateSummary) {
        return PartService.ReceivePartResponse
                .newBuilder()
                .setTimestamp(stateSummary.createdAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
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
