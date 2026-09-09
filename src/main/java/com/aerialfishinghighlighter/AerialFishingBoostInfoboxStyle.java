package com.aerialfishinghighlighter;

/** What a reboost reminder infobox shows as its main, always-visible label - see {@link AerialFishingHighlighterConfig#fishingReboostInfoboxStyle}. */
public enum AerialFishingBoostInfoboxStyle
{
	// Kept short deliberately - the dropdown clips longer labels (the full explanation
	// already lives in AerialFishingHighlighterConfig's fishingReboostInfoboxStyle/
	// hunterReboostInfoboxStyle @ConfigItem description instead).
	MARGIN("Icon + boost margin"),
	CUSTOM_TEXT("Custom text");

	private final String label;

	AerialFishingBoostInfoboxStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
