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
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

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
 * Separately (and independently toggled - see {@link AerialFishingHighlighterConfig}), this
 * plugin can also remind you to reboost Fishing and/or Hunter once your current boost on
 * that skill drops below a threshold you set, via an infobox, a one-off chat message the
 * moment it crosses the threshold, and/or by highlighting any reboosting item you're
 * carrying or wearing (see {@link AerialFishingBoostItems}). Both reminders only ever show
 * up while you're actually aerial fishing (wearing the Cormorant's glove - see
 * {@link #isWearingCormorantsGlove}), never while doing something unrelated elsewhere in
 * the game.
 * <p>
 * There's also an optional catches-per-hour stats panel (see
 * {@link AerialFishingHighlighterConfig#showCatchRateInfobox} and
 * {@link AerialFishingCatchRatePanelOverlay}) - it detects a catch the same way RuneLite's
 * own built-in Fishing plugin does (see {@link #onChatMessage}), and rates it against
 * elapsed time since the first catch this session, resetting on login/hop like everything
 * else above.
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

	/**
	 * Unlike a regular spot's randomised 10-19 tick lifespan, a frenzied spot's lifespan is
	 * fixed at 28 ticks - used both for {@link AerialFishingHighlighterConfig#showFrenziedTimer}
	 * and, unconditionally, in {@link #estimatedTicksRemaining} so the survives-the-round-trip
	 * ranking check is accurate for frenzied spots regardless of that display toggle.
	 */
	static final int FRENZIED_LIFESPAN_TICKS = 28;

	/**
	 * https://oldschool.runescape.wiki/w/Cormorant%27s_glove - worn in the weapon slot while
	 * aerial fishing; morphs between these two ids depending on whether the cormorant is
	 * currently out on a catch. Either one equipped is used as "the player is actually aerial
	 * fishing right now", to gate the boost reminders below - see
	 * {@link #isWearingCormorantsGlove}.
	 */
	static final int CORMORANTS_GLOVE_NO_BIRD = 22816;
	static final int CORMORANTS_GLOVE_BIRD = 22817;

	/**
	 * The exact chat line RuneLite's own built-in Fishing plugin matches (as one alternative
	 * in its {@code FISHING_CATCH_REGEX}) to detect an aerial catch - see
	 * {@link #onChatMessage}.
	 */
	static final String AERIAL_FISHING_CATCH_MESSAGE = "Your cormorant returns with its catch.";

	/**
	 * Same idea as RuneLite's own built-in Fishing plugin's configurable session timeout
	 * ({@code FishingConfig.statTimeout}, checked every tick against time since the last
	 * catch) - after 5 minutes with no catch, the catches-per-hour session is stale and
	 * gets reset (see {@link #onGameTick}), rather than left showing a rate that no longer
	 * reflects what you're currently doing.
	 */
	static final int CATCH_RATE_IDLE_TIMEOUT_TICKS = 500;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AerialFishingHighlighterOverlay overlay;

	@Inject
	private AerialFishingBoostItemOverlay boostItemOverlay;

	@Inject
	private AerialFishingBoostTextOverlay boostTextOverlay;

	@Inject
	private AerialFishingCatchRatePanelOverlay catchRatePanelOverlay;

	@Inject
	private AerialFishingHighlighterConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpriteManager spriteManager;

	private final Map<NPC, AerialFishingSpot> spots = new LinkedHashMap<>();

	private boolean needsReconciliation;

	private boolean fishingReboostNeeded;
	private boolean hunterReboostNeeded;
	// Tracks the previous refresh's outcome purely to detect the "just crossed below the
	// threshold" edge for the chat message - reset to false whenever the reminder is
	// disabled/ungloved (see the early-return branches below), so re-enabling while still
	// low sends a fresh message rather than staying silent because it was "already needed".
	private boolean fishingReboostNeededPrev;
	private boolean hunterReboostNeededPrev;
	private AerialFishingInfoBox fishingBoostInfoBox;
	private AerialFishingInfoBox hunterBoostInfoBox;

	// -1 means no catch observed yet this session - the rate is undefined until the first
	// one, rather than measured from whenever the glove went on (dead time standing around
	// before your first catch shouldn't count against your rate).
	private int fishCaughtCount;
	private int firstCatchTick = -1;
	// Dedups the chat-message and Fishing-xp signals (see recordCatchIfAerialFishing) so a
	// single catch that fires both never gets counted twice.
	private int lastCatchTick = -1;
	// -1 means "haven't seen a Fishing StatChanged yet this session". RuneLite fires one for
	// every skill right at login purely to report your current xp from the server, not
	// because anything actually changed - treating that synthetic event as a real gain (see
	// onStatChanged) miscounted a catch before the player had caught anything. Tracking the
	// actual xp value lets a genuine increase be told apart from that initial sync.
	private int lastKnownFishingXp = -1;

	Collection<AerialFishingSpot> getSpots()
	{
		return spots.values();
	}

	int getFishCaughtCount()
	{
		return fishCaughtCount;
	}

	boolean isFishingReboostNeeded()
	{
		return fishingReboostNeeded;
	}

	boolean isHunterReboostNeeded()
	{
		return hunterReboostNeeded;
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
		overlayManager.add(boostItemOverlay);
		overlayManager.add(boostTextOverlay);
		overlayManager.add(catchRatePanelOverlay);
		spots.clear();
		// Spots may already be sitting on Lake Molch from before the plugin was enabled -
		// we can't know how long they've been there, so they're reconciled in on the next
		// tick rather than assumed to have just spawned. See AerialFishingSpot's javadoc.
		needsReconciliation = true;
		resetCatchRateSession();
		lastKnownFishingXp = -1;
		refreshFishingReboost();
		refreshHunterReboost();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(boostItemOverlay);
		overlayManager.remove(boostTextOverlay);
		overlayManager.remove(catchRatePanelOverlay);
		spots.clear();
		clearFishingBoostInfoBox();
		clearHunterBoostInfoBox();
		fishingReboostNeeded = false;
		hunterReboostNeeded = false;
		fishingReboostNeededPrev = false;
		hunterReboostNeededPrev = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			spots.clear();
			clearFishingBoostInfoBox();
			clearHunterBoostInfoBox();
			fishingReboostNeeded = false;
			hunterReboostNeeded = false;
			fishingReboostNeededPrev = false;
			hunterReboostNeededPrev = false;
			resetCatchRateSession();
			lastKnownFishingXp = -1;
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

		if (lastCatchTick != -1 && client.getTickCount() - lastCatchTick >= CATCH_RATE_IDLE_TIMEOUT_TICKS)
		{
			resetCatchRateSession();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		if (skill == Skill.FISHING)
		{
			// Belt-and-braces alongside onChatMessage below: a genuine xp gain while gloved
			// is also a reliable "one catch happened" signal on its own, and catches the
			// case where the exact chat line/type ever doesn't match (observed happening -
			// see recordCatchIfAerialFishing's tick-based dedup for why this can't
			// double-count against the chat message for the same catch).
			//
			// RuneLite fires one StatChanged per skill right at login purely to report your
			// current xp from the server - not a real gain - so the very first one seen
			// each session is recorded as the baseline and never itself counted as a catch;
			// only a genuine increase past that baseline counts (see lastKnownFishingXp's
			// field comment). Without this, logging in already wearing the glove miscounted
			// that sync event as catch #1 before anything was actually caught.
			int xp = event.getXp();
			if (lastKnownFishingXp == -1)
			{
				lastKnownFishingXp = xp;
			}
			else if (xp > lastKnownFishingXp)
			{
				lastKnownFishingXp = xp;
				recordCatchIfAerialFishing();
			}
			refreshFishingReboost();
		}
		else if (skill == Skill.HUNTER)
		{
			refreshHunterReboost();
		}
	}

	/**
	 * Mirrors the catch-detection signal RuneLite's own built-in Fishing plugin uses
	 * ({@code FishingPlugin.onChatMessage} matches a {@link ChatMessageType#SPAM} message
	 * against its aerial-specific catch line) - but checks message content only, not the
	 * exact type or an exact full-string match, since a stricter version of this check was
	 * observed not firing in practice (possibly a type or formatting difference this
	 * plugin's isolated re-implementation doesn't reproduce exactly). {@link #onStatChanged}
	 * above provides a second, independent signal for the same reason.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage().contains(AERIAL_FISHING_CATCH_MESSAGE))
		{
			recordCatchIfAerialFishing();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"aerialfishinghighlighter".equals(event.getGroup()))
		{
			return;
		}

		// ConfigChanged is fired from the settings panel on the Swing EDT, not the client
		// thread - reading Client state (isWearingCormorantsGlove, getBoostedSkillLevel,
		// etc.) directly here is unsafe and was observed leaving the item highlight stuck
		// off after toggling/editing a reminder, until a genuine client-thread event (e.g.
		// actually catching a fish) came along and recomputed it correctly. Marshalling
		// onto the client thread fixes that.
		clientThread.invoke(() ->
		{
			// Force both infoboxes to be torn down and, if still warranted, rebuilt from
			// scratch - covers toggling a reminder on/off, changing its threshold, and
			// editing its tooltip text (which otherwise wouldn't apply until the infobox
			// next disappeared and reappeared on its own) without waiting for a real skill
			// change.
			clearFishingBoostInfoBox();
			clearHunterBoostInfoBox();
			refreshFishingReboost();
			refreshHunterReboost();
		});
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.WORN)
		{
			return;
		}

		// Covers putting on/taking off the Cormorant's glove without having to wait for the
		// next real change to Fishing/Hunter itself.
		refreshFishingReboost();
		refreshHunterReboost();
	}

	/**
	 * @return whether the player currently has the Cormorant's glove equipped - see
	 * {@link #CORMORANTS_GLOVE_NO_BIRD}. Both boost reminders are gated on this so they only
	 * ever show up while actually aerial fishing, per explicit user request, rather than
	 * nagging about Fishing/Hunter boosts no matter what else the player is doing.
	 */
	private boolean isWearingCormorantsGlove()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		return equipment != null
			&& (equipment.contains(CORMORANTS_GLOVE_NO_BIRD) || equipment.contains(CORMORANTS_GLOVE_BIRD));
	}

	private void refreshFishingReboost()
	{
		if (client.getLocalPlayer() == null || !config.fishingReboostEnabled() || !isWearingCormorantsGlove())
		{
			fishingReboostNeeded = false;
			fishingReboostNeededPrev = false;
			clearFishingBoostInfoBox();
			return;
		}

		boolean wasNeeded = fishingReboostNeededPrev;
		fishingReboostNeeded = getFishingBoostMargin() < config.fishingReboostThreshold();
		fishingReboostNeededPrev = fishingReboostNeeded;

		if (fishingReboostNeeded && !wasNeeded && config.fishingReboostChatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", config.fishingReboostText(), null);
		}

		if (fishingReboostNeeded && config.fishingReboostInfobox()
			&& config.fishingReboostInfoboxStyle() == AerialFishingBoostInfoboxStyle.MARGIN)
		{
			if (fishingBoostInfoBox == null)
			{
				fishingBoostInfoBox = new AerialFishingInfoBox(null, this,
					() -> "+" + getFishingBoostMargin(), config::fishingReboostText);
				infoBoxManager.addInfoBox(fishingBoostInfoBox);
				spriteManager.getSpriteAsync(SpriteID.Staticons.FISHING, 0, fishingBoostInfoBox);
			}
		}
		else
		{
			clearFishingBoostInfoBox();
		}
	}

	private void refreshHunterReboost()
	{
		if (client.getLocalPlayer() == null || !config.hunterReboostEnabled() || !isWearingCormorantsGlove())
		{
			hunterReboostNeeded = false;
			hunterReboostNeededPrev = false;
			clearHunterBoostInfoBox();
			return;
		}

		boolean wasNeeded = hunterReboostNeededPrev;
		hunterReboostNeeded = getHunterBoostMargin() < config.hunterReboostThreshold();
		hunterReboostNeededPrev = hunterReboostNeeded;

		if (hunterReboostNeeded && !wasNeeded && config.hunterReboostChatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", config.hunterReboostText(), null);
		}

		if (hunterReboostNeeded && config.hunterReboostInfobox()
			&& config.hunterReboostInfoboxStyle() == AerialFishingBoostInfoboxStyle.MARGIN)
		{
			if (hunterBoostInfoBox == null)
			{
				hunterBoostInfoBox = new AerialFishingInfoBox(null, this,
					() -> "+" + getHunterBoostMargin(), config::hunterReboostText);
				infoBoxManager.addInfoBox(hunterBoostInfoBox);
				spriteManager.getSpriteAsync(SpriteID.Staticons2.HUNTER, 0, hunterBoostInfoBox);
			}
		}
		else
		{
			clearHunterBoostInfoBox();
		}
	}

	int getFishingBoostMargin()
	{
		return client.getBoostedSkillLevel(Skill.FISHING) - client.getRealSkillLevel(Skill.FISHING);
	}

	int getHunterBoostMargin()
	{
		return client.getBoostedSkillLevel(Skill.HUNTER) - client.getRealSkillLevel(Skill.HUNTER);
	}

	private void clearFishingBoostInfoBox()
	{
		if (fishingBoostInfoBox != null)
		{
			infoBoxManager.removeInfoBox(fishingBoostInfoBox);
			fishingBoostInfoBox = null;
		}
	}

	private void clearHunterBoostInfoBox()
	{
		if (hunterBoostInfoBox != null)
		{
			infoBoxManager.removeInfoBox(hunterBoostInfoBox);
			hunterBoostInfoBox = null;
		}
	}

	/** Clears the catches-per-hour session - see {@link #CATCH_RATE_IDLE_TIMEOUT_TICKS} and every other reset point (login, hop, plugin disable). */
	private void resetCatchRateSession()
	{
		fishCaughtCount = 0;
		firstCatchTick = -1;
		lastCatchTick = -1;
	}

	/**
	 * Called once per confirmed aerial catch (see {@link #onChatMessage}). The
	 * {@link #isWearingCormorantsGlove} check is just defence in depth, since that chat line
	 * should never appear otherwise. Counting itself always runs regardless of
	 * {@link AerialFishingHighlighterConfig#showCatchRateInfobox}, so the panel reflects the
	 * whole session the moment it's turned on, rather than only catches made after enabling
	 * display.
	 */
	private void recordCatchIfAerialFishing()
	{
		if (!isWearingCormorantsGlove())
		{
			return;
		}

		int currentTick = client.getTickCount();
		if (currentTick == lastCatchTick)
		{
			// Already counted a catch this tick - the chat message and the Fishing xp gain
			// for that same catch land on the same tick, so this is what stops them being
			// counted as two catches.
			return;
		}
		lastCatchTick = currentTick;

		fishCaughtCount++;
		if (firstCatchTick == -1)
		{
			firstCatchTick = currentTick;
		}
	}

	/**
	 * Estimated fish caught per hour this session, based on elapsed time since the first
	 * catch rather than since the plugin/glove went on - dead time standing around before
	 * your first catch shouldn't drag the rate down. 0 if nothing's been caught yet.
	 */
	double getCatchesPerHour()
	{
		if (firstCatchTick == -1)
		{
			return 0;
		}

		int elapsedTicks = Math.max(1, client.getTickCount() - firstCatchTick);
		double elapsedHours = elapsedTicks * 0.6 / 3600.0;
		return fishCaughtCount / elapsedHours;
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
	 * Estimates how many more ticks a spot is likely to stick around. Frenzied spots use
	 * their known fixed {@link #FRENZIED_LIFESPAN_TICKS}; regular spots fall back to
	 * {@link AerialFishingHighlighterConfig#assumedLifespanTicks}, since their true lifespan
	 * is random and unobservable in advance (see {@link #MIN_LIFESPAN_TICKS}). Never
	 * negative: a spot estimated to already be overdue simply reads as 0 remaining.
	 */
	int estimatedTicksRemaining(AerialFishingSpot spot, int currentTick)
	{
		int elapsed = currentTick - spot.getSpawnTick();
		int assumedLifespan = spot.isFrenzied() ? FRENZIED_LIFESPAN_TICKS : config.assumedLifespanTicks();
		return Math.max(0, assumedLifespan - elapsed);
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
