package com.aerialfishinghighlighter;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("aerialfishinghighlighter")
public interface AerialFishingHighlighterConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "overlayEnabled",
		name = "Enable overlay",
		description = "Master switch for the fishing spot highlight overlay"
	)
	default boolean overlayEnabled()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "highlightColor",
		name = "Highlight colour",
		description = "Highlight colour for the fishing spot you can catch from soonest (ties broken by whichever spot is oldest)"
	)
	default Color highlightColor()
	{
		return new Color(0, 255, 0, 130);
	}

	@ConfigItem(
		position = 2,
		keyName = "highlightSecondBest",
		name = "Highlight 2nd best spot",
		description = "Also highlight the second-best spot to fish next, using the colour below"
	)
	default boolean highlightSecondBest()
	{
		return false;
	}

	@ConfigItem(
		position = 3,
		keyName = "secondBestColor",
		name = "2nd best colour",
		description = "Highlight colour for the second-best spot to fish next"
	)
	default Color secondBestColor()
	{
		return new Color(255, 255, 0, 130);
	}

	@ConfigItem(
		position = 4,
		keyName = "highlightThirdBest",
		name = "Highlight 3rd best spot",
		description = "Also highlight the third-best spot to fish next, using the colour below"
	)
	default boolean highlightThirdBest()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = "thirdBestColor",
		name = "3rd best colour",
		description = "Highlight colour for the third-best spot to fish next"
	)
	default Color thirdBestColor()
	{
		return new Color(255, 140, 0, 130);
	}

	@ConfigItem(
		position = 6,
		keyName = "includeFrenziedSpots",
		name = "Include frenzied spots",
		description = "Let frenzied fishing spots (the larger, auto-catching spots that occasionally appear) be picked as the highlighted spot. Frenzied spots always rank as the 3-tick zone: a 1- or 2-tick spot still wins over them, but they win over a plain 3-tick spot"
	)
	default boolean includeFrenziedSpots()
	{
		return true;
	}

	@ConfigItem(
		position = 7,
		keyName = "showDistanceText",
		name = "Show travel ticks",
		description = "Show the number of game ticks it'll take to catch from each highlighted spot"
	)
	default boolean showDistanceText()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "showLifespanText",
		name = "Show estimated time left",
		description = "Show a rough countdown of how much longer each highlighted spot has before it likely despawns. This is a best-effort estimate (spot lifespans are randomised and not exactly observable), not an exact timer"
	)
	default boolean showLifespanText()
	{
		return false;
	}

	@ConfigItem(
		position = 9,
		keyName = "assumedLifespanTicks",
		name = "Assumed lifespan (ticks)",
		description = "A spot's real lifespan is randomised and can't be known exactly - it's been observed staying up anywhere from 10 to 19 ticks. This is the value assumed for the estimated-time-left countdown and for deciding whether a spot will survive the round trip; raise it for a more optimistic estimate, lower it for a more cautious one"
	)
	@Range(min = 10, max = 19)
	default int assumedLifespanTicks()
	{
		return 14;
	}

	@ConfigItem(
		position = 10,
		keyName = "lifespanFormat",
		name = "Time left format",
		description = "How the estimated-time-left countdown is worded"
	)
	default AerialFishingLifespanFormat lifespanFormat()
	{
		return AerialFishingLifespanFormat.TICKS_LEFT;
	}

	@ConfigItem(
		position = 11,
		keyName = "showOtherSpots",
		name = "Outline other spots",
		description = "Faintly outline every other tracked fishing spot, not just the highlighted one(s)"
	)
	default boolean showOtherSpots()
	{
		return false;
	}

	@ConfigItem(
		position = 12,
		keyName = "otherSpotsColor",
		name = "Other spots colour",
		description = "Outline colour used for tracked fishing spots that aren't a highlighted one"
	)
	default Color otherSpotsColor()
	{
		return new Color(255, 255, 255, 60);
	}

	@ConfigItem(
		position = 13,
		keyName = "fillClickbox",
		name = "Fill clickbox with colour",
		description = "Fill each spot's whole clickbox with its own colour, instead of just outlining it with a generic dark tint"
	)
	default boolean fillClickbox()
	{
		return false;
	}
}
