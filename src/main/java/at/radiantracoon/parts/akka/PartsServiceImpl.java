package at.radiantracoon.parts.akka;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import at.radiantracoon.parts.akka.proto.PartService;
import at.radiantracoon.parts.akka.proto.PartsService;
import at.radiantracoon.parts.akka.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class PartsServiceImpl implements PartsService {

    private final ClusterSharding sharding;

    private final DeviceRepository deviceRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PartsServiceImpl(ActorSystem<?> system, DeviceRepository repository) {
        this.deviceRepository = repository;

        sharding = ClusterSharding.get(system);
    }

    @Override
    public CompletionStage<PartService.ReceivePartResponse> receivePart(PartService.ReceivePartRequest in) {
        logger.info("receivePart {}", in.toString());
        var result = PartService.ReceivePartResponse
                .newBuilder()
                .setTimestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return CompletableFuture.completedFuture(result.build());
    }
}
