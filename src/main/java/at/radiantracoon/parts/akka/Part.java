package at.radiantracoon.parts.akka;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Part {

    @Id
    private final String partId;

    private final String partName;

    private final double partWeight;

    @ManyToOne()
    private Device device;

    public Part() {
        this("", "", 0.0, null);
    }

    public Part(String partId, String partName, double partWeight, Device device) {
        this.partId = partId;
        this.partName = partName;
        this.partWeight = partWeight;
        this.device = device;
    }

    public String getPartId() {
        return partId;
    }

    public String getPartName() {
        return partName;
    }

    public double getPartWeight() {
        return partWeight;
    }

    public Device getDevice() {
        return device;
    }
}
