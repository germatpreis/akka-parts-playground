package at.radiantracoon.parts.akka.parts;

import akka.actor.typed.ActorRef;
import at.radiantracoon.parts.akka.CborSerializable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class State implements CborSerializable {
    LocalDateTime createdAt;
    String name;
    Double weight;
    Map<PartId, ActorRef<Command>> references;

    State(LocalDateTime createdAt, String name, Double weight, Map<PartId, ActorRef<Command>> references) {
        this.createdAt = createdAt;
        this.name = name;
        this.weight = weight;
        this.references = references;
    }

    StateSummary getStateSummary() {
        return new StateSummary(createdAt, references.size());
    }

    Set<String> getReferencesAsStringSet() {
        return references.keySet().stream().map(r -> r.value).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "State{" +
                "createdAt=" + createdAt +
                ", name='" + name + '\'' +
                ", weight=" + weight +
                ", references=" + references +
                '}';
    }

    public State integrate(String name, Double weight, Set<String> references) {
        this.name = name;
        this.weight = weight;
        return mergeReferences(references);
    }

    public State mergeReferences(Set<String> references) {
        references.forEach(r -> this.references.putIfAbsent(new PartId(r), null));
        return this;
    }

    public boolean noMaterializedReferences() {
        return references.isEmpty();
    }
}
