package at.radiantracoon.parts.akka.parts;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.projection.ProjectionBehavior;
import akka.projection.ProjectionId;
import akka.projection.eventsourced.javadsl.EventSourcedProvider;
import akka.projection.javadsl.AtLeastOnceProjection;
import akka.projection.javadsl.SourceProvider;
import akka.projection.jdbc.javadsl.JdbcProjection;
import at.radiantracoon.parts.akka.repository.HibernateJdbcSession;
import org.springframework.orm.jpa.JpaTransactionManager;
import akka.persistence.query.Offset;
import akka.projection.eventsourced.EventEnvelope;

public class PublishEventsProjection {

    private PublishEventsProjection() {}

    public static void create(ActorSystem<?> system, JpaTransactionManager transactionManager) {
        ShardedDaemonProcess.get(system)
                .init(
                        ProjectionBehavior.Command.class,
                        "PublishEventsProjection",
                        PartsAggregate.TAGS.size(),
                        index -> ProjectionBehavior.create(
                                createProjectionFor(
                                        system,
                                        transactionManager,
                                        index
                                )
                        )
                );
    }

    private static AtLeastOnceProjection<Offset, EventEnvelope<Event>> createProjectionFor(
            ActorSystem<?> system,
            JpaTransactionManager transactionManager,
            int index
    ) {
        var tag = PartsAggregate.TAGS.get(index);

        SourceProvider<Offset, EventEnvelope<Event>> sourceProvider =
                EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), tag);

        return JdbcProjection.atLeastOnceAsync(
                ProjectionId.of("PublishEventsProjection", tag),
                sourceProvider,
                () -> new HibernateJdbcSession(transactionManager),
                () -> new PublishEventsProjectionHandler(system, tag),
                system
        );

    }

}
