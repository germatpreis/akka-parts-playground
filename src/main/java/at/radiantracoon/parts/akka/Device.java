package at.radiantracoon.parts.akka;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    private final String deviceId;

    @Version
    private final Long version;

    private final String deviceName;

    @OneToMany(mappedBy = "device")
    private final List<Part> parts;

    public Device() {
        this("", 0L, "", List.of());
    }

    public Device(String deviceId, Long version, String deviceName, List<Part> parts) {
        this.deviceId = deviceId;
        this.version = version;
        this.deviceName = deviceName;
        this.parts = parts;
    }

    public Device updateDeviceName(String newName) {
        return new Device(deviceId, version, newName, new ArrayList<>(parts));
    }
}
