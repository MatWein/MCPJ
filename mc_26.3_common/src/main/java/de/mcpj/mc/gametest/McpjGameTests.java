package de.mcpj.mc.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * All game tests of the mod. Each loader registers the functions in Minecraft's test function registry
 * ({@code mcpj:<name>}); the build generates a test instance per entry ({@code data/mcpj/test_instance/<name>.json},
 * structure {@code mcpj:platform}) from this list, so names and time limits live only here.
 */
public final class McpjGameTests {
    /** A test: its name, how many ticks it may take and the test itself. */
    public record TestCase(String name, int maxTicks, Consumer<GameTestHelper> function) {}

    public static final List<TestCase> ALL = List.of(
            test("cargo_survives_save", 100, JumperCargoTestCases::cargoSurvivesSave),
            test("picks_up_nearby_items", 60, JumperCargoTestCases::picksUpNearbyItems),
            test("drone_hits_zombie", 300, JumperCombatTestCases::droneHitsZombie),
            test("attack_on_pilot_hits_hull", 100, JumperCombatTestCases::attackOnPilotHitsHull),
            test("zombie_attacks_hull_of_pilot", 100, JumperCombatTestCases::zombieAttacksHullOfPilot),
            test("drone_hits_other_jumper", 300, JumperCombatTestCases::droneHitsOtherJumper),
            test("own_drone_explosion_spares_jumper", 100, JumperCombatTestCases::ownDroneExplosionSparesJumper),
            test("shield_makes_indestructible", 100, JumperCombatTestCases::shieldMakesIndestructible),
            test("survives_one_drone_not_two", 100, JumperCombatTestCases::survivesOneDroneNotTwo),
            test("breaks_and_drops_cargo", 100, JumperCombatTestCases::breaksAndDropsCargo),
            test("water_damages_hull", 120, JumperCombatTestCases::waterDamagesHull),
            test("shield_blocks_water", 120, JumperCombatTestCases::shieldBlocksWater),
            test("start_charge_and_no_drive_drain_when_landed", 100, JumperEnergyTestCases::startChargeAndNoDriveDrainWhenLanded),
            test("systems_drain_energy", 100, JumperEnergyTestCases::systemsDrainEnergy),
            test("shield_collapses_without_energy", 100, JumperEnergyTestCases::shieldCollapsesWithoutEnergy),
            test("charging_pad_charges", 60, JumperEnergyTestCases::chargingPadCharges),
            test("hovers_without_pilot", 100, JumperFlightTestCases::hoversWithoutPilot),
            test("solid_even_when_cloaked", 100, JumperFlightTestCases::solidEvenWhenCloaked),
            test("segments_follow_rotation", 100, JumperFlightTestCases::segmentsFollowRotation),
            test("edge_guidance_slips_through_opening", 100, JumperFlightTestCases::edgeGuidanceSlipsThroughOpening),
            test("edge_guidance_ignores_wide_miss", 100, JumperFlightTestCases::edgeGuidanceIgnoresWideMiss),
            test("ignores_knockback", 100, JumperFlightTestCases::ignoresKnockback),
            test("crashes_without_energy", 100, JumperFlightTestCases::crashesWithoutEnergy),
            test("impact_damage_curve", 100, JumperFlightTestCases::impactDamageCurve),
            test("lava_damages_hull", 120, JumperHazardTestCases::lavaDamagesHull),
            test("flowing_lava_from_above_damages_hull", 300, JumperHazardTestCases::flowingLavaFromAboveDamagesHull),
            test("shield_absorbs_lava", 120, JumperHazardTestCases::shieldAbsorbsLava),
            test("passenger_is_invulnerable", 100, JumperHazardTestCases::passengerIsInvulnerable),
            test("destruction_ejects_pilot_and_explodes", 100, JumperHazardTestCases::destructionEjectsPilotAndExplodes),
            test("lights_place_and_remove_blocks", 100, JumperLightTestCases::lightsPlaceAndRemoveBlocks),
            test("lights_follow_lazily", 60, JumperLightTestCases::lightsFollowLazily),
            test("jumper_changes_dimension_with_its_state", 100, JumperTravelTestCases::jumperChangesDimensionWithItsState),
            test("hull_segments_never_travel_alone", 100, JumperTravelTestCases::hullSegmentsNeverTravelAlone),
            test("seated_pilot_does_not_travel_alone", 100, JumperTravelTestCases::seatedPilotDoesNotTravelAlone),
            test("teleport_within_dimension_keeps_pilot", 100, JumperTravelTestCases::teleportWithinDimensionKeepsPilot),
            test("destroyed_segment_destroys_ship", 100, JumperTravelTestCases::destroyedSegmentDestroysShip));

    private McpjGameTests() {}

    // one entry per line: test("name", maxTicks, function), parsed by the build (see build.gradle)
    private static TestCase test(String name, int maxTicks, Consumer<GameTestHelper> function) {
        return new TestCase(name, maxTicks, function);
    }
}
