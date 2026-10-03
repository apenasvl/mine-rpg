package com.rpgstats.gui;

import java.util.List;
import java.util.function.Predicate;

/** Shared, dependency-free copy for the corruption HUD and Codex. */
public final class MageCorruptionText {
    public record GuidePage(String title, String body) {}

    private static final List<GuidePage> GUIDE = List.of(
            new GuidePage("CORRUPÇÃO: ORIGEM",
                    "Ocultista e Sanguimante.\nConhecimento Proibido libera +3 por Blood/Eldritch, até 1 vez/s. Máximo: 100.\nApós mais de 6s sem lançar, perde 1/s."),
            new GuidePage("CORRUPÇÃO: PODER",
                    "Com Conhecimento Proibido e o talento da faixa:\n25+: Tentação +4%.\n50+: Poder Instável +8%.\n75+: Poder Proibido +12%.\nDano mágico bruto. As faixas não somam."),
            new GuidePage("CORRUPÇÃO: RISCO",
                    "Talentos comprados:\n50+: Eficiência Proibida, -6% custo de Mana.\n75+: Poder Proibido, +8% dano recebido.\nO balanceamento do feitiço pode reduzir o bônus mágico e a economia."),
            new GuidePage("CORRUPÇÃO: PURGA",
                    "Exige os talentos:\nPurificação: ativa, -40. Recarga: 20s.\nReação Adversa: golpe de 6+ de dano remove 10. Recarga interna: 4s.\nSem lançar, também dissipa. Mínimo: zero."),
            new GuidePage("CORRUPÇÃO: HUD",
                    "CORR: carga atual.\nDM: bônus mágico bruto.\nM: custo de Mana.\nR: dano recebido.\nSó efeitos comprados.\nCorrupção não aplica Bleed por si só nem substitui a escala de INT.")
    );

    private MageCorruptionText() {}

    public static List<GuidePage> guidePages() {
        return GUIDE;
    }

    /** Raw corruption contributions; final spell scaling is applied by combat balance. */
    public static String hudEffects(float corruption, Predicate<String> hasNode) {
        int damage = 0;
        if (hasNode.test("mag_occ_forbidden")) {
            if (corruption >= 75f && hasNode.test("mag_occ_forbidden_power")) damage = 12;
            else if (corruption >= 50f && hasNode.test("mag_occ_instability")) damage = 8;
            else if (corruption >= 25f && hasNode.test("mag_occ_temptation")) damage = 4;
        }
        StringBuilder text = new StringBuilder();
        if (damage > 0) text.append("DM+").append(damage).append('%');
        if (corruption >= 50f && hasNode.test("mag_occ_efficiency")) append(text, "M-6%");
        if (corruption >= 75f && hasNode.test("mag_occ_forbidden_power")) append(text, "R+8%");
        return text.isEmpty() ? "sem bônus" : text.toString();
    }

    private static void append(StringBuilder text, String value) {
        if (!text.isEmpty()) text.append(' ');
        text.append(value);
    }
}
