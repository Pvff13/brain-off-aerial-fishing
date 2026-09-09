package com.aerialfishinghighlighter;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Highlights the aerial fishing spot the player can catch from soonest, at Lake Molch,
 * ranked by zone rather than raw tile distance - two spots in the same zone (e.g. 3 and 4
 * tiles away, both the "2 tick" zone) are considered equally good, see
 * {@link #ticksToTravel}. Frenzied spots are always ranked as the "3 tick" zone regardless
 * of their actual distance, since their catch cycle is a flat 3 ticks once reached rather
 * than distance-dependent; within that shared zone a frenzied spot outranks a non-frenzied
 * one, but the 1- and 2-tick zones still outrank any frenzied spot - see
 * {@link #effectiveTicks} and {@link #compareSpots}. Whenever two spots end up tied after
 * all of that, the one that's been around the longest ranks ahead, since it's the one
 * closest to disappearing. The overlay highlights the best spot, plus the 2nd and 3rd best
 * if enabled in settings, and can show an estimated countdown of how much longer each one
 * has left (see {@link #estimatedTicksRemaining}).
 * <p>
 * A spot that's estimated not to survive the round trip - i.e. it'd likely despawn before
 * the cormorant could get there and catch - is ranked behind every spot that would survive
 * it, so the highlight moves on to the next best option instead of sending you to a spot
 * that's probably about to disappear. See {@link #getRankedSpots}.
 * <p>
 * Overlay-only: this plugin never clicks, moves the mouse, or interacts with anything. It
 * only subscribes to NPC/tick/game-state events and draws on top of the scene.
 */
// Class/package/config-group names still say "AerialFishingHighlighter" internally (kept
// from the original name) even though the user-visible display name below has changed -
// renaming the config group would silently reset everyone's saved settings, so only the
// display name actually changed. Same approach as RainbowCrabPlugin/"Crab Trapping Helper".
@Slf4j
@PluginDescriptor(
	name = "Brain off Aerial Fishing",
	description = "Highlights the aerial fishing spot you can catch from soonest by zone, ranking frenzied spots as the 3-tick zone, and breaking remaining ties by whichever spot spawned first",
	tags = {"fishing", "aerial fishing", "molch", "lake molch", "cormorant", "overlay", "highlight", "closest", "fishing spot"}
	// enabledByDefault intentionally omitted (defaults to true): the Plugin Hub's
	// automated review rejects enabledByDefault=false as confusing UX for something the
	// user explicitly installed.
)
public class AerialFishingHighlighterPlugin extends Plugin
{
	/**
	 * https://oldschool.runescape.wiki/w/Fishing_spot_(aerial_fishing) - "Fishing spot",
	 * NPC id 8523. This is the only id the wiki lists for the regular spot; unlike some
	 * other fishing spots it does not morph into a different id as it ages or empties, it
	 * simply despawns once its lifespan is up (and a new one spawns elsewhere).
	 */
	static final int NPC_FISHING_SPOT = 8523;

	/**
	 * https://oldschool.runescape.wiki/w/Frenzied_fishing_spot - a larger variant that has
	 * a chance to appear while aerial fishing, which auto-catches every 3 ticks without
	 * further input once you start fishing it. NPC id 16346.
	 */
	static final int NPC_FRENZIED_FISHING_SPOT = 16346;

	/**
	 * A fishing spot's lifespan isn't documented on the wiki and can't be observed directly
	 * (there's no visible countdown), so this range is taken from
	 * https://github.com/call-me-maple/aerial-fishing-timers - a plugin built specifically
	 * to time this - whose README states spots stay active for somewhere between 10 and 19
	 * ticks. The true lifespan of any given spot is unknowable in advance, so remaining-time
	 * estimates and the "will it survive the round trip" check both work off a single
	 * assumed value within this range - see {@link AerialFishingHighlighterConfig#assumedLifespanTicks}.
	 * Treat all of this as a best-effort estimate, not a confirmed game mechanic.
	 */
	static final int MIN_LIFESPAN_TICKS = 10;
	static final int MAX_LIFESPAN_TICKS = 19;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AerialFishingHighlighterOverlay overlay;

	@Inject
	private AerialFishingHighlighterConfig config;

	private final Map<NPC, AerialFishingSpot> spots = new LinkedHashMap<>();

	private boolean needsReconciliation;

	Collection<AerialFishingSpot> getSpots()
	{
		return spots.values();
	}

	@Provides
	AerialFishingHighlighterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AerialFishingHighlighterConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		spots.clear();
		// Spots may already be sitting on Lake Molch from before the plugin was enabled -
		// we can't know how long they've been there, so they're reconciled in on the next
		// tick rather than assumed to have just spawned. See AerialFishingSpot's javadoc.
		needsReconciliation = true;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		spots.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			spots.clear();
		}
		else if (state == GameState.LOGGED_IN)
		{
			needsReconciliation = true;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		trackIfEligible(event.getNpc(), client.getTickCount());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		spots.remove(event.getNpc());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (needsReconciliation)
		{
			reconcileExistingSpots();
			needsReconciliation = false;
		}
	}

	/**
	 * Returns every eligible spot, best first, using the ranking rules from the class
	 * javadoc. The overlay highlights however many of the leading entries the user has
	 * enabled (1st is always shown; 2nd/3rd are opt-in - see
	 * {@link AerialFishingHighlighterConfig}).
	 */
	List<AerialFishingSpot> getRankedSpots()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || spots.isEmpty())
		{
			return Collections.emptyList();
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		int currentTick = client.getTickCount();
		Map<AerialFishingSpot, Integer> ticksBySpot = new HashMap<>();
		Map<AerialFishingSpot, Boolean> viableBySpot = new HashMap<>();
		List<AerialFishingSpot> eligible = new ArrayList<>();

		for (AerialFishingSpot spot : spots.values())
		{
			if (spot.isFrenzied() && !config.includeFrenziedSpots())
			{
				continue;
			}

			// WorldPoint.distanceTo returns Integer.MAX_VALUE for a different plane, which
			// naturally excludes spots that aren't actually reachable/visible right now.
			int tileDistance = playerLocation.distanceTo(spot.getNpc().getWorldLocation());
			if (tileDistance == Integer.MAX_VALUE)
			{
				continue;
			}

			int ticks = effectiveTicks(spot.isFrenzied(), tileDistance);
			ticksBySpot.put(spot, ticks);
			viableBySpot.put(spot, estimatedTicksRemaining(spot, currentTick) >= ticks);
			eligible.add(spot);
		}

		eligible.sort((a, b) -> compareSpots(
			a, ticksBySpot.get(a), viableBySpot.get(a),
			b, ticksBySpot.get(b), viableBySpot.get(b)));
		return eligible;
	}

	/**
	 * Estimates how many more ticks a spot is likely to stick around, assuming it lives for
	 * {@link AerialFishingHighlighterConfig#assumedLifespanTicks} - the estimate can't be
	 * exact since the true lifespan is random and unobservable in advance (see
	 * {@link #MIN_LIFESPAN_TICKS}). Never negative: a spot estimated to already be overdue
	 * simply reads as 0 remaining.
	 */
	int estimatedTicksRemaining(AerialFishingSpot spot, int currentTick)
	{
		int elapsed = currentTick - spot.getSpawnTick();
		return Math.max(0, config.assumedLifespanTicks() - elapsed);
	}

	/**
	 * @return a negative number if {@code a} (ranked at {@code aTicks} from
	 * {@link #effectiveTicks}, and estimated to still be around by the time the cormorant
	 * gets there iff {@code aViable}) should rank ahead of {@code b} (same, but for
	 * {@code bTicks}/{@code bViable}), a positive number for the reverse, or zero if truly
	 * tied (which shouldn't happen in practice, since spawn ticks paired with object
	 * identity make every spot distinguishable).
	 */
	private int compareSpots(AerialFishingSpot a, int aTicks, boolean aViable, AerialFishingSpot b, int bTicks, boolean bViable)
	{
		// A spot that probably won't survive the round trip ranks behind every spot that
		// would, regardless of zone - no point steering towards a likely-wasted attempt
		// when a sure thing is available. If neither (or both) are viable, this changes
		// nothing and falls through to the normal zone-based ranking below.
		if (aViable != bViable)
		{
			return aViable ? -1 : 1;
		}

		if (aTicks != bTicks)
		{
			return Integer.compare(aTicks, bTicks);
		}

		// Same zone. The only way that's possible between a frenzied and a non-frenzied
		// spot is both landing in the shared "3 tick" zone (see effectiveTicks) - and
		// there, frenzied wins per explicit user request. Frenzied-vs-frenzied and
		// non-frenzied-vs-non-frenzied ties fall through to the oldest-wins rule below.
		if (a.isFrenzied() != b.isFrenzied())
		{
			return a.isFrenzied() ? -1 : 1;
		}

		return Integer.compare(a.getSpawnTick(), b.getSpawnTick());
	}

	/**
	 * Ranks a spot by zone, for comparison purposes: a frenzied spot is always ranked in
	 * the "3 tick" zone regardless of its actual distance, since once you start fishing
	 * one it just keeps auto-catching every 3 ticks - see {@link #NPC_FRENZIED_FISHING_SPOT}
	 * - while a non-frenzied spot is ranked by how long it actually takes to reach.
	 */
	static int effectiveTicks(boolean frenzied, int tileDistance)
	{
		return frenzied ? 3 : ticksToTravel(tileDistance);
	}

	/**
	 * Converts a tile distance (Chebyshev, i.e. {@link WorldPoint#distanceTo}) into the
	 * number of game ticks the cormorant takes to reach and catch from that spot.
	 * <p>
	 * Breakpoints as observed in-game: 1-2 tiles = 1 tick, 3-4 tiles = 2 ticks, 5 tiles = 3
	 * ticks, 6-7 tiles = 4 ticks, 8-9 tiles = 5 ticks, and it caps at 6 ticks for anything
	 * beyond that - it does not keep climbing indefinitely.
	 */
	static int ticksToTravel(int tileDistance)
	{
		if (tileDistance <= 2)
		{
			return 1;
		}
		if (tileDistance <= 4)
		{
			return 2;
		}
		if (tileDistance <= 5)
		{
			return 3;
		}
		if (tileDistance <= 7)
		{
			return 4;
		}
		if (tileDistance <= 9)
		{
			return 5;
		}
		return 6;
	}

	private void trackIfEligible(NPC npc, int currentTick)
	{
		int id = npc.getId();
		if (id != NPC_FISHING_SPOT && id != NPC_FRENZIED_FISHING_SPOT)
		{
			return;
		}

		spots.putIfAbsent(npc, new AerialFishingSpot(npc, currentTick, id == NPC_FRENZIED_FISHING_SPOT));
	}

	/**
	 * Scans the currently loaded scene for eligible fishing spots that already existed
	 * before this plugin started tracking them (plugin enabled, or logged in mid-session),
	 * and seeds the spot map from them exactly as a fresh NpcSpawned would.
	 */
	private void reconcileExistingSpots()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		WorldView worldView = localPlayer.getWorldView();
		int currentTick = client.getTickCount();
		for (NPC npc : worldView.npcs())
		{
			trackIfEligible(npc, currentTick);
		}
	}
}
