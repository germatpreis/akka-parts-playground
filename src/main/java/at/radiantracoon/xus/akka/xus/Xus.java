package at.radiantracoon.xus.akka.xus;

import akka.actor.typed.ActorRef;
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
import at.radiantracoon.xus.akka.util.CborSerializable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Xus extends EventSourcedBehavior<Xus.Command, Xus.Event, Xus.State> {

    // *****************************
    // BOOTSTRAP
    // *****************************
    static final EntityTypeKey<Command> ENTITY_KEY = EntityTypeKey.create(Command.class, "Xus");
    static final List<String> TAGS = List.of("xus-0", "xus-1", "xus-2", "xus-3");

    public static void init(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(
                    Entity.of(
                            ENTITY_KEY, entityContext -> {
                                var i = Math.abs(entityContext.getEntityId().hashCode() % TAGS.size());
                                var selectedTag = TAGS.get(i);
                                return Xus.create(entityContext.getEntityId(), selectedTag);
                            }
                    )
                );
    }

    public static Behavior<Command> create(String randomId, String projectionTag) {
        return Behaviors.setup(ctx -> EventSourcedBehavior.start(new Xus(randomId, projectionTag), ctx));
    }

    private Xus(String randomId, String projectionTag) {
        super(
                PersistenceId.of(ENTITY_KEY.name(), randomId),
                SupervisorStrategy.restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(3), 0.1)
        );
        this.randomId = randomId;
        this.projectionTag = projectionTag;
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(100, 3);
    }

    @Override
    public Set<String> tagsFor(Event event) {
        return Set.of(projectionTag);
    }

    // *****************************
    // INSTANCE VARIABLES
    // *****************************
    private final String randomId;
    private final String projectionTag;

    // *****************************
    // STATE
    // *****************************
    static final class State implements CborSerializable {
        private Uid uid;
        private final ReferencedSysIds referencedSysIds;
        private final ManagedSysIds managedSysIds;
        private final TrackedInstances trackedInstances;

        State() {
            this(new ReferencedSysIds(), new ManagedSysIds(), new TrackedInstances());
        }

        State(ReferencedSysIds referencedSysIds, ManagedSysIds managedSysIds, TrackedInstances trackedInstances) {
            this.referencedSysIds = referencedSysIds;
            this.managedSysIds = managedSysIds;
            this.trackedInstances = trackedInstances;
        }

        State generateUid(FirstUpdateSetArrived evt) {
            this.uid = new Uid(String.format("%s-%s-%s", evt.snowCompanyId, evt.snowInstanceId, evt.sysId));
            return this;
        }

        public State addLocalUpdateSet(LocalUpdateSetAddedToXus evt) {
            this.referencedSysIds.add(evt.sysId);
            this.managedSysIds.add(new ManagedSysId(evt.sysId, ManagedSysIdType.LocalUpdateSet, Set.of(evt.snowInstanceId)));
            return this;
        }
    }

    @Override
    public State emptyState() {
        return new State();
    }

    // *****************************
    // COMMANDS
    // *****************************
    interface Command extends CborSerializable {}
    record ProcessLocalUpdateSetCmd(String snowCompanyId, String snowInstanceId, String sysId, String state, boolean deleted, ActorRef<StatusReply<Uid>> replyTo) implements Command {}

    @Override
    public CommandHandler<Command, Event, State> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(ProcessLocalUpdateSetCmd.class, this::onProcessLocalUpdateSetCmd)
                .build();
    }

    private ReplyEffect<Event, State> onProcessLocalUpdateSetCmd(State state, ProcessLocalUpdateSetCmd cmd) {
        var events = new ArrayList<Event>();

        if (state.referencedSysIds.isEmpty()) {
            events.add(new FirstUpdateSetArrived(cmd.snowCompanyId, cmd.snowInstanceId, cmd.sysId));
        }

        events.add(new LocalUpdateSetAddedToXus(cmd.snowCompanyId, cmd.snowInstanceId, cmd.sysId, cmd.state, cmd.deleted));

        return Effect()
                .persist(events)
                .thenReply(cmd.replyTo, updatedState -> StatusReply.success(state.uid));
    }

    // *****************************
    // EVENTS
    // *****************************
    interface Event extends CborSerializable {}
    record FirstUpdateSetArrived(String snowCompanyId, String snowInstanceId, String sysId) implements Event {}
    record LocalUpdateSetAddedToXus(String snowCompanyId, String snowInstanceId, String sysId, String state, boolean deleted) implements Event {}
    record LocalUpdateSetStateChanged(String xusId, String snowCompanyId, String snowInstanceId, String sysId, String state, boolean deleted) implements Event {}
    record LocalUpdateSetMarkedAsDeleted(String xusId, String snowCompanyId, String snowInstanceId, String sysId) implements Event {}

    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(FirstUpdateSetArrived.class, (state, evt) -> state.generateUid(evt))
                .onEvent(LocalUpdateSetAddedToXus.class, (state, evt) -> state.addLocalUpdateSet(evt))
                .build();
    }

}
