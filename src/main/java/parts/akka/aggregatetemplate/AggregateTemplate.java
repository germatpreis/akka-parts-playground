package parts.akka.aggregatetemplate;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandlerWithReply;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.EventSourcedBehaviorWithEnforcedReplies;

import java.time.Duration;
import java.util.List;

public class AggregateTemplate extends EventSourcedBehaviorWithEnforcedReplies<Command, Event, State> {

    private static final EntityTypeKey<Command> ENTITY_KEY = EntityTypeKey.create(Command.class, "Part");

    private static final List<String> TAGS = List.of("parts-0", "parts-1", "parts-2", "parts-3", "parts-4");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(
                        Entity.of(
                                ENTITY_KEY,
                                entityContext -> {
                                    int i = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
                                    String selectedTag = TAGS.get(i);
                                    return AggregateTemplate.create(entityContext.getEntityId(), selectedTag);
                                }
                        )
                );
    }

    public static Behavior<Command> create(String partId, String projectionTag) {
        return Behaviors.setup(
                ctx -> EventSourcedBehavior.start(new AggregateTemplate(partId, projectionTag), ctx)
        );
    }

    private final String partId;
    private final String projectionTag;

    private AggregateTemplate(String partId, String projectionTag) {
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
        return newCommandHandlerWithReplyBuilder().build();
    }

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder().build();
    }
}
