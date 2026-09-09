package com.aerialfishinghighlighter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * A proper (draggable, sized-to-fit-the-text) panel for reboost reminders set to
 * {@link AerialFishingBoostInfoboxStyle#CUSTOM_TEXT} - a fixed ~35px infobox icon isn't built
 * to hold a full sentence, so custom text gets this instead: one line per active
 * custom-text reminder, custom text on the left and the current boost margin on the right,
 * both always visible with no hovering needed for either. Reminders left on
 * {@link AerialFishingBoostInfoboxStyle#MARGIN} still use {@link AerialFishingBoostInfoBox}
 * and never appear here.
 */
class AerialFishingBoostTextOverlay extends Overlay
{
	private final AerialFishingHighlighterPlugin plugin;
	private final AerialFishingHighlighterConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	AerialFishingBoostTextOverlay(AerialFishingHighlighterPlugin plugin, AerialFishingHighlighterConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		boolean any = false;

		if (plugin.isFishingReboostNeeded() && config.fishingReboostInfobox()
			&& config.fishingReboostInfoboxStyle() == AerialFishingBoostInfoboxStyle.CUSTOM_TEXT)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(config.fishingReboostText())
				.right("+" + plugin.getFishingBoostMargin())
				.build());
			any = true;
		}

		if (plugin.isHunterReboostNeeded() && config.hunterReboostInfobox()
			&& config.hunterReboostInfoboxStyle() == AerialFishingBoostInfoboxStyle.CUSTOM_TEXT)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(config.hunterReboostText())
				.right("+" + plugin.getHunterBoostMargin())
				.build());
			any = true;
		}

		return any ? panelComponent.render(graphics) : null;
	}
}
