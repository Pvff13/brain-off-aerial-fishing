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
 * A draggable stats panel for the catches-per-hour tracker - just "Caught fish:"/"Fish/hr:"
 * rows, no title - rather than a cramped infobox icon.
 */
class AerialFishingCatchRatePanelOverlay extends Overlay
{
	private final AerialFishingHighlighterPlugin plugin;
	private final AerialFishingHighlighterConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	AerialFishingCatchRatePanelOverlay(AerialFishingHighlighterPlugin plugin, AerialFishingHighlighterConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showCatchRateInfobox() || plugin.getFishCaughtCount() == 0)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Caught fish:")
			.right(String.valueOf(plugin.getFishCaughtCount()))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Fish/hr:")
			.right(String.valueOf(Math.round(plugin.getCatchesPerHour())))
			.build());

		return panelComponent.render(graphics);
	}
}
