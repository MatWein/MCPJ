package de.mcpj.mc.client.navigation;

import de.mcpj.core.navigation.JumperNavigation;
import de.mcpj.core.navigation.NavigationAction;
import de.mcpj.core.navigation.Waypoint;
import de.mcpj.mc.client.ClientCompat;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.network.NavigationPacket;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The navigation console (key N, for everyone on board): save the current position as a waypoint, choose the
 * destination, rename and delete waypoints. Built from widgets only (no drawing of its own), so it is the same for
 * every Minecraft version. Changes go to the server; the list refreshes when the synced memory changes.
 */
public final class NavigationScreen extends Screen {
    private static final String KEY = "gui.mcpj.navigation.";
    private static final int WIDTH = 320;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int SMALL_BUTTON = 20;
    private static final int GAP = 2;
    private static final int TEXT_HEIGHT = 12;
    private static final int TOP = 12;
    private static final int ACTION_WIDTH = 120;
    private static final int RENAME_WIDTH = 58;
    private static final int PAGE_BUTTON = 24;
    private static final int FOOTER = 56;

    private final PuddleJumperEntity jumper;
    private String shownNavigation = "";
    private int page;
    /** Index of the waypoint being renamed, or NONE. */
    private int renaming = JumperNavigation.NONE;
    private String typedName = "";

    private NavigationScreen(PuddleJumperEntity jumper) {
        super(Component.translatable(KEY + "title"));
        this.jumper = jumper;
    }

    public static void open(PuddleJumperEntity jumper) {
        ClientCompat.setScreen(new NavigationScreen(jumper));
    }

    @Override
    protected void init() {
        JumperNavigation navigation = jumper.core().navigation();
        shownNavigation = jumper.navigation();
        List<Waypoint> waypoints = navigation.waypoints();
        if (renaming >= waypoints.size()) renaming = JumperNavigation.NONE;
        int left = (width - WIDTH) / 2;
        int y = TOP;

        addRenderableWidget(new StringWidget(left, y, WIDTH, TEXT_HEIGHT, title, font));
        y += TEXT_HEIGHT + GAP * 2;
        y = addNameRow(left, y, navigation);

        int rows = Math.max(1, (height - y - FOOTER) / ROW_HEIGHT);
        int pages = Math.max(1, (waypoints.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        if (waypoints.isEmpty()) {
            addRenderableWidget(new StringWidget(left, y + GAP, WIDTH, TEXT_HEIGHT,
                    Component.translatable(KEY + "empty").withStyle(ChatFormatting.GRAY), font));
        }
        String dimension = McCompat.dimensionId(jumper.level());
        for (int i = page * rows; i < Math.min(waypoints.size(), (page + 1) * rows); i++) {
            addWaypointRow(left, y, i, waypoints.get(i), i == navigation.selectedIndex(), dimension);
            y += ROW_HEIGHT;
        }

        int bottom = height - FOOTER + GAP * 4;
        if (pages > 1) addPageButtons(bottom, pages);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - ACTION_WIDTH) / 2, bottom + ROW_HEIGHT, ACTION_WIDTH, BUTTON_HEIGHT).build());
    }

    /** Name field with "save position" (or "rename" and "cancel" while renaming). */
    private int addNameRow(int left, int y, JumperNavigation navigation) {
        int actionWidth = renaming == JumperNavigation.NONE ? ACTION_WIDTH : RENAME_WIDTH * 2 + GAP;
        EditBox name = new EditBox(font, left, y, WIDTH - actionWidth - GAP, BUTTON_HEIGHT, Component.translatable(KEY + "name_hint"));
        name.setMaxLength(JumperNavigation.MAX_NAME_LENGTH);
        name.setValue(typedName);
        name.setHint(Component.translatable(KEY + "name_hint").withStyle(ChatFormatting.DARK_GRAY));
        name.setResponder(value -> typedName = value);
        addRenderableWidget(name);
        int x = left + WIDTH - actionWidth;
        if (renaming == JumperNavigation.NONE) {
            Button add = addRenderableWidget(Button.builder(Component.translatable(KEY + "add"), button -> addWaypoint(navigation))
                    .bounds(x, y, ACTION_WIDTH, BUTTON_HEIGHT).build());
            add.active = !navigation.isFull();
            if (navigation.isFull()) add.setTooltip(Tooltip.create(Component.translatable(KEY + "full", JumperNavigation.MAX_WAYPOINTS)));
        } else {
            addRenderableWidget(Button.builder(Component.translatable(KEY + "rename"), button -> finishRenaming())
                    .bounds(x, y, RENAME_WIDTH, BUTTON_HEIGHT).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> stopRenaming())
                    .bounds(x + RENAME_WIDTH + GAP, y, RENAME_WIDTH, BUTTON_HEIGHT).build());
        }
        return y + BUTTON_HEIGHT + GAP * 3;
    }

    /** One waypoint: a button that makes it the destination (or clears it), rename and delete. */
    private void addWaypointRow(int left, int y, int index, Waypoint waypoint, boolean destination, String dimension) {
        boolean here = waypoint.dimension().equals(dimension);
        MutableComponent label = Component.literal((destination ? "► " : "") + waypoint.name() + "  ")
                .append(Component.literal(waypoint.x() + " / " + waypoint.y() + " / " + waypoint.z()).withStyle(ChatFormatting.GRAY));
        if (destination) label.withStyle(ChatFormatting.GOLD);
        MutableComponent tooltip = Component.translatable(KEY + (destination ? "clear_tooltip" : "select_tooltip"));
        if (!here) tooltip.append(Component.literal(" (" + waypoint.dimension() + ")").withStyle(ChatFormatting.GRAY));
        int nameWidth = WIDTH - 2 * (SMALL_BUTTON + GAP);
        Button select = Button.builder(label, button -> send(NavigationAction.SELECT, destination ? JumperNavigation.NONE : index, ""))
                .bounds(left, y, nameWidth, BUTTON_HEIGHT).tooltip(Tooltip.create(tooltip)).build();
        addRenderableWidget(select);
        addRenderableWidget(Button.builder(Component.literal("✎"), button -> startRenaming(index, waypoint))
                .bounds(left + nameWidth + GAP, y, SMALL_BUTTON, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(KEY + "rename"))).build());
        addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), button -> delete(index))
                .bounds(left + WIDTH - SMALL_BUTTON, y, SMALL_BUTTON, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(KEY + "delete"))).build());
    }

    private void addPageButtons(int y, int pages) {
        int center = width / 2;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1))
                .bounds(center - PAGE_BUTTON * 2, y, PAGE_BUTTON, BUTTON_HEIGHT).build());
        previous.active = page > 0;
        addRenderableWidget(new StringWidget(center - PAGE_BUTTON, y, PAGE_BUTTON * 2, BUTTON_HEIGHT,
                Component.translatable(KEY + "page", page + 1, pages), font));
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1))
                .bounds(center + PAGE_BUTTON, y, PAGE_BUTTON, BUTTON_HEIGHT).build());
        next.active = page < pages - 1;
    }

    private void addWaypoint(JumperNavigation navigation) {
        String name = typedName.isBlank()
                ? Component.translatable(KEY + "default_name", navigation.waypoints().size() + 1).getString() : typedName;
        send(NavigationAction.ADD, JumperNavigation.NONE, name);
        typedName = "";
        rebuildWidgets();
    }

    private void startRenaming(int index, Waypoint waypoint) {
        renaming = index;
        typedName = waypoint.name();
        rebuildWidgets();
    }

    private void finishRenaming() {
        if (!typedName.isBlank()) send(NavigationAction.RENAME, renaming, typedName);
        stopRenaming();
    }

    private void stopRenaming() {
        renaming = JumperNavigation.NONE;
        typedName = "";
        rebuildWidgets();
    }

    private void delete(int index) {
        if (renaming == index) {
            renaming = JumperNavigation.NONE;
            typedName = "";
        }
        send(NavigationAction.DELETE, index, "");
    }

    private void turnPage(int direction) {
        page += direction;
        rebuildWidgets();
    }

    private static void send(NavigationAction action, int index, String name) {
        McpjPlatform.get().sendToServer(new NavigationPacket(action, index, name));
    }

    @Override
    public void tick() {
        super.tick();
        Player player = Minecraft.getInstance().player;
        if (player == null || player.getVehicle() != jumper || !jumper.isAlive()) {
            onClose();
        } else if (!jumper.navigation().equals(shownNavigation)) {
            rebuildWidgets();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
