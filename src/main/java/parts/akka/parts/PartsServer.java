package parts.akka.parts;

import akka.actor.typed.ActorSystem;
import akka.grpc.javadsl.ServerReflection;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.function.Function;
import parts.akka.http.BaseServer;
import parts.akka.proto.PartsService;
import parts.akka.proto.PartsServiceHandlerFactory;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

public class PartsServer extends BaseServer {

    private PartsServer() {}

    public static void start(String host, int port, ActorSystem<?> system, PartsService grpcService) {
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> service =
                ServiceHandler.concatOrNotFound(
                        PartsServiceHandlerFactory.create(grpcService, system),
                        ServerReflection.create(
                                Collections.singletonList(PartsService.description), system));

        startInternal(host, port, system, service);
    }

}
