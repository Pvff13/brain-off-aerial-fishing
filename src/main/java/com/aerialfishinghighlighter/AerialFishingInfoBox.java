package com.aerialfishinghighlighter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * A generic supplier-driven infobox, reused for every simple text infobox this plugin
 * shows - the Fishing/Hunter "boost is low" reminders (when set to
 * {@link AerialFishingBoostInfoboxStyle#MARGIN} - the {@code CUSTOM_TEXT} style uses
 * {@link AerialFishingBoostTextOverlay} instead, since a full sentence doesn't fit on a
 * ~35px icon) and the catches-per-hour tracker. Both the main label and the tooltip are
 * read live from the given suppliers on every render, rather than cached, so they stay
 * accurate without this plugin having to rebuild the infobox itself.
 */
class AerialFishingInfoBox extends InfoBox
{
	private final Supplier<String> textSupplier;
	private final Supplier<String> tooltipSupplier;

	AerialFishingInfoBox(BufferedImage image, Plugin plugin, Supplier<String> textSupplier, Supplier<String> tooltipSupplier)
	{
		super(image, plugin);
		this.textSupplier = textSupplier;
		this.tooltipSupplier = tooltipSupplier;
	}

	@Override
	public String getText()
	{
		return textSupplier.get();
	}

	@Override
	public String getTooltip()
	{
		return tooltipSupplier.get();
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}
}
