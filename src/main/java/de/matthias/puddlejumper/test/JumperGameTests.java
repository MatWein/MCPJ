package de.matthias.puddlejumper.test;

import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.DroneEntity;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Automatische Tests: ./gradlew runGameTestServer */
@GameTestHolder(PuddleJumperMod.MODID)
@PrefixGameTestTemplate(false)
public class JumperGameTests {

    private static PuddleJumperEntity spawnJumper(GameTestHelper h, double x, double y, double z, float yaw) {
        PuddleJumperEntity j = h.spawn(ModRegistry.PUDDLE_JUMPER.get(), (float) x, (float) y, (float) z);
        j.setYRot(yaw);
        j.yRotO = yaw;
        return j;
    }

    /** Ohne Pilot sinkt der Jumper ab und landet auf dem Boden. */
    @GameTest(template = "platform", timeoutTicks = 200)
    public static void landsWithoutPilot(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 8, 12, 0);
        double floor = h.absoluteVec(new Vec3(0, 2, 0)).y; // Struktur liegt 1 Block über dem Strukturblock, Boden bei y=1
        h.succeedWhen(() -> {
            h.assertTrue(j.onGround(), "Jumper ist noch nicht gelandet: y=" + j.getY() + " floor=" + floor + " v=" + j.getDeltaMovement() + " removed=" + j.isRemoved());
            h.assertTrue(Math.abs(j.getY() - floor) < 0.01, "Falsche Landehöhe: " + j.getY() + " statt " + floor);
        });
    }

    /** Bug- und Heck-Segment folgen Position und Ausrichtung, IDs sind fortlaufend. */
    @GameTest(template = "platform")
    public static void partsFollowRotation(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 90f); // Bug nach Westen (-X)
        h.runAfterDelay(2, () -> {
            var parts = j.getParts();
            h.assertTrue(parts.length == 2, "Erwarte 2 Segmente");
            h.assertTrue(parts[0].getId() == j.getId() + 1 && parts[1].getId() == j.getId() + 2, "Segment-IDs falsch");
            double dxFront = parts[0].getX() - j.getX();
            double dxRear = parts[1].getX() - j.getX();
            h.assertTrue(Math.abs(dxFront + 2.7) < 0.01, "Bug-Segment falsch: dx=" + dxFront);
            h.assertTrue(Math.abs(dxRear - 2.7) < 0.01, "Heck-Segment falsch: dx=" + dxRear);
            h.succeed();
        });
    }

    /** Laderaum und Zustände überstehen Speichern/Laden. */
    @GameTest(template = "platform")
    public static void cargoSurvivesSave(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        j.getCargo().setItem(0, new ItemStack(Items.DIAMOND, 17));
        j.getCargo().setItem(53, new ItemStack(Items.ENDER_PEARL, 3));
        j.setCloaked(true);
        CompoundTag tag = j.saveWithoutId(new CompoundTag());

        PuddleJumperEntity copy = ModRegistry.PUDDLE_JUMPER.get().create(h.getLevel());
        h.assertTrue(copy != null, "Konnte Jumper nicht erzeugen");
        copy.load(tag);
        h.assertTrue(copy.getCargo().getItem(0).is(Items.DIAMOND) && copy.getCargo().getItem(0).getCount() == 17, "Slot 0 falsch");
        h.assertTrue(copy.getCargo().getItem(53).is(Items.ENDER_PEARL) && copy.getCargo().getItem(53).getCount() == 3, "Slot 53 falsch");
        h.assertTrue(copy.isCloaked(), "Tarnung nicht gespeichert");
        h.succeed();
    }

    /** Eine Drohne verfolgt und trifft einen Zombie, ohne den eigenen Jumper zu beschädigen. */
    @GameTest(template = "platform", timeoutTicks = 120)
    public static void droneHitsZombie(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 4, 2, 12, -90f); // Bug nach Osten (+X)
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(20, 2, 6));
        zombie.setNoAi(true);
        float startHealth = zombie.getHealth();

        h.runAfterDelay(2, () -> {
            Vec3 start = j.position().add(0, 1.2, 0);
            DroneEntity drone = new DroneEntity(h.getLevel(), h.makeMockPlayer(), j, zombie);
            drone.setPos(start.x, start.y, start.z);
            drone.setDeltaMovement(new Vec3(0.3, 0.3, 0.6)); // bewusst schräg -> muss nachsteuern
            h.getLevel().addFreshEntity(drone);
        });
        h.succeedWhen(() -> {
            h.assertTrue(zombie.getHealth() < startHealth || !zombie.isAlive(), "Zombie wurde nicht getroffen");
            h.assertTrue(j.getDamage() == 0, "Drohne hat den eigenen Jumper beschädigt");
        });
    }

    /** Genug Schaden zerstört den Jumper; Item und Ladung fallen heraus. */
    @GameTest(template = "platform")
    public static void breaksAndDrops(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        j.getCargo().setItem(5, new ItemStack(Items.GOLD_INGOT, 9));
        DamageSource src = h.getLevel().damageSources().generic();
        for (int i = 0; i < 10 && j.isAlive(); i++) j.hurt(src, 8f);
        h.assertTrue(j.isRemoved(), "Jumper wurde nicht zerstört");
        h.runAfterDelay(1, () -> {
            var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, j.getBoundingBox().inflate(4));
            boolean jumperItem = items.stream().anyMatch(e -> e.getItem().is(ModRegistry.PUDDLE_JUMPER_ITEM.get()));
            boolean gold = items.stream().anyMatch(e -> e.getItem().is(Items.GOLD_INGOT));
            h.assertTrue(jumperItem, "Jumper-Item fehlt");
            h.assertTrue(gold, "Ladung wurde nicht fallen gelassen");
            h.succeed();
        });
    }
}
