package org.cloudburstmc.server.network;

import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.level.gamerule.GameRuleMap;

import java.util.List;

@UtilityClass
public class NetworkUtils {

    public static AttributeData toNetwork(Attribute attr) {
        return new AttributeData(attr.getName(), attr.getMinValue(), attr.getMaxValue(), attr.getValue(), attr.getDefaultValue());
    }

    public static void toNetwork(GameRuleMap gameRules, List<GameRuleData<?>> networkRules) {
        gameRules.forEach((rule, o) -> {
            networkRules.add(new GameRuleData<>(rule.getName(), o));
        });
    }
}
