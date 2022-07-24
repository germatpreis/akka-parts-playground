package at.radiantracoon.xus.akka.xus;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.*;

record Uid(String value) {}

record ReferencedSysIds(Set<String> sysIds) {

    ReferencedSysIds() {
        this(emptySet());
    }

    boolean isEmpty() {
        return sysIds.isEmpty();
    }

    boolean contains(String sysId) {
        return sysId.contains(sysId);
    }

    public void add(String sysId) {
        this.sysIds.add(sysId);
    }
}

enum ManagedSysIdType {
    LocalUpdateSet,
    RemoteUpdateSet
}
record ManagedSysIds(Set<ManagedSysId> sysIds) {

    ManagedSysIds() {
        this(emptySet());
    }

    Optional<ManagedSysId> find(ManagedSysId managedSysId) {
        return sysIds.stream().filter(s -> s.equals(managedSysId)).findFirst();
    }

    public void add(ManagedSysId managedSysId) {
        var potentiallyExistingManagedSysId = find(managedSysId);

        if (potentiallyExistingManagedSysId.isPresent()) {
            var existingManagedSysId = potentiallyExistingManagedSysId.get();
            existingManagedSysId.addToFoundInInstance(managedSysId.foundInInstance());
        }

    }
}
record ManagedSysId(String sysId, ManagedSysIdType type, Set<String> foundInInstance) {

    ManagedSysId(String sysId, ManagedSysIdType type) {
        this(sysId, type, new HashSet<>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedSysId that = (ManagedSysId) o;
        return sysId.equals(that.sysId) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sysId, type);
    }

    public ManagedSysId addToFoundInInstance(Set<String> foundInInstance) {
        this.foundInInstance.addAll(foundInInstance);
        return this;
    }
}

record TrackedInstances(List<Instance> instance) {

    TrackedInstances() {
        this(List.of());
    }

    Optional<Instance> findBySnowInstanceId(String snowInstanceId) {
        return this.instance.stream().filter(i -> i.snowInstanceId().equals(snowInstanceId)).findFirst();
    }

}

record Instance(String snowInstanceId, TrackedLocalUpdateSets trackedLocalUpdateSets) {}

record TrackedLocalUpdateSets(List<LocalUpdateSet> updateSets) {

    TrackedLocalUpdateSets() {
        this(List.of());
    }

    Optional<LocalUpdateSet> findBySysId(String sysId) {
        return this.updateSets.stream().filter(us -> us.sysId().equals(sysId)).findFirst();
    }

}

record LocalUpdateSet(String snowInstanceId, String sysId, String state, boolean deleted) {}
