package at.radiantracoon.parts.akka.devices;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.pattern.StatusReply;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Describes that an aggregate can create another aggregate:
 * https://stackoverflow.com/questions/34064031/how-should-i-structure-persistence-actors-in-akka-persistence
 */
public class DeviceAggregate extends EventSourcedBehaviorWithEnforcedReplies<Command, Event, State> {
    private final Logger logger = LoggerFactory.getLogger(DeviceAggregate.class);

    public static final EntityTypeKey<Command> ENTITY_KEY = EntityTypeKey.create(Command.class, "Device");

    private static final List<String> TAGS = List.of("devices-0", "devices-1", "devices-2", "devices-3", "devices-4");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(
                        Entity.of(
                                ENTITY_KEY,
                                entityContext -> {
                                    int i = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
                                    String selectedTag = TAGS.get(i);
                                    return DeviceAggregate.create(entityContext.getEntityId(), selectedTag);
                                }
                        )
                );
    }

    public static Behavior<Command> create(String partId, String projectionTag) {
        return Behaviors.setup(
                ctx -> EventSourcedBehavior.start(new DeviceAggregate(partId, projectionTag), ctx)
        );
    }

    private final String partId;
    private final String projectionTag;

    private DeviceAggregate(String partId, String projectionTag) {
        super(
                PersistenceId.of(ENTITY_KEY.name(), partId),
                SupervisorStrategy.restartWithBackoff(
                        Duration.ofMillis(200),
                        Duration.ofSeconds(5),
                        0.1
                )
        );
        this.partId = partId;
        this.projectionTag = projectionTag;
    }

    @Override
    public State emptyState() {
        return new State();
    }

    @Override
    public CommandHandlerWithReply<Command, Event, State> commandHandler() {
        return newCommandHandlerWithReplyBuilder()
                .forAnyState()
                .onCommand(Command.CreateDevice.class, this::onCreateDevice)
                .build();
    }

    private ReplyEffect<Event, State> onCreateDevice(State state, Command.CreateDevice cmd) {
        return Effect()
        .persist(new Event.DeviceCreated(cmd.deviceId(), cmd.creatorPartId()))
                .thenReply(cmd.replyTo(), updatedState -> StatusReply.success(updatedState.getDeviceStateSummary()));
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(Event.DeviceCreated.class, (state, evt) -> {
                    logger.info("Received Event.DeviceCreated event: {}", evt);
                    return state.initialize(evt.deviceId, evt.creatorPartId);
                })
                .build();
    }
}