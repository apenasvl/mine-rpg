package com.rpgstats;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rpgstats.boss.BossScaler;
import com.rpgstats.combat.CombatHandler;
import com.rpgstats.combat.CombatState;
import com.rpgstats.combat.MageCombatHandler;
import com.rpgstats.compat.CompatManager;
import com.rpgstats.stats.PlayerStats;
import com.rpgstats.stats.StatsManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@net.minecraftforge.fml.common.Mod(RPGStatsMod.MOD_ID)
public class RPGStatsMod {
    public static final String MOD_ID = "rpgstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier SYNC = new Identifier(MOD_ID, "sync_stats");
    public static final Identifier OPEN = new Identifier(MOD_ID, "open_screen");
    public static final Identifier ALLOCATE = new Identifier(MOD_ID, "allocate");
    public static final Identifier SELECT_CLASS = new Identifier(MOD_ID, "select_class");
    public static final Identifier SELECT_SUBCLASS = new Identifier(MOD_ID, "select_subclass");
    public static final Identifier SELECT_PATH = new Identifier(MOD_ID, "select_path");
    public static final Identifier SELECT_SPECIALIZATION = new Identifier(MOD_ID, "select_specialization");
    public static final Identifier AWAKEN = new Identifier(MOD_ID, "awaken");
    public static final Identifier SELECT_AFFINITY = new Identifier(MOD_ID, "select_affinity");
    public static final Identifier SELECT_ACTIVE = new Identifier(MOD_ID, "select_active");
    public static final Identifier UNLOCK = new Identifier(MOD_ID, "unlock_node");
    public static final Identifier ACTIVATE = new Identifier(MOD_ID, "activate_ability");

    public RPGStatsMod() {
        com.rpgstats.network.RpgNetwork.initialize();
        CompatManager.initialize();
        LOGGER.info("RPG Stats Forge: {} optional mod(s) detected.", CompatManager.activeModules().size());
    }

    public static void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
                dispatcher.register(literal("rpg")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) openScreen(player);
                            return 1;
                        })
                        .then(literal("open").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) openScreen(player);
                            return 1;
                        }))
                        .then(literal("ability").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) CombatHandler.activateAbility(player);
                            return 1;
                        }))
                        .then(literal("guia").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                syncStats(player);
                                com.rpgstats.network.RpgNetwork.openGuide(player);
                            }
                            return 1;
                        }))
                        .then(literal("debug")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) com.rpgstats.debug.RpgDebug.sendState(player);
                                    return 1;
                                })
                                .then(literal("on").executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) com.rpgstats.debug.RpgDebug.enable(player);
                                    return 1;
                                }))
                                .then(literal("off").executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) com.rpgstats.debug.RpgDebug.disable(player);
                                    return 1;
                                }))
                                .then(literal("state").executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) com.rpgstats.debug.RpgDebug.sendState(player);
                                    return 1;
                                }))
                                .then(literal("clear").executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) com.rpgstats.debug.RpgDebug.clear(player);
                                    return 1;
                                })))
                        .then(literal("addpoints").requires(source -> source.hasPermissionLevel(2))
                                .then(argument("jogador", EntityArgumentType.players())
                                        .then(argument("quantidade", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    int amount = IntegerArgumentType.getInteger(context, "quantidade");
                                                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "jogador")) {
                                                        PlayerStats stats = StatsManager.get(player);
                                                        stats.statPoints += amount;
                                                        StatsManager.finish(player, stats);
                                                    }
                                                    return 1;
                                                }))))
                        .then(literal("addxp").requires(source -> source.hasPermissionLevel(2))
                                .then(argument("jogador", EntityArgumentType.players())
                                        .then(argument("quantidade", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    int amount = IntegerArgumentType.getInteger(context, "quantidade");
                                                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "jogador")) {
                                                        StatsManager.addXp(player, amount);
                                                    }
                                                    return 1;
                                                }))))
                        .then(literal("compat").executes(context -> {
                            context.getSource().sendFeedback(() -> Text.literal("§6RPG Stats: §e"
                                    + CompatManager.activeModules().size() + " mod(s) opcional(is) detectado(s)."), false);
                            for (var module : CompatManager.activeModules()) {
                                context.getSource().sendFeedback(() -> Text.literal("§a- " + module.id()
                                        + "§7: " + module.featureSummary()), false);
                            }
                            return 1;
                        }))
                        .then(literal("reset").requires(source -> source.hasPermissionLevel(2))
                                .then(argument("jogador", EntityArgumentType.players())
                                        .executes(context -> {
                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "jogador")) {
                                                CombatState.remove(player.getUuid());
                                                MageCombatHandler.remove(player.getUuid());
                                                StatsManager.finish(player, new PlayerStats());
                                                player.sendMessage(Text.literal("§cSeus atributos foram resetados."), false);
                                            }
                                            return 1;
                                        }))));
    }

    public static void syncStats(ServerPlayerEntity player) {
        PlayerStats stats = StatsManager.get(player);
        NbtCompound nbt = stats.toNbt();

        // Estado transitório exclusivamente visual: nunca é persistido no PlayerStats.
        CombatState combat = CombatState.get(player.getUuid());
        nbt.putInt("uiArcherAffinity", Math.floorMod(combat.classMode,3));
        NbtCompound cooldowns = new NbtCompound();
        NbtCompound cooldownMax = new NbtCompound();
        for (int slot = 0; slot < PlayerStats.ACTIVE_SLOTS; slot++) {
            String nodeId = stats.activeSlot(slot);
            if (nodeId == null || nodeId.isBlank()) continue;
            int current = combat.cooldown(nodeId);
            if (current <= 0) continue;
            cooldowns.putInt(nodeId, current);
            cooldownMax.putInt(nodeId, Math.max(current, combat.cooldownMax(nodeId)));
        }
        nbt.put("uiCooldowns", cooldowns);
        nbt.put("uiCooldownMax", cooldownMax);

        com.rpgstats.network.RpgNetwork.sync(player, nbt);
    }

    public static void openScreen(ServerPlayerEntity player) {
        com.rpgstats.network.RpgNetwork.open(player);
    }
}

