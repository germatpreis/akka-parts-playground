package at.radiantracoon.parts.akka;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import at.radiantracoon.parts.akka.parts.PartsAggregate;
import at.radiantracoon.parts.akka.parts.PartsServer;
import at.radiantracoon.parts.akka.parts.PartsServiceImpl;
import at.radiantracoon.parts.akka.repository.DeviceRepository;
import at.radiantracoon.parts.akka.repository.SpringIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

public class MainFoobar {
    private static final Logger logger = LoggerFactory.getLogger(MainFoobar.class);

    public static void main(String[] args) {
        var system = ActorSystem.<Void>create(Behaviors.empty(), "XusService");
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

        // projection
        var springContext = SpringIntegration.applicationContext(system);
        var repository = springContext.getBean(DeviceRepository.class);
        var txManager = springContext.getBean(JpaTransactionManager.class);

        // grpc
        var config = system.settings().config();
        var grpcInterface = config.getString("xus-service.grpc.interface");
        int grpcPort = config.getInt("xus-service.grpc.port");

        var grpcService = new PartsServiceImpl(system, repository);
        PartsServer.start(grpcInterface, grpcPort, system, grpcService);
    }

}
