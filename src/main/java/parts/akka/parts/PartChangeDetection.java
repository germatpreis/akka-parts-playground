package parts.akka.parts;

import java.util.HashSet;
import java.util.Set;

enum Changed {
    NAME,
    WEIGHT,
    REFERENCES
}

class PartChangeDetection {

    Set<Changed> detect(State state, Command.ReceivePart cmd) {
        Set<Changed> result = new HashSet<>();

        if (state.name != null && !state.name.equals(cmd.name())) {
            result.add(Changed.NAME);
        }

        if (state.weight !=null && state.weight != cmd.weight()) {
            result.add(Changed.WEIGHT);
        }

        if (!state.getReferencesAsStringSet().isEmpty() && !state.getReferencesAsStringSet().equals(cmd.references())) {
            result.add(Changed.REFERENCES);
        }

        return result;
    }

}
