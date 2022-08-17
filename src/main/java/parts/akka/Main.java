package parts.akka;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import parts.akka.devices.DeviceAggregate;
import parts.akka.parts.PartsAggregate;
import parts.akka.parts.PartsServer;
import parts.akka.parts.PartsServiceImpl;
import parts.akka.parts.PublishEventsProjection;
import parts.akka.repository.SpringIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var system = ActorSystem.<Void>create(Behaviors.empty(), "PartsService");
        try {
            init(system);
        } catch (Exception e) {
            logger.error("Terminating due to initialization feature.", e);
            system.terminate();
        }
    }

    private static void init(ActorSystem<Void> system) {
        // basic
        AkkaManagement.get(system).start();
        ClusterBootstrap.get(system).start();

        // akka persistence
        PartsAggregate.init(system);
        DeviceAggregate.init(system);

        // projection (database, not yet used)
        var springContext = SpringIntegration.applicationContext(system);
        var txManager = springContext.getBean(JpaTransactionManager.class);

        // projection (trigger other aggregates)
        PublishEventsProjection.create(system, txManager);

        // grpc
        var config = system.settings().config();
        var grpcInterface = config.getString("parts-service.grpc.interface");
        int grpcPort = config.getInt("parts-service.grpc.port");

        var grpcService = new PartsServiceImpl(system);
        PartsServer.start(grpcInterface, grpcPort, system, grpcService);
    }

}
