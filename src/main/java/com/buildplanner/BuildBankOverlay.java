package com.buildplanner;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Reads an immutable set published by the EDT; never touches Swing or planner state. */
public final class BuildBankOverlay extends WidgetItemOverlay
{
    private final BuildPlannerPlugin plugin;
    private final BuildPlannerConfig config;
    @Inject BuildBankOverlay(BuildPlannerPlugin plugin, BuildPlannerConfig config)
    {
        this.plugin = plugin; this.config = config;
        drawAfterLayer(InterfaceID.Bankmain.ITEMS);
    }
    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
    {
        if (!config.highlightBank() || !plugin.bankItems().contains(itemId) || item.getQuantity() <= 0) return;
        Rectangle bounds = item.getCanvasBounds();
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(new Color(208, 178, 112, 45));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(new Color(208, 178, 112));
            g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        } finally { g.dispose(); }
    }
}
