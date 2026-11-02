package de.mcpj.gametest;

import de.mcpj.McpjMod;
import de.mcpj.block.ChargingPadBlock;
import de.mcpj.core.config.EnergyConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static de.mcpj.gametest.GameTestSupport.PLATFORM;
import static de.mcpj.gametest.GameTestSupport.spawnJumper;

@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperEnergyTests {
    private static final int MEASURE_TICKS = 10;

    @GameTest(template = PLATFORM)
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

    @GameTest(template = PLATFORM, timeoutTicks = 100)
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

    @GameTest(template = PLATFORM)
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

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void chargingPadCharges(GameTestHelper helper) {
        BlockPos[] pads = {new BlockPos(11, 1, 12), new BlockPos(12, 1, 12), new BlockPos(13, 1, 12)};
        int buffer = ServerConfig.CHARGING_PAD.buffer();
        for (BlockPos pad : pads) {
            helper.setBlock(pad, ModBlocks.CHARGING_PAD.get());
            helper.getBlockEntity(pad).getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
                int accepted = 0;
                while (storage.receiveEnergy(buffer, false) > 0) accepted = storage.getEnergyStored();
                helper.assertTrue(accepted == buffer, "Pad should buffer " + buffer + " FE, has " + accepted);
            });
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
}
