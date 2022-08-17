package parts.akka.parts;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import parts.akka.CborSerializable;

import java.util.Set;

public interface Command extends CborSerializable {

    record ReceivePart(ActorRef<StatusReply<StateSummary>> replyTo, String partId, String name, double weight, Set<String> references) implements Command {}
    record ReportDeviceAssociation(ActorRef<StatusReply<Command>> replyTo) implements Command {}

}
