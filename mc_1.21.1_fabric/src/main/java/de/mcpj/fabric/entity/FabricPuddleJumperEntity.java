package de.mcpj.fabric.entity;

import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The jumper on Fabric: bow and stern are segment entities found through {@link SegmentIndex}. */
public class FabricPuddleJumperEntity extends PuddleJumperEntity {
    private final FabricJumperSegment bow;
    private final FabricJumperSegment stern;
    private final List<FabricJumperSegment> segments;

    public FabricPuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        bow = new FabricJumperSegment(this);
        stern = new FabricJumperSegment(this);
        segments = List.of(bow, stern);
        // the segments take the ids right after the ship (like NeoForge's part entities)
        setId(ENTITY_COUNTER.getAndAdd(segments.size() + 1) + 1);
    }

    @Override
    public List<FabricJumperSegment> segments() {
        return segments;
    }

    @Override
    protected void moveSegments(Vec3 bowCenter, Vec3 sternCenter) {
        moveSegment(bow, bowCenter);
        moveSegment(stern, sternCenter);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (segments == null) return;
        for (int i = 0; i < segments.size(); i++) segments.get(i).setId(id + i + 1);
    }

    /** Fabric calls this when the ship is loaded into a level (see McpjFabric). */
    public void loaded() {
        onJoinedLevel();
    }
}
