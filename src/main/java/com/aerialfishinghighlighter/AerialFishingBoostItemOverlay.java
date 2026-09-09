package com.aerialfishinghighlighter;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Highlights any inventory or equipped item that can reboost Fishing or Hunter (see
 * {@link AerialFishingBoostItems}), but only while that skill's boost has actually dropped
 * below its configured threshold - see {@link AerialFishingHighlighterPlugin}'s
 * {@code StatChanged} handling.
 */
class AerialFishingBoostItemOverlay extends WidgetItemOverlay
{
	private final AerialFishingHighlighterPlugin plugin;
	private final AerialFishingHighlighterConfig config;

	@Inject
	AerialFishingBoostItemOverlay(AerialFishingHighlighterPlugin plugin, AerialFishingHighlighterConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Color color = colorFor(itemId);
		if (color != null)
		{
			OverlayUtil.renderPolygon(graphics, widgetItem.getCanvasBounds(), color);
		}
	}

	private Color colorFor(int itemId)
	{
		if (plugin.isFishingReboostNeeded() && config.fishingReboostHighlightItems() && AerialFishingBoostItems.FISHING.contains(itemId))
		{
			return config.fishingReboostHighlightColor();
		}
		if (plugin.isHunterReboostNeeded() && config.hunterReboostHighlightItems() && AerialFishingBoostItems.HUNTER.contains(itemId))
		{
			return config.hunterReboostHighlightColor();
		}
		return null;
	}
}
