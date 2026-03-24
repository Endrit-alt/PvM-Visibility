package com.visibilityenhancer;

import java.awt.Color;
import net.runelite.client.config.*;

@ConfigGroup("visibilityenhancer")
public interface VisibilityEnhancerConfig extends Config
{
	// --- OPACITY SECTION ---
	@ConfigSection(
			name = "Opacity & Range",
			description = "Control how transparent players and projectiles appear.",
			position = 1
	)
	String opacitySection = "opacitySection";

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "selfOpacity",
			name = "My Opacity",
			position = 1,
			section = opacitySection,
			description = "Transparency of your own character"
	)
	default int selfOpacity() { return 100; }

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "playerOpacity",
			name = "Others Opacity",
			position = 2,
			section = opacitySection,
			description = "Transparency of nearby players"
	)
	default int playerOpacity() { return 100; }

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "myProjectileOpacity",
			name = "My Projectile Opacity",
			position = 3,
			section = opacitySection,
			description = "Transparency of projectiles you fire"
	)
	default int myProjectileOpacity() { return 100; }

	@Range(min = 1, max = 50)
	@ConfigItem(
			keyName = "proximityRange",
			name = "Others Distance",
			position = 4,
			section = opacitySection,
			description = "Radius (in tiles) around you where other players will be affected"
	)
	default int proximityRange() { return 10; }

	@ConfigItem(
			keyName = "ignoreFriends",
			name = "Ignore Friends",
			position = 5,
			section = opacitySection,
			description = "Prevents friends from being affected/transparent"
	)
	default boolean ignoreFriends() { return false; }

	@ConfigItem(
			keyName = "limitAffectedPlayers",
			name = "Limit Max Others",
			position = 6,
			section = opacitySection,
			description = "Limits the number of players affected for performance"
	)
	default boolean limitAffectedPlayers() { return true; }

	@Range(min = 1, max = 100)
	@ConfigItem(
			keyName = "maxAffectedPlayers",
			name = "Max Others",
			position = 7,
			section = opacitySection,
			description = "The maximum number of players to apply effects to"
	)
	default int maxAffectedPlayers() { return 8; }


	// --- EXTRAS SECTION ---
	@ConfigSection(
			name = "Visibility Extras",
			description = "Ground view filters and projectile cleanup.",
			position = 10
	)
	String extrasSection = "extrasSection";

	@ConfigItem(
			keyName = "hideStackedOutlines",
			name = "Hide Stacked Highlights",
			position = 1,
			section = extrasSection,
			description = "Only shows one highlight per tile if players are standing on each other"
	)
	default boolean hideStackedOutlines() { return true; }

	@ConfigItem(
			keyName = "selfClearGround",
			name = "Clear Ground (Self)",
			position = 2,
			section = extrasSection,
			description = "Hides your Cape, Shield, Legs, and Boots to see ground markers better."
	)
	default boolean selfClearGround() { return false; }

	@ConfigItem(
			keyName = "othersClearGround",
			name = "Clear Ground (Others)",
			position = 3,
			section = extrasSection,
			description = "Hides Cape, Shield, Legs, and Boots on nearby affected players."
	)
	default boolean othersClearGround() { return false; }

	@ConfigItem(
			keyName = "hideOthersProjectiles",
			name = "Hide Others' Projectiles",
			position = 4,
			section = extrasSection,
			description = "Completely hides projectiles that didn't come from you"
	)
	default boolean hideOthersProjectiles() { return false; }

	@ConfigItem(
			keyName = "customTransparentPrayers",
			name = "Transparent Others Extras",
			position = 5,
			section = extrasSection,
			description = "Hides native overheads, hitsplats, and HP bars for others, replacing them with transparent versions"
	)
	default boolean othersTransparentPrayers() { return false; }

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "prayersOpacity",
			name = "  ↳ Prayers Opacity",
			position = 6,
			section = extrasSection,
			description = "(Requires 'Transparent Others Extras' enabled)<br>Transparency of replaced overhead prayers"
	)
	default int prayersOpacity() { return 100; }

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "hpBarOpacity",
			name = "  ↳ HP Bar Opacity",
			position = 7,
			section = extrasSection,
			description = "(Requires 'Transparent Others Extras' enabled)<br>Transparency of replaced HP bars"
	)
	default int hpBarOpacity() { return 100; }

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "hitsplatsOpacity",
			name = "  ↳ Hitsplats Opacity",
			position = 8,
			section = extrasSection,
			description = "(Requires 'Transparent Others Extras' enabled)<br>Transparency of replaced hitsplats"
	)
	default int hitsplatsOpacity() { return 100; }

	@ConfigItem(
			keyName = "hideHitsplatBackground",
			name = "  ↳ Hide Hitsplat Bg",
			position = 9,
			section = extrasSection,
			description = "(Requires 'Transparent Others Extras' enabled)<br>Removes the hitsplat background box completely, showing only the numbers"
	)
	default boolean hideHitsplatBackground() { return false; }

	@ConfigItem(
			keyName = "hideThralls",
			name = "Hide Thralls",
			position = 10,
			section = extrasSection,
			description = "Completely hides all Arceeus thralls"
	)
	default boolean hideThralls() { return false; }


	// --- OUTLINE SECTION ---
	@ConfigSection(
			name = "Highlights",
			description = "Settings for player outlines and floor tiles.",
			position = 20
	)
	String outlineSection = "outlineSection";

	@ConfigItem(
			keyName = "highlightSelf",
			name = "Highlight Myself",
			position = 1,
			section = outlineSection,
			description = "Choose how to highlight your own character"
	)
	default HighlightStyle highlightSelf() { return HighlightStyle.NONE; }

	@Alpha
	@ConfigItem(
			keyName = "selfOutlineColor",
			name = "My Highlight Color",
			position = 2,
			section = outlineSection,
			description = "The color of your own highlight"
	)
	default Color selfOutlineColor() { return Color.WHITE; }

	@ConfigItem(
			keyName = "highlightOthers",
			name = "Highlight Others",
			position = 3,
			section = outlineSection,
			description = "Choose how to highlight nearby affected players"
	)
	default HighlightStyle highlightOthers() { return HighlightStyle.NONE; }

	@Alpha
	@ConfigItem(
			keyName = "othersOutlineColor",
			name = "Others Highlight Color",
			position = 4,
			section = outlineSection,
			description = "The color of other players' highlights"
	)
	default Color othersOutlineColor() { return new Color(255, 255, 255, 150); }

	@ConfigItem(
			keyName = "highlightThralls",
			name = "Highlight Thralls",
			position = 5,
			section = outlineSection,
			description = "Choose how to highlight Arceeus thralls"
	)
	default HighlightStyle highlightThralls() { return HighlightStyle.NONE; }

	@Alpha
	@ConfigItem(
			keyName = "thrallsOutlineColor",
			name = "Thralls Highlight Color",
			position = 6,
			section = outlineSection,
			description = "The color of thrall highlights"
	)
	default Color thrallsOutlineColor() { return new Color(0, 255, 255, 150); }


	// --- OUTLINE STYLE SECTION ---
	@ConfigSection(
			name = "Highlight Style",
			description = "Visual aesthetics of the highlights (Global settings).",
			position = 30
	)
	String styleSection = "styleSection";

	@Range(min = 1, max = 10)
	@ConfigItem(
			keyName = "outlineWidth",
			name = "Line Thickness",
			position = 1,
			section = styleSection,
			description = "Thickness of the primary outline"
	)
	default int outlineWidth() { return 1; }

	@Range(min = 0, max = 10)
	@ConfigItem(
			keyName = "outlineFeather",
			name = "Line Blur (Feather)",
			position = 2,
			section = styleSection,
			description = "How soft the edges of the primary line are"
	)
	default int outlineFeather() { return 0; }

	@ConfigItem(
			keyName = "enableGlow",
			name = "Add Outer Glow",
			position = 3,
			section = styleSection,
			description = "Adds a secondary, wider blurred layer behind the primary line"
	)
	default boolean enableGlow() { return false; }

	@Range(min = 1, max = 20)
	@ConfigItem(
			keyName = "glowWidth",
			name = "Glow Thickness",
			position = 4,
			section = styleSection,
			description = "Width of the glow layer"
	)
	default int glowWidth() { return 4; }

	@Range(min = 1, max = 10)
	@ConfigItem(
			keyName = "glowFeather",
			name = "Glow Blur",
			position = 5,
			section = styleSection,
			description = "Softness/Feathering of the glow layer"
	)
	default int glowFeather() { return 4; }

	@ConfigItem(
			keyName = "fillFloorTile",
			name = "Fill Floor Tile",
			position = 6,
			section = styleSection,
			description = "Fills the inside of the floor tile if a 'Tile' highlight is selected"
	)
	default boolean fillFloorTile() { return false; }

	// --- HOTKEY SECTION ---
	@ConfigSection(
			name = "Hotkeys",
			description = "Hotkey settings for the plugin.",
			position = 40
	)
	String hotkeySection = "hotkeySection";

	@ConfigItem(
			keyName = "toggleHotkey",
			name = "Toggle Plugin",
			position = 1,
			section = hotkeySection,
			description = "Press this key to enable or disable the plugin's effects."
	)
	default Keybind toggleHotkey() { return Keybind.NOT_SET; }

	@ConfigItem(
			keyName = "doubleTapDelay",
			name = "Double-tap delay",
			description = "Delay for the double-tap to toggle the plugin off. 0 to disable.",
			position = 2,
			section = hotkeySection
	)
	@Units(Units.MILLISECONDS)
	default int doubleTapDelay() { return 250; }
}