package at.radiantracoon.parts.akka.devices;

import at.radiantracoon.parts.akka.CborSerializable;

public interface Event extends CborSerializable {

    abstract class BaseEvent implements Event {
        final String deviceId;

        protected BaseEvent(String deviceId) {
            this.deviceId = deviceId;
        }
    }

    final class DeviceCreated extends BaseEvent {

        final String creatorPartId;

        protected DeviceCreated(String deviceId, String creatorPartId) {
            super(deviceId);
            this.creatorPartId = creatorPartId;
        }

        @Override
        public String toString() {
            return "DeviceCreated{" +
                    "creatorPartId='" + creatorPartId + '\'' +
                    ", deviceId='" + deviceId + '\'' +
                    '}';
        }
    }



}
