package org.cloudburstmc.server.command.data;

import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;

import java.util.*;

/**
 * @author CreeperFace
 */
@ToString
public class CommandEnum {

    private final String name;
    private final List<String> values;

    public CommandEnum(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public CommandEnumData toNetwork() {
        String[] aliases;
        if (values.size() > 0) {
            List<String> aliasList = new ArrayList<>(values);
            aliasList.add(this.name);
            aliases = aliasList.toArray(new String[0]);
        } else {
            aliases = new String[]{this.name};
        }
        return new CommandEnumData(this.name + "Aliases", toNetwork(aliases), false);
    }

    private static LinkedHashMap<String, Set<CommandEnumConstraint>> toNetwork(String[] values) {
        LinkedHashMap<String, Set<CommandEnumConstraint>> map = new LinkedHashMap<>();
        for (String value : values) {
            map.put(value, Collections.emptySet());
        }
        return map;
    }
}
