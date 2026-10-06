package com.rpgstats.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;

/** Admin-only recording controls; no changes to player UI or combat. */
public final class ArenaRecorderCommands {
    public static LiteralArgumentBuilder<ServerCommandSource> tree() {
        return literal("arena").requires(s->s.hasPermissionLevel(2))
            .then(literal("start").then(argument("boss",EntityArgumentType.entity())
                .then(argument("build",StringArgumentType.word()).executes(c->{
                    var p=c.getSource().getPlayer();var target=EntityArgumentType.getEntity(c,"boss");
                    boolean ok=target instanceof LivingEntity b && ArenaRecorder.start(p,b,StringArgumentType.getString(c,"build"));
                    if(!ok)c.getSource().sendError(Text.literal("Boss vivo no mesmo mundo, classe ativa e build offensive/balanced/defensive necessários; máximo8 sessões."));return ok?1:0;
                }))))
            .then(literal("stop").executes(c->ArenaRecorder.stop(c.getSource().getPlayer(),"STOPPED")==null?0:1))
            .then(literal("mark").then(argument("note",StringArgumentType.word()).executes(c->{ArenaRecorder.mark(c.getSource().getPlayer(),StringArgumentType.getString(c,"note"));return 1;})));
    }
    private ArenaRecorderCommands(){}
}
