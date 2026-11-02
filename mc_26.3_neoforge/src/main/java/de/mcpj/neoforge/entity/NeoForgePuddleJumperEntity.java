package de.mcpj.neoforge.entity;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.List;

/** The jumper on NeoForge: bow and stern are NeoForge part entities. */
public class NeoForgePuddleJumperEntity extends PuddleJumperEntity {
    private final PuddleJumperPart bow;
    private final PuddleJumperPart stern;
    private final PuddleJumperPart[] parts;

    public NeoForgePuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        bow = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        stern = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        parts = new PuddleJumperPart[]{bow, stern};
        // part entities take the ids right after the ship: the server reserves them (its id counter counts up);
        // the client gets the ship's id from the server
        for (int i = 0; i < parts.length; i++) level.getNextEntityId();
        setId(getId());
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
    public void onAddedToLevel() {
        super.onAddedToLevel();
        onJoinedLevel();
    }
}
