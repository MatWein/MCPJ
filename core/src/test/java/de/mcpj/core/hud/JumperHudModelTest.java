package de.mcpj.core.hud;

import de.mcpj.core.config.DefaultConfigBuilder;
import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.TestJumpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumperHudModelTest {
    @BeforeEach
    void setUp() {
        DefaultConfigBuilder.useDefaults();
    }

    @Test
    void showsAllSystemsAndWarnsAboutLava() {
        Jumper jumper = TestJumpers.inLava();
        List<HudLine> lines = JumperHudModel.lines(jumper, 0, 64.7, true, null);
        assertEquals(11, lines.size());
        HudLine hull = lines.get(3);
        assertTrue(hull.parts().stream().anyMatch(part -> part instanceof HudText.Translated t && t.key().equals("hud.mcpj.lava_warning")));
        assertEquals(HudColors.RED, hull.color(), "warning blinks red");
        assertEquals(new HudText.Literal("64"), ((HudText.Translated) lines.get(2).parts().get(0)).args()[0]);
    }
}
