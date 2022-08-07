package at.radiantracoon.parts.akka.devices;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import at.radiantracoon.parts.akka.CborSerializable;

public interface Command extends CborSerializable {

    record CreateDevice(ActorRef<StatusReply<DeviceStateSummary>> replyTo, String deviceId, String creatorPartId) implements Command {}

}
