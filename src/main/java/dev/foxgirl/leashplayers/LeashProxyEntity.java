package dev.foxgirl.leashplayers;

import net.minecraft.entity.*;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public final class LeashProxyEntity extends TurtleEntity {
    private final LivingEntity target;

    private boolean proxyUpdate() {
        if (proxyIsRemoved()) return false;

        if (target == null) return true;
        if (target.world != world || !target.isAlive()) return true;

        Vec3d posActual = getPos();
        Vec3d posTarget = target.getPos().add(0.0D, 1.3D, -0.15D);

        if (!Objects.equals(posActual, posTarget)) {
            setRotation(0.0F, 0.0F);
            setPos(posTarget.getX(), posTarget.getY(), posTarget.getZ());
            setBoundingBox(getDimensions(EntityPose.DYING).getBoxAt(posTarget));
        }

        updateLeash();

        return false;
    }

    @Override
    public void tick() {
        if (world.isClient) return;
        if (proxyUpdate() && !proxyIsRemoved()) {
            proxyRemove();
        }
    }

    public boolean proxyIsRemoved() {
        return this.removed;
    }

    public void proxyRemove() {
        super.remove();
    }

    @Override
    public void remove() {
    }

    public static final String TEAM_NAME = "leashplayersimpl";

    public LeashProxyEntity(LivingEntity target) {
        super(EntityType.TURTLE, target.world);

        this.target = target;

        setHealth(1.0F);
        setInvulnerable(true);

        setBaby(true);
        setInvisible(true);
        noClip = true;

        MinecraftServer server = getServer();
        if (server != null) {
            ServerScoreboard scoreboard = server.getScoreboard();

            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) {
                team = scoreboard.addTeam(TEAM_NAME);
            }
            if (team.getCollisionRule() != AbstractTeam.CollisionRule.NEVER) {
                team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
            }

            scoreboard.addPlayerToTeam(getEntityName(), team);
        }
    }

    @Override
    public float getHealth() {
        return 1.0F;
    }

    @Override
    public void detachLeash(boolean sendPacket, boolean dropItem) {
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    protected void initGoals() {
    }

    @Override
    public boolean collides() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
    }
}
