package com.aerialfishinghighlighter;

/** How the estimated-time-left countdown is worded - see {@link AerialFishingHighlighterConfig#lifespanFormat()}. */
public enum AerialFishingLifespanFormat
{
	TICKS_LEFT("Ticks left (e.g. \"~7 ticks left\")"),
	NUMBER_ONLY("Number only (e.g. \"~7\")");

	private final String label;

	AerialFishingLifespanFormat(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
