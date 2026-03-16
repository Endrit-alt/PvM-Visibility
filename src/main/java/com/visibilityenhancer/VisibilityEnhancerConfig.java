package com.visibilityenhancer;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("visibilityenhancer")
public interface VisibilityEnhancerConfig extends Config
{
	@ConfigSection(name = "Self Settings", description = "Your character settings", position = 1)
	String selfSection = "selfSection";

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "selfOpacity", name = "My Opacity", position = 2, section = selfSection, description = "Transparency of your character")
	default int selfOpacity() { return 100; }

	@ConfigItem(keyName = "selfSilhouette", name = "Silhouette Myself", position = 3, section = selfSection, description = "Enable silhouette for yourself")
	default boolean selfSilhouette() { return false; }

	@ConfigItem(keyName = "silhouetteColor", name = "Silhouette Color", position = 4, section = selfSection, description = "Color of the silhouette")
	default Color silhouetteColor() { return new Color(0, 255, 255); }

	@ConfigSection(name = "Other Players", description = "Settings for nearby players", position = 10)
	String othersSection = "othersSection";

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "playerOpacity", name = "Ghost Opacity", position = 11, section = othersSection, description = "Transparency of nearby players")
	default int playerOpacity() { return 50; }

	@Range(min = 1, max = 50)
	@ConfigItem(keyName = "proximityRange", name = "Proximity Distance", position = 12, section = othersSection, description = "Radius for the effect")
	default int proximityRange() { return 10; }

	@ConfigItem(keyName = "ignoreFriends", name = "Ignore Friends", position = 13, section = othersSection, description = "Don't ghost your friends")
	default boolean ignoreFriends() { return false; }

	// --- NEW OPTIMIZATION SETTINGS ---
	@ConfigItem(keyName = "limitAffectedPlayers", name = "Limit affected players", position = 14, section = othersSection, description = "Limit the number of players that receive the ghosting effect to save performance")
	default boolean limitAffectedPlayers() { return true; }

	@Range(min = 1, max = 100)
	@ConfigItem(keyName = "maxAffectedPlayers", name = "Max affected players", position = 15, section = othersSection, description = "The maximum number of players to ghost at once")
	default int maxAffectedPlayers() { return 8; }
}