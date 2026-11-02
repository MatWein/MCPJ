package de.mcpj.forge.entity.jumper;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The jumper on Forge: bow and stern are Forge part entities; Forge's dimension change hook. */
public class ForgePuddleJumperEntity extends PuddleJumperEntity {
    private final PuddleJumperPart bow;
    private final PuddleJumperPart stern;
    private final PuddleJumperPart[] parts;

    public ForgePuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        bow = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        stern = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        parts = new PuddleJumperPart[]{bow, stern};
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    @Override
    public List<PuddleJumperPart> segments() {
        return List.of(parts);
    }

    @Override
    protected void moveSegments(Vec3 bowCenter, Vec3 sternCenter) {
        moveSegment(bow, bowCenter);
        moveSegment(stern, sternCenter);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts == null) return;
        for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        onAddedToLevel();
    }

    @Override
    @Nullable
    public Entity changeDimension(ServerLevel destination, ITeleporter teleporter) {
        return changeDimensionWithPassengers(() -> super.changeDimension(destination, teleporter));
    }
}
