package de.mcpj.mc.gametest;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LightningBolt;
import de.mcpj.mc.entity.jumper.JumperLightning;
import de.mcpj.mc.block.ChargingPadBlock;
import de.mcpj.core.config.EnergyConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;

import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;

/** Energy game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperEnergyTestCases {
    private JumperEnergyTestCases() {}

    private static final int MEASURE_TICKS = 10;

    public static void startChargeAndNoDriveDrainWhenLanded(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        int expected = (int) Math.round(ServerConfig.ENERGY.capacity() * ServerConfig.ENERGY.startCharge());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(jumper.energy().stored() == expected, "Start charge " + jumper.energy().stored() + " instead of " + expected);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(jumper.onGround(), "Jumper should have landed");
                helper.assertTrue(jumper.energy().stored() == expected, "Landed jumper consumed energy");
                helper.succeed();
            });
        });
    }

    public static void systemsDrainEnergy(GameTestHelper helper) {
        EnergyConfig config = ServerConfig.ENERGY;
        PuddleJumperEntity jumper = spawnJumper(helper, 5, 6, 18, 0);
        int[] before = new int[1];
        helper.runAfterDelay(2, () -> before[0] = jumper.energy().stored());
        helper.runAfterDelay(2 + MEASURE_TICKS, () -> {
            assertDrain(helper, before[0] - jumper.energy().stored(), config.hoverDrain(), "Hovering");
            jumper.setShielded(true);
            before[0] = jumper.energy().stored();
        });
        helper.runAfterDelay(2 + 2 * MEASURE_TICKS, () -> {
            assertDrain(helper, before[0] - jumper.energy().stored(), config.hoverDrain() + config.shieldDrain(), "Shield");
            jumper.hurt(helper.getLevel().damageSources().generic(), 10f);
            helper.assertTrue(jumper.hull().integrity() == 1f, "Shield did not absorb the hit");
            before[0] = jumper.energy().stored();
        });
        helper.runAfterDelay(2 + 3 * MEASURE_TICKS, () -> {
            double underFire = config.hoverDrain() + config.shieldDrain() * config.underFireFactor();
            assertDrain(helper, before[0] - jumper.energy().stored(), underFire, "Under fire");
            helper.succeed();
        });
    }

    private static void assertDrain(GameTestHelper helper, int drained, double perTick, String what) {
        double expected = perTick * MEASURE_TICKS;
        helper.assertTrue(Math.abs(drained - expected) <= perTick, what + ": drained " + drained + " FE instead of ~" + expected);
    }

    public static void shieldCollapsesWithoutEnergy(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        helper.runAfterDelay(2, () -> {
            jumper.energy().set(1000);
            jumper.setShielded(true);
            jumper.setLightsOn(true);
            jumper.hurt(helper.getLevel().damageSources().generic(), 10f);
            helper.assertTrue(!jumper.isShielded(), "Shield should have collapsed");
            helper.assertTrue(jumper.hull().integrity() < 1f, "Hit did not go through");
            helper.assertTrue(!jumper.energy().hasEnergy() && !jumper.isLightsOn(), "Collapse should drain the storage and shut down all systems");
            jumper.energy().set(10);
            jumper.setLightsOn(true);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(!jumper.energy().hasEnergy(), "Storage should be empty");
                helper.assertTrue(!jumper.isLightsOn(), "Lights should go out without energy");
                helper.succeed();
            });
        });
    }

    public static void chargingPadCharges(GameTestHelper helper) {
        BlockPos[] pads = {new BlockPos(11, 1, 12), new BlockPos(12, 1, 12), new BlockPos(13, 1, 12)};
        int buffer = ServerConfig.CHARGING_PAD.buffer();
        for (BlockPos pad : pads) {
            helper.setBlock(pad, GameTestCompat.modBlock("charging_pad"));
            int accepted = GameTestHooks.get().fillChargingPad(GameTestCompat.blockEntity(helper, pad), buffer);
            helper.assertTrue(accepted == buffer, "Pad should buffer " + buffer + " FE, has " + accepted);
        }
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        int[] before = new int[1];
        helper.runAfterDelay(2, () -> before[0] = jumper.energy().stored());
        helper.runAfterDelay(22, () -> {
            int gained = jumper.energy().stored() - before[0];
            int expectedMinimum = pads.length * ServerConfig.CHARGING_PAD.chargeRate() * MEASURE_TICKS;
            helper.assertTrue(gained > expectedMinimum, "Charged only " + gained + " FE");
            BlockState state = helper.getBlockState(pads[1]);
            helper.assertTrue(state.getValue(ChargingPadBlock.MODE) == ChargingPadBlock.Mode.CHARGING, "Pad does not show charging");
            helper.assertTrue(state.getLightEmission() == ChargingPadBlock.lightLevel(state) && state.getLightEmission() > 0, "Charging pad does not glow");
            helper.succeed();
        });
    }

    /** Lightning charges 20 % once per bolt (hull and segments are all hit) and does no damage. */
    public static void lightningChargesJumper(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.setShielded(false);
        int capacity = jumper.energy().capacity();
        jumper.energy().set(capacity / 10);
        LightningBolt bolt = GameTestCompat.lightning(helper);
        bolt.setPos(jumper.getX(), jumper.getBoundingBox().maxY, jumper.getZ());
        helper.getLevel().addFreshEntity(bolt);
        helper.runAfterDelay(20, () -> {
            float charge = jumper.energy().fraction();
            helper.assertTrue(Math.abs(charge - 0.3f) < 0.01f, "Expected 30 % after the strike, got " + charge);
            helper.assertTrue(jumper.hull().integrity() == 1f, "Lightning damaged the hull");
            helper.assertTrue(jumper.isAlive(), "Jumper destroyed by lightning");
            helper.succeed();
        });
    }

    /** A lightning bolt near a jumper strikes the jumper instead, but only under open sky. */
    public static void jumperAttractsLightning(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        LightningBolt bolt = GameTestCompat.lightning(helper);
        Vec3 beside = helper.absoluteVec(new Vec3(17, 2, 16));
        bolt.setPos(beside.x, beside.y, beside.z);
        JumperLightning.strikeNearestJumper(bolt);
        // Forge's game test server has no open sky above the tests: then the bolt must stay where it is
        boolean openSky = helper.getLevel().canSeeSky(BlockPos.containing(jumper.getX(), jumper.getBoundingBox().maxY, jumper.getZ()));
        boolean moved = Math.abs(bolt.getX() - jumper.getX()) < 0.01 && Math.abs(bolt.getZ() - jumper.getZ()) < 0.01;
        helper.assertTrue(moved == openSky, (openSky ? "Bolt not moved onto the jumper: " : "Bolt moved onto a jumper without open sky: ")
                + bolt.position());
        helper.succeed();
    }

    /** Without energy from cables (and requirePower off, the default) pads charge on their own at the free rate. */
    public static void chargingPadChargesWithoutPower(GameTestHelper helper) {
        BlockPos[] pads = {new BlockPos(11, 1, 12), new BlockPos(12, 1, 12), new BlockPos(13, 1, 12)};
        for (BlockPos pad : pads) helper.setBlock(pad, GameTestCompat.modBlock("charging_pad"));
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.setShielded(false);
        int[] before = new int[1];
        helper.runAfterDelay(2, () -> before[0] = jumper.energy().stored());
        helper.runAfterDelay(22, () -> {
            int gained = jumper.energy().stored() - before[0];
            // measured over 20 ticks; the jumper charges in steps, so allow a few ticks either way
            int perTick = pads.length * ServerConfig.CHARGING_PAD.freeChargeRate();
            helper.assertTrue(gained >= perTick * 15 && gained <= perTick * 25,
                    "Expected about " + perTick * 20 + " FE without power, charged " + gained);
            BlockState state = helper.getBlockState(pads[1]);
            helper.assertTrue(state.getValue(ChargingPadBlock.MODE) == ChargingPadBlock.Mode.CHARGING, "Pad does not show charging");
            helper.succeed();
        });
    }
}
