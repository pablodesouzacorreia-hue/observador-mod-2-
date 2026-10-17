package com.pablo.observador.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Utilitários para achar onde a criatura pode ficar em pé e se o jogador a está vendo. */
public final class Placement {
    private Placement() {}

    /**
     * Procura um lugar onde uma criatura de 3 blocos de altura cabe em pé,
     * perto da altura de referência. Retorna null se não achar ou se o chunk
     * não estiver carregado.
     */
    public static BlockPos findStandPos(Level level, double x, double refY, double z) {
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos feet = BlockPos.containing(x, refY + dy, z);
            if (!level.hasChunkAt(feet)) {
                return null;
            }
            BlockPos below = feet.below();
            BlockState floor = level.getBlockState(below);
            if (!floor.isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            if (!isFree(level, feet) || !isFree(level, feet.above()) || !isFree(level, feet.above(2))) {
                continue;
            }
            if (!level.getFluidState(feet).isEmpty()) {
                continue;
            }
            return feet;
        }
        return null;
    }

    private static boolean isFree(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /** 1.0 = bem no centro da visão do jogador, 0.0 = 90 graus de lado, negativo = atrás. */
    public static double viewDot(Player player, Vec3 pos) {
        Vec3 view = player.getViewVector(1.0F);
        Vec3 delta = pos.subtract(player.getEyePosition());
        double length = delta.length();
        if (length < 1.0E-4) {
            return 1.0;
        }
        return view.dot(delta.scale(1.0 / length));
    }
}
