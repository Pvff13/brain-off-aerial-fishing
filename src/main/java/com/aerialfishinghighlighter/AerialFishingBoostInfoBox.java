package com.aerialfishinghighlighter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * A "your boost is running low" reminder infobox for one skill (Fishing or Hunter), used only
 * when that skill's reminder is set to {@link AerialFishingBoostInfoboxStyle#MARGIN} - the
 * {@code CUSTOM_TEXT} style uses {@link AerialFishingBoostTextOverlay} instead, since a full
 * sentence doesn't fit on a ~35px icon. Both the main label (the boost margin, e.g. "+2") and
 * the tooltip (the custom reminder text) are read live from the given suppliers on every
 * render, rather than cached, so they stay accurate as the boost decays without this plugin
 * having to rebuild the infobox itself.
 */
class AerialFishingBoostInfoBox extends InfoBox
{
	private final Supplier<String> textSupplier;
	private final Supplier<String> tooltipSupplier;

	AerialFishingBoostInfoBox(BufferedImage image, Plugin plugin, Supplier<String> textSupplier, Supplier<String> tooltipSupplier)
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
