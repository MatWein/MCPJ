package de.mcpj.core.jumper;

public final class JumperGeometry {
    public static final float RENDER_SCALE = 1.3f;
    public static final float HITBOX_SCALE = 1.1f;

    public static final float WIDTH = 2.6f * HITBOX_SCALE;
    public static final float HEIGHT = 2.0f * HITBOX_SCALE;
    public static final double SEGMENT_OFFSET = 2.7 * HITBOX_SCALE;
    public static final double CENTER_HEIGHT = HEIGHT / 2.0;

    private static final float MODEL_LENGTH_UNITS = 28.9f;
    private static final float BASE_LENGTH_BLOCKS = 8.0f;
    public static final float MODEL_SCALE = BASE_LENGTH_BLOCKS * RENDER_SCALE / MODEL_LENGTH_UNITS;

    // Model units; must match the drive pods in models/entity/puddle_jumper.obj
    public static final float POD_X = 4.95f;
    public static final float POD_Y = 4.3f;
    public static final float POD_REAR_Z = -12.2f;
    public static final float POD_EXTENSION = 1.6f;
    public static final float POD_DROP = 0.2f;
    public static final float NOZZLE_OFFSET_X = 0.38f;
    public static final float NOZZLE_HALF_WIDTH = 0.26f;
    public static final float NOZZLE_HALF_HEIGHT = 0.8f;
    public static final double PILOT_SEAT_Z = 6.3;
    public static final double LAUNCHER_X = 4.2;
    public static final double LAUNCHER_Y = 4.3;
    public static final double LAUNCHER_Z = -1.0;

    public static final float TILT_PIVOT_HEIGHT = RENDER_SCALE;
    public static final double BOW_LIGHT_DISTANCE = 5.0 * RENDER_SCALE;
    public static final double DISMOUNT_LONGITUDINAL = 5.0 * RENDER_SCALE;
    public static final double DISMOUNT_LATERAL = 2.4 * RENDER_SCALE;
    public static final double CAMERA_HEIGHT = 1.2 * RENDER_SCALE;

    private JumperGeometry() {}
}
