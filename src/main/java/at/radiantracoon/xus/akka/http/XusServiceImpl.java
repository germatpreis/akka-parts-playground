package at.radiantracoon.xus.akka.http;

import at.radiantracoon.xus.akka.proto.XusService;
import at.radiantracoon.xus.akka.proto.XusServiceOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class XusServiceImpl implements XusService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public CompletionStage<XusServiceOuterClass.LocalUpdateSetBatchResponse> processLocalUpdateSet(XusServiceOuterClass.LocalUpdateSetBatchRequest in) {
        logger.info("processLocalUpdateSet()");

        var result = XusServiceOuterClass.LocalUpdateSetBatchResponse
                .newBuilder();

        for (var us : in.getLocalUpdateSetsList()) {
            var xusId = String.format("%s-%s-%s", us.getSnowCompanyId(), us.getSnowInstanceId(), us.getSysId());
            var protoXusId = XusServiceOuterClass.XusId.newBuilder()
                    .setNewlyCreated(true)
                    .setId(xusId)
                    .build();
            result.addXusIds(protoXusId);
        }

        return CompletableFuture.completedFuture(result.build());
    }
}
