package at.radiantracoon.parts.akka.parts;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PartsAggregate extends EventSourcedBehaviorWithEnforcedReplies<Command, Event, State> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final EntityTypeKey<Command> ENTITY_KEY = EntityTypeKey.create(Command.class, "Part");

    static final List<String> TAGS = List.of("parts-0", "parts-1", "parts-2", "parts-3", "parts-4");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(
                        Entity.of(
                                ENTITY_KEY,
                                entityContext -> {
                                    int i = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
                                    String selectedTag = TAGS.get(i);
                                    return PartsAggregate.create(entityContext.getEntityId(), selectedTag);
                                }
                        )
                );
    }

    private static Behavior<Command> create(String partId, String projectionTag) {
        return Behaviors.setup(
                ctx -> EventSourcedBehavior.start(new PartsAggregate(partId, projectionTag), ctx)
        );
    }

    private final PartChangeDetection partChangeDetection;

    private final String partId;
    private final String projectionTag;

    private PartsAggregate(String partId, String projectionTag) {
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

        this.partChangeDetection = new PartChangeDetection();
    }
    @Override
    public State emptyState() {
        return new State(LocalDateTime.now(), null, null, new HashMap<>());
    }

    @Override
    public CommandHandlerWithReply<Command, Event, State> commandHandler() {
        return newCommandHandlerWithReplyBuilder()
                .forAnyState()
                .onCommand(Command.ReceivePart.class, this::onReceivePart)
                .build();
    }

//    private CommandHandlerWithReplyBuilderByState<Command, Event, State, State> partAlreadyReceived() {
//        return newCommandHandlerWithReplyBuilder()
//                .forState(State::isAlreadyReceived)
//                .onCommand(
//                        Command.ReceivePart.class, cmd -> Effect()
//                                .reply(cmd.replyTo(), StatusReply.error("Can't receive receive the same part " + cmd.partId() + " again"))
//                );
//    }

    private ReplyEffect<Event, State> onReceivePart(State state, Command.ReceivePart cmd) {
        var events = new ArrayList<Event>();
        events.add(new Event.PartReceived(cmd.partId(), cmd.name(), cmd.weight(), cmd.references()));

        var changes = partChangeDetection.detect(state, cmd);

        if (changes.contains(Changed.NAME)) {
            events.add(new Event.PartNameChanged(partId, state.name, cmd.name()));
        }

        if (changes.contains(Changed.WEIGHT)) {
            events.add(new Event.PartWeightChanged(partId, state.weight, cmd.weight()));
        }

        if (changes.contains(Changed.REFERENCES)) {
            events.add(new Event.PartReferencesChanged(partId, state.getReferencesAsStringSet(), cmd.references()));
        }

        if (state.noMaterializedReferences()) {
            events.add(new Event.FirstPartOfDeviceReceived(partId, cmd.name(), cmd.weight()));
        }

        return Effect()
                .persist(events)
                .thenReply(cmd.replyTo(), updatedState -> StatusReply.success(updatedState.getStateSummary()));
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(Event.PartReceived.class, (state, evt) -> {
                    logger.info("Received Event.PartReceived");
                    return state.integrate(evt.name, evt.weight, evt.references);
                })
                .onEvent(Event.FirstPartOfDeviceReceived.class, (state, evt) -> {
                    logger.info("Received Event.FirstPartOfDeviceReceived");
                    return state;
                })
                .onEvent(Event.PartNameChanged.class, (state, evt) -> {
                    logger.info("Received Event.PartNameChanged");
                    return state;
                })
                .onEvent(Event.PartWeightChanged.class, (state, evt) -> {
                    logger.info("Received Event.PartWeightChanged");
                    return state;
                })
                .onEvent(Event.PartReferencesChanged.class, (state, evt) -> {
                    logger.info("Received Event.PartReferencesChanged");
                    return state;
                })
                .build();
    }

    @Override
    public Set<String> tagsFor(Event event) {
        return Set.of(projectionTag);
    }
}
