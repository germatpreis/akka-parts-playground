package at.radiantracoon.parts.akka.devices;

import java.util.Set;

public class DeviceStateSummary {
    private Set<String> referencedParts;
    private String creatorPartId;


    DeviceStateSummary(String creatorPartId, Set<String> referencedParts) {
        this.creatorPartId = creatorPartId;
        this.referencedParts = referencedParts;
    }

    public Set<String> getReferencedParts() {
        return referencedParts;
    }

    public String getCreatorPartId() {
        return creatorPartId;
    }
}
