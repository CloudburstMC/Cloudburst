package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.resolver.PositionResolver;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.*;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class ParticleCommand extends AdvertisedCommand {
    public ParticleCommand() {
        super("particle", "commands.particle.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.particle");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("particle", arguments.particle())
                .then(Commands.argument("position", CommandArgumentTypes.position())
                        .executes(this::executeCommand)
                        .then(Commands.argument("count", CommandArgumentTypes.integer(1))
                                .executes(this::executeCommand)
                                .then(Commands.argument("data", CommandArgumentTypes.integer())
                                        .executes(this::executeCommand)))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        Location defaultLocation;
        if (sender instanceof CloudPlayer) {
            defaultLocation = ((CloudPlayer) sender).getLocation();
        } else {
            defaultLocation = Location.from(Vector3f.ZERO, sender.getServer().getDefaultLevel());
        }

        Vector3f parsedPosition = argumentValue(context, "position", PositionResolver.class)
                .resolve(context.getSource());
        Location location = Location.from(parsedPosition, defaultLocation.getLevel());

        int count = hasArgument(context, "count") ? argumentValue(context, "count", Integer.class) : 1;

        int data = hasArgument(context, "data") ? argumentValue(context, "data", Integer.class) : -1;

        ParticleType type = argumentValue(context, "particle", ParticleType.class);
        String name = type.id().getName();

        sender.sendMessage(Component.translatable("commands.particle.success",
                Component.text(name), Component.text(count)));

        CloudLevel level = (CloudLevel) location.getLevel();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            Vector3f position = location.getPosition()
                    .add(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1);
            Particle particle = this.createParticle(type, position, data);
            if (particle == null) {
                level.spawnParticle(type, position);
            } else {
                level.addParticle(particle);
            }
        }

        return success();
    }

    private Particle createParticle(ParticleType type, Vector3f pos, int data) {
        String name = type.id().getName();
        return switch (name) {
            case "explode" -> new ExplodeParticle(pos);
            case "large_explode" -> new HugeExplodeSeedParticle(pos);
            case "huge_explosion" -> new HugeExplodeParticle(pos);
            case "bubble" -> new BubbleParticle(pos);
            case "water_splash" -> new SplashParticle(pos);
            case "water_wake" -> new WaterParticle(pos);
            case "crit" -> new CriticalParticle(pos);
            case "smoke" -> new SmokeParticle(pos, data != -1 ? data : 0);
            case "mob_spell" -> new EnchantParticle(pos);
            case "mob_spell_instantaneous" -> new InstantEnchantParticle(pos);
            case "drip_water" -> new WaterDripParticle(pos);
            case "drip_lava" -> new LavaDripParticle(pos);
            case "town_aura" -> new SporeParticle(pos);
            case "portal" -> new PortalParticle(pos);
            case "flame" -> new FlameParticle(pos);
            case "lava" -> new LavaParticle(pos);
            case "red_dust" -> new RedstoneParticle(pos, data != -1 ? data : 1);
            case "snowball_poof" -> new ItemBreakParticle(pos,
                    ItemStack.builder().itemType(ItemTypes.SNOWBALL).build());
            case "slime" -> new ItemBreakParticle(pos,
                    ItemStack.builder().itemType(ItemTypes.SLIME_BALL).build());
            case "heart" -> new HeartParticle(pos, data != -1 ? data : 0);
            case "ink" -> new InkParticle(pos, data != -1 ? data : 0);
            case "rain_splash" -> new RainSplashParticle(pos);
            case "enchanting_table" -> new EnchantmentTableParticle(pos);
            case "villager_happy" -> new HappyVillagerParticle(pos);
            case "villager_angry" -> new AngryVillagerParticle(pos);
            case "block_force_field" -> new BlockForceFieldParticle(pos);
            default -> null;
        };
    }
}
