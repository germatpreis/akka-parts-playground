package at.radiantracoon.xus.akka.http;

import akka.actor.typed.ActorSystem;
import akka.grpc.javadsl.ServerReflection;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.function.Function;
import at.radiantracoon.xus.akka.proto.XusService;
import at.radiantracoon.xus.akka.proto.XusServiceHandlerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

public class XusServer {

    private XusServer() {}

    public static void start(String host, int port, ActorSystem<?> system, XusService grpcService) {
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> service =
                ServiceHandler.concatOrNotFound(
                        XusServiceHandlerFactory.create(grpcService, system),
                        // ServerReflection enabled to support grpcurl without import-path and proto parameters
                        ServerReflection.create(
                                Collections.singletonList(XusService.description), system));

        CompletionStage<ServerBinding> bound =
                Http.get(system).newServerAt(host, port).bind(service::apply);

        bound.whenComplete(
                (binding, ex) -> {
                    if (binding != null) {
                        binding.addToCoordinatedShutdown(Duration.ofSeconds(3), system);
                        InetSocketAddress address = binding.localAddress();
                        system
                                .log()
                                .info(
                                        "Shopping online at gRPC server {}:{}",
                                        address.getHostString(),
                                        address.getPort());
                    } else {
                        system.log().error("Failed to bind gRPC endpoint, terminating system", ex);
                        system.terminate();
                    }
                });
    }

}
