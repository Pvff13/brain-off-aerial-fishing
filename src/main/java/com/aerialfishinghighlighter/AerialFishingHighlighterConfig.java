package com.aerialfishinghighlighter;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
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

	@ConfigSection(
		name = "Fishing reboost reminder",
		description = "Reminds you to reboost Fishing when your current boost drops low",
		position = 14,
		closedByDefault = false
	)
	String fishingReboostSection = "fishingReboostSection";

	@ConfigItem(
		position = 0,
		keyName = "fishingReboostEnabled",
		name = "Enable reminder",
		description = "Remind you to reboost Fishing once your current boost drops below the threshold below. Only shows up while you're wearing the Cormorant's glove (i.e. actually aerial fishing), never otherwise. Off by default - this is a separate feature from the fishing spot highlighter above",
		section = "fishingReboostSection"
	)
	default boolean fishingReboostEnabled()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "fishingReboostThreshold",
		name = "Remind below",
		description = "Remind you once your Fishing boost (how many levels above your base level you currently are) drops below this. 6 is the maximum Fishing boost possible",
		section = "fishingReboostSection"
	)
	@Range(min = 0, max = 6)
	default int fishingReboostThreshold()
	{
		return 3;
	}

	@ConfigItem(
		position = 2,
		keyName = "fishingReboostInfobox",
		name = "Show infobox",
		description = "Show a reminder infobox while your Fishing boost is below the threshold",
		section = "fishingReboostSection"
	)
	default boolean fishingReboostInfobox()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "fishingReboostHighlightItems",
		name = "Highlight boost items",
		description = "Highlight anything in your inventory or worn that can reboost Fishing (potions, capes, harpoons, pies, mixes - see the README for the full list), while your Fishing boost is below the threshold",
		section = "fishingReboostSection"
	)
	default boolean fishingReboostHighlightItems()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "fishingReboostHighlightColor",
		name = "Highlight colour",
		description = "Highlight colour used for Fishing-reboosting items",
		section = "fishingReboostSection"
	)
	default Color fishingReboostHighlightColor()
	{
		return new Color(0, 200, 255, 160);
	}

	@ConfigItem(
		position = 5,
		keyName = "fishingReboostInfoboxStyle",
		name = "Infobox style",
		description = "What the Fishing reminder infobox shows as its main, always-visible label. Either way, hovering it shows the other piece of information as a tooltip",
		section = "fishingReboostSection"
	)
	default AerialFishingBoostInfoboxStyle fishingReboostInfoboxStyle()
	{
		return AerialFishingBoostInfoboxStyle.MARGIN;
	}

	@ConfigItem(
		position = 6,
		keyName = "fishingReboostText",
		name = "Custom text",
		description = "Used as the infobox's main visible label when the style above is set to \"Custom text\" - otherwise it's just the tooltip you get from hovering the icon. Infoboxes are small, so keep this short",
		section = "fishingReboostSection"
	)
	default String fishingReboostText()
	{
		return "Fishing boost is low - reboost!";
	}

	@ConfigSection(
		name = "Hunter reboost reminder",
		description = "Reminds you to reboost Hunter when your current boost drops low",
		position = 15,
		closedByDefault = false
	)
	String hunterReboostSection = "hunterReboostSection";

	@ConfigItem(
		position = 0,
		keyName = "hunterReboostEnabled",
		name = "Enable reminder",
		description = "Remind you to reboost Hunter once your current boost drops below the threshold below. Only shows up while you're wearing the Cormorant's glove (i.e. actually aerial fishing), never otherwise. Off by default - this is a separate feature from the fishing spot highlighter above",
		section = "hunterReboostSection"
	)
	default boolean hunterReboostEnabled()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "hunterReboostThreshold",
		name = "Remind below",
		description = "Remind you once your Hunter boost (how many levels above your base level you currently are) drops below this. 6 is the maximum Hunter boost possible",
		section = "hunterReboostSection"
	)
	@Range(min = 0, max = 6)
	default int hunterReboostThreshold()
	{
		return 3;
	}

	@ConfigItem(
		position = 2,
		keyName = "hunterReboostInfobox",
		name = "Show infobox",
		description = "Show a reminder infobox while your Hunter boost is below the threshold",
		section = "hunterReboostSection"
	)
	default boolean hunterReboostInfobox()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "hunterReboostHighlightItems",
		name = "Highlight boost items",
		description = "Highlight anything in your inventory or worn that can reboost Hunter (potions, capes, drinks, mixes - see the README for the full list), while your Hunter boost is below the threshold",
		section = "hunterReboostSection"
	)
	default boolean hunterReboostHighlightItems()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "hunterReboostHighlightColor",
		name = "Highlight colour",
		description = "Highlight colour used for Hunter-reboosting items",
		section = "hunterReboostSection"
	)
	default Color hunterReboostHighlightColor()
	{
		return new Color(255, 0, 200, 160);
	}

	@ConfigItem(
		position = 5,
		keyName = "hunterReboostInfoboxStyle",
		name = "Infobox style",
		description = "What the Hunter reminder infobox shows as its main, always-visible label. Either way, hovering it shows the other piece of information as a tooltip",
		section = "hunterReboostSection"
	)
	default AerialFishingBoostInfoboxStyle hunterReboostInfoboxStyle()
	{
		return AerialFishingBoostInfoboxStyle.MARGIN;
	}

	@ConfigItem(
		position = 6,
		keyName = "hunterReboostText",
		name = "Custom text",
		description = "Used as the infobox's main visible label when the style above is set to \"Custom text\" - otherwise it's just the tooltip you get from hovering the icon. Infoboxes are small, so keep this short",
		section = "hunterReboostSection"
	)
	default String hunterReboostText()
	{
		return "Hunter boost is low - reboost!";
	}
}
