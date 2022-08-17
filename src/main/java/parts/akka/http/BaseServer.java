package parts.akka.http;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.function.Function;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class BaseServer {

    protected BaseServer() {}

    protected static void startInternal(String host, int port, ActorSystem<?> system, Function<HttpRequest, CompletionStage<HttpResponse>> service) {
        var bound =
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
