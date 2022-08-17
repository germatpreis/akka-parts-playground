package parts.akka.devices;

import parts.akka.CborSerializable;

import java.util.HashMap;
import java.util.Map;

final class State implements CborSerializable {
    private String deviceId;
    private Map<String, String> referencedParts = new HashMap<>();
    private String creatorPartId;

    State initialize(String deviceId, String creatorPartId) {
        this.deviceId = deviceId;
        this.creatorPartId = creatorPartId;
        this.referencedParts.put(creatorPartId, null);
        return this;
    }

    public DeviceStateSummary getDeviceStateSummary() {
        return new DeviceStateSummary(creatorPartId, referencedParts.keySet());
    }
}
