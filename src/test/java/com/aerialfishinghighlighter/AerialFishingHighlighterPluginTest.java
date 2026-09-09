package com.aerialfishinghighlighter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer entry point: boots a real RuneLite client with this plugin loaded, via
 * {@code ./gradlew run}. This is the officially supported way to test a plugin against a
 * live account - RuneLite has no generic mechanism for dropping an arbitrary standalone
 * jar into an existing production install.
 */
public class AerialFishingHighlighterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AerialFishingHighlighterPlugin.class);
		RuneLite.main(args);
	}
}
