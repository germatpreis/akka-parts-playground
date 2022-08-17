package parts.akka.parts;

import java.util.Objects;

public class PartId {

    final String value;

    public PartId(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartId partId = (PartId) o;
        return value.equals(partId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PartId{" +
                "value='" + value + '\'' +
                '}';
    }
}
