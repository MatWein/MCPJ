package de.mcpj.core.jumper;

import de.mcpj.core.math.BlockPosition;

/** Ready-made jumpers for tests of other packages. */
public final class TestJumpers {
    private TestJumpers() {}

    public static Jumper inLava() {
        FakeJumperHost host = new FakeJumperHost();
        host.lava.add(BlockPosition.containing(host.location.add(0, 0.5, 0)));
        Jumper jumper = new Jumper(host);
        jumper.onAdded();
        return jumper;
    }
}
