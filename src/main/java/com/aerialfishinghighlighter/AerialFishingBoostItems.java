package com.aerialfishinghighlighter;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Item ids for every temporary Fishing/Hunter boost source listed on
 * https://oldschool.runescape.wiki/w/Temporary_skill_boost as of 2026-09-09, transcribed from
 * each item's own Infobox Item wikitext. Deliberately excludes two sources from that page:
 * <ul>
 * <li>Spicy stew - a single item id is shared regardless of which spice (and therefore which
 * skill) was mixed in, and the effect is a random +0-5 that can just as easily be a drain, so
 * an inventory Spicy stew can't be identified as "a Fishing/Hunter booster" from its id alone.
 * <li>Fishing Guild's +7 and Horn of Plenty's +2/+4 - both are explicitly documented as
 * "invisible" boosts that only affect success-rate rolls and never touch the visible boosted
 * skill level this plugin actually watches (see AerialFishingHighlighterPlugin's StatChanged
 * handling), so highlighting them wouldn't correspond to anything this feature can detect -
 * and Fishing Guild isn't an item to highlight in the first place.
 * </ul>
 */
final class AerialFishingBoostItems
{
	/**
	 * https://oldschool.runescape.wiki/w/Fishing_potion - (1)=155, (2)=153, (3)=151, (4)=2438; +3
	 * https://oldschool.runescape.wiki/w/Super_fishing_potion - (1)=31611, (2)=31608, (3)=31605, (4)=31602; +6
	 * https://oldschool.runescape.wiki/w/Fishing_cape - untrimmed=9798, trimmed=9799; +1 via the worn "Boost" option
	 * https://oldschool.runescape.wiki/w/Barnacle_blaster - 31255; +1
	 * https://oldschool.runescape.wiki/w/Trawler%27s_trust - 31820; +2 (also boosts Sailing)
	 * https://oldschool.runescape.wiki/w/Dragon_harpoon - 21028; +3 via the "Fishstabber" special attack
	 * https://oldschool.runescape.wiki/w/Infernal_harpoon - charged=21031, uncharged=21033; +3 via "Fishstabber"
	 * https://oldschool.runescape.wiki/w/Crystal_harpoon - active=23762, inactive=23764; +3 via "Fishstabber"
	 * https://oldschool.runescape.wiki/w/Fishing_mix - (1)=11479, (2)=11477; +3
	 * https://oldschool.runescape.wiki/w/Fish_pie - whole=7188, half=7190; +3
	 * https://oldschool.runescape.wiki/w/Bottle_of_fishtongue_tonic - 31873; +3
	 * https://oldschool.runescape.wiki/w/Admiral_pie - whole=7198, half=7200; +5
	 */
	static final Set<Integer> FISHING = ImmutableSet.of(
		155, 153, 151, 2438,
		31611, 31608, 31605, 31602,
		9798, 9799,
		31255,
		31820,
		21028,
		21031, 21033,
		23762, 23764,
		11479, 11477,
		7188, 7190,
		31873,
		7198, 7200
	);

	/**
	 * https://oldschool.runescape.wiki/w/Hunter_potion - (1)=10004, (2)=10002, (3)=10000, (4)=9998; +3
	 * https://oldschool.runescape.wiki/w/Super_hunter_potion - (1)=31635, (2)=31632, (3)=31629, (4)=31626; +6
	 * https://oldschool.runescape.wiki/w/Hunter_cape - untrimmed=9948, trimmed=9949; +1 via the worn "Boost" option
	 * https://oldschool.runescape.wiki/w/Blackbird_red - 29944; +1
	 * https://oldschool.runescape.wiki/w/Sailor%27s_mirage - 31261; +1
	 * https://oldschool.runescape.wiki/w/Trapper%27s_tipple - 29277; +2
	 * https://oldschool.runescape.wiki/w/Hunting_mix - (1)=11519, (2)=11517; +3
	 */
	static final Set<Integer> HUNTER = ImmutableSet.of(
		10004, 10002, 10000, 9998,
		31635, 31632, 31629, 31626,
		9948, 9949,
		29944,
		31261,
		29277,
		11519, 11517
	);

	private AerialFishingBoostItems()
	{
	}
}
