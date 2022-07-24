package at.radiantracoon.xus.akka;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import at.radiantracoon.xus.akka.http.XusServer;
import at.radiantracoon.xus.akka.http.XusServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Foobar {
    private static final Logger logger = LoggerFactory.getLogger(Foobar.class);

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
        AkkaManagement.get(system).start();
        ClusterBootstrap.get(system).start();

        var config = system.settings().config();
        var grpcInterface = config.getString("xus-service.grpc.interface");
        int grpcPort = config.getInt("xus-service.grpc.port");
        var grpcService = new XusServiceImpl();
        XusServer.start(grpcInterface, grpcPort, system, grpcService);
    }

}
