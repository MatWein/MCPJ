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
import net.minecraft.world.phys.AABB;
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

    /** Ohne Pilot bleibt der Jumper in der Luft stehen (Schwebe-Modus). */
    @GameTest(template = "platform", timeoutTicks = 100)
    public static void hoversWithoutPilot(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 7, 12, 0);
        double startY = j.getY();
        h.runAfterDelay(60, () -> {
            h.assertTrue(!j.onGround(), "Jumper ist gelandet statt zu schweben");
            h.assertTrue(Math.abs(j.getY() - startY) < 0.01, "Jumper ist abgesunken: " + j.getY() + " statt " + startY);
            h.assertTrue(j.isPodsOut(), "Gondeln sollten in der Luft ausgefahren sein");
            h.succeed();
        });
    }

    /** Der Jumper ist fest (auch getarnt) - Bug, Mitte und Heck blockieren Bewegung. */
    @GameTest(template = "platform")
    public static void solidEvenWhenCloaked(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        j.setCloaked(true);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setNoAi(true);
        h.runAfterDelay(2, () -> {
            for (double off : new double[]{-PuddleJumperEntity.SEGMENT_OFFSET, 0, PuddleJumperEntity.SEGMENT_OFFSET}) {
                AABB box = zombie.getBoundingBox().move(j.getX() - zombie.getX(), j.getY() - zombie.getY(), j.getZ() + off - zombie.getZ());
                h.assertTrue(!h.getLevel().noCollision(zombie, box), "Kein Widerstand bei Offset " + off);
            }
            // eigene Bug-/Heck-Teile dürfen den Jumper nicht blockieren (andere Entities hier ignorieren:
            // das Test-Framework kann mehrere Tests nacheinander auf derselben Fläche ausführen)
            for (var part : j.getParts()) {
                h.assertTrue(!j.canCollideWith(part), "Jumper kollidiert mit eigenem Teil");
            }
            h.succeed();
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
            h.assertTrue(Math.abs(dxFront + PuddleJumperEntity.SEGMENT_OFFSET) < 0.01, "Bug-Segment falsch: dx=" + dxFront);
            h.assertTrue(Math.abs(dxRear - PuddleJumperEntity.SEGMENT_OFFSET) < 0.01, "Heck-Segment falsch: dx=" + dxRear);
            h.assertTrue(j.onGround(), "Jumper auf dem Boden sollte als gelandet gelten");
            h.assertTrue(!j.isPodsOut(), "Gondeln sollten gelandet eingefahren sein");
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
    @GameTest(template = "platform", timeoutTicks = 300)
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

    /** Andere Jumper sind gültige Ziele: eine Drohne von A trifft B, A bleibt heil. */
    @GameTest(template = "platform", timeoutTicks = 300)
    public static void droneHitsOtherJumper(GameTestHelper h) {
        PuddleJumperEntity a = spawnJumper(h, 4, 2, 4, 0);
        PuddleJumperEntity b = spawnJumper(h, 18, 2, 18, 90f);
        h.assertTrue(a.isValidTarget(b), "Anderer Jumper sollte erfassbar sein");
        h.assertTrue(!a.isValidTarget(a), "Eigener Jumper darf kein Ziel sein");
        h.runAfterDelay(2, () -> {
            DroneEntity drone = new DroneEntity(h.getLevel(), h.makeMockPlayer(), a, b);
            Vec3 start = a.position().add(0, 2.5, 0);
            drone.setPos(start.x, start.y, start.z);
            drone.setDeltaMovement(new Vec3(0, 0.3, 0.4));
            h.getLevel().addFreshEntity(drone);
        });
        h.succeedWhen(() -> {
            h.assertTrue(b.getDamage() > 0 || !b.isAlive(), "Ziel-Jumper wurde nicht getroffen");
            h.assertTrue(a.getDamage() == 0, "Eigener Jumper wurde beschädigt");
        });
    }

    /** Die Explosion einer eigenen Drohne verschont den Jumper, trifft aber Mobs daneben. */
    @GameTest(template = "platform")
    public static void droneExplosionSparesOwnJumper(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(12, 2, 17));
        zombie.setNoAi(true);
        float startHealth = zombie.getHealth();
        DroneEntity drone = new DroneEntity(h.getLevel(), h.makeMockPlayer(), j, null);
        Vec3 at = j.position().add(0, 1, 4.2);
        drone.setPos(at.x, at.y, at.z);
        h.getLevel().addFreshEntity(drone);
        h.getLevel().explode(drone, at.x, at.y, at.z, 2.0f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        h.assertTrue(j.isAlive() && j.getDamage() == 0, "Eigene Drohnen-Explosion hat den Jumper beschädigt: " + j.getDamage());
        h.assertTrue(zombie.getHealth() < startHealth, "Explosion hat den Zombie nicht getroffen");
        h.succeed();
    }

    /** Kein Rückstoß: Explosion und Stöße verschieben einen schwebenden Jumper nicht. */
    @GameTest(template = "platform", timeoutTicks = 100)
    public static void noKnockback(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 5, 12, 0);
        h.runAfterDelay(2, () -> {
            Vec3 start = j.position();
            h.getLevel().explode(null, j.getX() + 2, j.getY() + 1, j.getZ(), 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            j.push(2, 1, 2);
            j.setDeltaMovement(j.getDeltaMovement().add(3, 0, 3)); // wie Explosions-Rückstoß
            h.runAfterDelay(20, () -> {
                h.assertTrue(j.position().distanceTo(start) < 0.01, "Jumper wurde verschoben: " + j.position().subtract(start));
                h.assertTrue(j.getDamage() > 0, "Explosion sollte trotzdem Schaden machen");
                h.succeed();
            });
        });
    }

    private static void fillWater(GameTestHelper h) {
        for (int x = 8; x <= 16; x++)
            for (int y = 2; y <= 6; y++)
                for (int z = 6; z <= 18; z++)
                    h.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.WATER);
    }

    /** Unter Wasser verliert der Jumper ohne Schild Hülle und treibt nicht auf. */
    @GameTest(template = "platform", timeoutTicks = 120)
    public static void waterDamagesHull(GameTestHelper h) {
        fillWater(h);
        PuddleJumperEntity j = spawnJumper(h, 12, 3, 12, 0);
        h.runAfterDelay(50, () -> {
            h.assertTrue(j.isSubmerged(), "Jumper sollte untergetaucht sein");
            h.assertTrue(j.getHealth() < PuddleJumperEntity.MAX_HEALTH, "Kein Wasserschaden: " + j.getHealth());
            h.assertTrue(Math.abs(j.getY() - h.absoluteVec(new Vec3(0, 3, 0)).y) < 0.01, "Jumper ist aufgetrieben: y=" + j.getY());
            h.succeed();
        });
    }

    /** Mit aktivem Schild nimmt der Jumper unter Wasser keinen Schaden. */
    @GameTest(template = "platform", timeoutTicks = 120)
    public static void shieldBlocksWater(GameTestHelper h) {
        fillWater(h);
        PuddleJumperEntity j = spawnJumper(h, 12, 3, 12, 0);
        j.setShielded(true);
        h.runAfterDelay(50, () -> {
            h.assertTrue(j.isSubmerged(), "Jumper sollte untergetaucht sein");
            h.assertTrue(j.getHealth() == PuddleJumperEntity.MAX_HEALTH, "Schaden trotz Schild: " + j.getHealth());
            h.succeed();
        });
    }

    /** Mit aktivem Schild ist der Jumper unzerstörbar. */
    @GameTest(template = "platform")
    public static void shieldMakesIndestructible(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        j.setShielded(true);
        DamageSource src = h.getLevel().damageSources().generic();
        for (int i = 0; i < 50; i++) j.hurt(src, 20f);
        j.hurt(h.getLevel().damageSources().playerAttack(h.makeMockPlayer()), 1000f);
        h.assertTrue(j.isAlive() && !j.isRemoved(), "Jumper wurde trotz Schild zerstört");
        h.assertTrue(j.getDamage() == 0, "Schild hat Schaden durchgelassen: " + j.getDamage());
        h.succeed();
    }

    /** Eine fremde Drohne übersteht der Jumper knapp, die zweite zerstört ihn (Treffer + Explosion zählen einmal). */
    @GameTest(template = "platform")
    public static void survivesOneDroneNotTwo(GameTestHelper h) {
        PuddleJumperEntity target = spawnJumper(h, 12, 2, 12, 0);
        PuddleJumperEntity attacker = spawnJumper(h, 4, 2, 4, 0);
        var owner = h.makeMockPlayer();
        DroneEntity d1 = new DroneEntity(h.getLevel(), owner, attacker, target);
        target.hurt(h.getLevel().damageSources().thrown(d1, owner), 8f);
        target.hurt(h.getLevel().damageSources().thrown(d1, owner), 30f); // gleiche Drohne (Explosion) -> ignoriert
        float after = target.getHealth() / PuddleJumperEntity.MAX_HEALTH;
        h.assertTrue(target.isAlive(), "Jumper hat die erste Drohne nicht überlebt");
        h.assertTrue(Math.abs(after - 0.4f) < 0.01f, "Erwarte 40 % Hülle nach einer Drohne, ist " + after);
        DroneEntity d2 = new DroneEntity(h.getLevel(), owner, attacker, target);
        target.hurt(h.getLevel().damageSources().thrown(d2, owner), 8f);
        h.assertTrue(target.isRemoved(), "Zweite Drohne hat den Jumper nicht zerstört");
        h.succeed();
    }

    /** Genug Schaden zerstört den Jumper; Item und Ladung fallen heraus. */
    @GameTest(template = "platform")
    public static void breaksAndDrops(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        j.getCargo().setItem(5, new ItemStack(Items.GOLD_INGOT, 9));
        DamageSource src = h.getLevel().damageSources().generic();
        int hits = 0;
        while (j.isAlive() && hits < 200) {
            j.hurt(src, 7f); // Diamantschwert
            hits++;
        }
        h.assertTrue(j.isRemoved(), "Jumper wurde nicht zerstört");
        h.assertTrue(hits >= 20, "Jumper hält zu wenig aus: nur " + hits + " Schläge");
        h.runAfterDelay(1, () -> {
            var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, j.getBoundingBox().inflate(4));
            boolean jumperItem = items.stream().anyMatch(e -> e.getItem().is(ModRegistry.PUDDLE_JUMPER_ITEM.get()));
            boolean gold = items.stream().anyMatch(e -> e.getItem().is(Items.GOLD_INGOT));
            h.assertTrue(jumperItem, "Jumper-Item fehlt");
            h.assertTrue(gold, "Ladung wurde nicht fallen gelassen");
            h.succeed();
        });
    }

    /** Licht an: unsichtbare Lichtblöcke im Rumpf; aus, getarnt oder entfernt: wieder weg. */
    @GameTest(template = "platform")
    public static void lightsPlaceAndRemoveBlocks(GameTestHelper h) {
        PuddleJumperEntity j = spawnJumper(h, 12, 2, 12, 0);
        BlockPos center = BlockPos.containing(j.position().add(0, PuddleJumperEntity.HEIGHT * 0.5, 0));
        j.setLightsOn(true);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.getLevel().getBlockState(center).is(net.minecraft.world.level.block.Blocks.LIGHT), "Kein Lichtblock bei Licht an");
            j.setCloaked(true);
            h.runAfterDelay(2, () -> {
                h.assertTrue(h.getLevel().getBlockState(center).isAir(), "Getarnt sollte es dunkel sein");
                j.setCloaked(false);
                h.runAfterDelay(2, () -> {
                    h.assertTrue(h.getLevel().getBlockState(center).is(net.minecraft.world.level.block.Blocks.LIGHT), "Licht nach Enttarnen fehlt");
                    j.discard();
                    h.assertTrue(h.getLevel().getBlockState(center).isAir(), "Lichtblock nach Entfernen liegengeblieben");
                    h.succeed();
                });
            });
        });
    }
}
