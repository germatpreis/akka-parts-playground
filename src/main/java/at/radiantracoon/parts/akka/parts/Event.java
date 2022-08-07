package at.radiantracoon.parts.akka.parts;

import at.radiantracoon.parts.akka.CborSerializable;

import java.util.Set;

public interface Event extends CborSerializable {

    abstract class BaseEvent implements Event {
        final String partId;
        protected BaseEvent(String partId) {
            this.partId = partId;
        }
    }

    final class PartReceived extends BaseEvent {
        final String name;
        final double weight;
        final Set<String> references;
        public PartReceived(String partId, String name, double weight, Set<String> references) {
            super(partId);
            this.name = name;
            this.weight = weight;
            this.references = references;
        }
    }

    final class PartWeightChanged extends BaseEvent {
        final double oldWeight;
        final double newWeight;

        public PartWeightChanged(String partId, double oldWeight, double newWeight) {
            super(partId);
            this.oldWeight = oldWeight;
            this.newWeight = newWeight;
        }
    }

    final class PartNameChanged extends BaseEvent {
        final String oldName;
        final String newName;

        public PartNameChanged(String partId, String oldName, String newName) {
            super(partId);
            this.oldName = oldName;
            this.newName = newName;
        }
    }

    final class PartReferencesChanged extends BaseEvent {
        final Set<String> oldReferences;
        final Set<String> newReferences;

        public PartReferencesChanged(String partId, Set<String> oldReferences, Set<String> newReferences) {
            super(partId);
            this.oldReferences = oldReferences;
            this.newReferences = newReferences;
        }
    }
}
