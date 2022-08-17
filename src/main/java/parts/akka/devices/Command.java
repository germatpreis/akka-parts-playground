package parts.akka.devices;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import parts.akka.CborSerializable;

public interface Command extends CborSerializable {

    record CreateDevice(ActorRef<StatusReply<DeviceStateSummary>> replyTo, String deviceId, String creatorPartId) implements Command {}

}
