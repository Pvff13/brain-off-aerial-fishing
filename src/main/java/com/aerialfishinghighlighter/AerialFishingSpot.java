package com.aerialfishinghighlighter;

import net.runelite.api.NPC;

/**
 * One tracked aerial fishing spot NPC, plus the bookkeeping needed to pick a winner when
 * two or more spots are tied for closest, and to estimate how much longer it'll stick
 * around.
 */
class AerialFishingSpot
{
	private final NPC npc;
	private final int spawnTick;
	private final boolean frenzied;

	/**
	 * @param spawnTick the {@code Client.getTickCount()} value when this spot was first
	 * observed (see {@link AerialFishingHighlighterPlugin#trackIfEligible}), used both to
	 * break a distance tie in favour of whichever spot is older, and to estimate remaining
	 * lifespan (see {@link AerialFishingHighlighterPlugin#estimatedTicksRemaining}). Spots
	 * already on Lake Molch before the plugin started tracking (startup/login
	 * reconciliation) get the reconciliation tick itself, which is the best available
	 * approximation but means their true age - and thus true remaining lifespan - may
	 * actually be less than what's computed from it.
	 */
	AerialFishingSpot(NPC npc, int spawnTick, boolean frenzied)
	{
		this.npc = npc;
		this.spawnTick = spawnTick;
		this.frenzied = frenzied;
	}

	NPC getNpc()
	{
		return npc;
	}

	int getSpawnTick()
	{
		return spawnTick;
	}

	boolean isFrenzied()
	{
		return frenzied;
	}
}
