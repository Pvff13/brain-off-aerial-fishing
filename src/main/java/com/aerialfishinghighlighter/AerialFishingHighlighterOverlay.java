package com.aerialfishinghighlighter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws a highlight over the best aerial fishing spot to fish next (see
 * {@link AerialFishingHighlighterPlugin#getRankedSpots()}), optionally the 2nd/3rd best
 * too, and optionally a faint outline over every other tracked spot. Purely observational:
 * reads plugin/client state and paints on top of the scene, nothing else.
 */
public class AerialFishingHighlighterOverlay extends Overlay
{
	/** World-height offset (the units {@code getCanvasTextLocation} takes) used to anchor both text lines above a spot. */
	private static final int BASE_TEXT_Z_OFFSET = 40;

	/**
	 * Vertical gap, in actual screen pixels, enforced between the two text lines when both
	 * are shown. A world-height (z-offset) gap isn't reliable for this: how much screen
	 * separation it produces depends on camera pitch, and all but disappears at the steep,
	 * near-top-down angle most players use for aerial fishing - so this is applied directly
	 * to each line's on-screen position instead.
	 */
	private static final int TEXT_LINE_PIXEL_GAP = 16;

	private final Client client;
	private final AerialFishingHighlighterPlugin plugin;
	private final AerialFishingHighlighterConfig config;

	@Inject
	private AerialFishingHighlighterOverlay(Client client, AerialFishingHighlighterPlugin plugin, AerialFishingHighlighterConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.overlayEnabled() || plugin.getSpots().isEmpty())
		{
			return null;
		}

		List<AerialFishingSpot> ranked = plugin.getRankedSpots();
		AerialFishingSpot best = ranked.size() > 0 ? ranked.get(0) : null;
		AerialFishingSpot second = config.highlightSecondBest() && ranked.size() > 1 ? ranked.get(1) : null;
		AerialFishingSpot third = config.highlightThirdBest() && ranked.size() > 2 ? ranked.get(2) : null;

		if (config.showOtherSpots())
		{
			for (AerialFishingSpot spot : plugin.getSpots())
			{
				if (spot != best && spot != second && spot != third)
				{
					renderHull(graphics, spot.getNpc(), config.otherSpotsColor());
				}
			}
		}

		renderRanked(graphics, best, config.highlightColor());
		renderRanked(graphics, second, config.secondBestColor());
		renderRanked(graphics, third, config.thirdBestColor());

		return null;
	}

	private void renderRanked(Graphics2D graphics, AerialFishingSpot spot, Color color)
	{
		if (spot == null)
		{
			return;
		}

		renderHull(graphics, spot.getNpc(), color);

		String ticksText = config.showDistanceText() ? ticksLabel(spot) : null;
		String lifespanText = config.showLifespanText() ? lifespanLabel(spot) : null;
		Color lifespanColor = lifespanColor(spot, color);

		if (ticksText != null && lifespanText != null)
		{
			// Estimated time left goes on the bottom (closest to the spot), travel ticks
			// above it. Each line's canvas anchor is still computed independently (so it
			// stays horizontally centred on its own text width), but the vertical split
			// between them is a fixed screen-pixel offset from that anchor rather than a
			// world-height one - see TEXT_LINE_PIXEL_GAP.
			renderLabel(graphics, spot, color, ticksText, BASE_TEXT_Z_OFFSET, -TEXT_LINE_PIXEL_GAP / 2);
			renderLabel(graphics, spot, lifespanColor, lifespanText, BASE_TEXT_Z_OFFSET, TEXT_LINE_PIXEL_GAP / 2);
		}
		else if (ticksText != null)
		{
			renderLabel(graphics, spot, color, ticksText, BASE_TEXT_Z_OFFSET, 0);
		}
		else if (lifespanText != null)
		{
			renderLabel(graphics, spot, lifespanColor, lifespanText, BASE_TEXT_Z_OFFSET, 0);
		}
	}

	/**
	 * By default, {@link OverlayUtil#renderPolygon(Graphics2D, Shape, Color)} outlines the
	 * clickbox in {@code color} but fills it with a generic translucent black - the
	 * "Fill clickbox with colour" setting swaps that generic fill for {@code color} itself,
	 * so the whole clickbox reads as that spot's colour rather than just its edge.
	 */
	private void renderHull(Graphics2D graphics, NPC npc, Color color)
	{
		Shape hull = npc.getConvexHull();
		if (hull == null)
		{
			return;
		}

		if (config.fillClickbox())
		{
			OverlayUtil.renderPolygon(graphics, hull, color, color, new BasicStroke(2));
		}
		else
		{
			OverlayUtil.renderPolygon(graphics, hull, color);
		}
	}

	private String ticksLabel(AerialFishingSpot spot)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		int tileDistance = localPlayer.getWorldLocation().distanceTo(spot.getNpc().getWorldLocation());
		int ticks = AerialFishingHighlighterPlugin.effectiveTicks(spot.isFrenzied(), tileDistance);
		return ticks + (ticks == 1 ? " tick" : " ticks");
	}

	/**
	 * Always "~"-prefixed since the underlying lifespan is randomised and never exactly
	 * known - see {@link AerialFishingHighlighterPlugin#estimatedTicksRemaining}. Wording
	 * beyond that follows {@link AerialFishingHighlighterConfig#lifespanFormat}.
	 */
	private String lifespanLabel(AerialFishingSpot spot)
	{
		int remaining = plugin.estimatedTicksRemaining(spot, client.getTickCount());
		String text = "~" + remaining;
		if (config.lifespanFormat() == AerialFishingLifespanFormat.TICKS_LEFT)
		{
			text += remaining == 1 ? " tick left" : " ticks left";
		}
		return text;
	}

	/**
	 * Red once at least {@link AerialFishingHighlighterPlugin#MIN_LIFESPAN_TICKS} ticks
	 * have elapsed since the spot spawned - the low end of the observed lifespan range, so
	 * from that point on the spot could realistically have already despawned even though
	 * it's still being tracked. {@code baseColor} is this spot's own rank colour (best/2nd/
	 * 3rd), used as long as that warning doesn't apply.
	 */
	private Color lifespanColor(AerialFishingSpot spot, Color baseColor)
	{
		int elapsed = client.getTickCount() - spot.getSpawnTick();
		return elapsed >= AerialFishingHighlighterPlugin.MIN_LIFESPAN_TICKS ? Color.RED : baseColor;
	}

	/** @param pixelYOffset added to the computed canvas location's screen Y - negative moves the text up, positive moves it down. */
	private void renderLabel(Graphics2D graphics, AerialFishingSpot spot, Color color, String text, int zOffset, int pixelYOffset)
	{
		if (text == null)
		{
			return;
		}

		NPC npc = spot.getNpc();
		Point textLocation = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + zOffset);
		if (textLocation != null)
		{
			if (pixelYOffset != 0)
			{
				textLocation = new Point(textLocation.getX(), textLocation.getY() + pixelYOffset);
			}
			OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
		}
	}
}
