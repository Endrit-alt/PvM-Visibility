package com.visibilityenhancer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.SpriteID;
import net.runelite.api.Perspective;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

public class VisibilityEnhancerOverlay extends Overlay
{
	private final Client client;
	private final VisibilityEnhancer plugin;
	private final VisibilityEnhancerConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private final SpriteManager spriteManager;

	private final Set<WorldPoint> renderedTiles = new HashSet<>();
	private final List<Player> sortedGhosts = new ArrayList<>(32);

	private int cachedOutlineWidth = -1;
	private int cachedGlowWidth = -1;
	private BasicStroke primaryStroke;
	private BasicStroke glowStroke;

	private Color cachedColor;
	private Color cachedGlowColor;
	private Color cachedFillColor;

	@Inject
	private VisibilityEnhancerOverlay(Client client, VisibilityEnhancer plugin, VisibilityEnhancerConfig config, ModelOutlineRenderer modelOutlineRenderer, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.spriteManager = spriteManager;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isPluginToggledOn())
		{
			return null;
		}

		Player local = client.getLocalPlayer();
		WorldPoint localPoint = local != null ? local.getWorldLocation() : null;
		LocalPoint localLocalPoint = local != null ? local.getLocalLocation() : null;

		// --- Thralls Outlines ---
		if (config.thrallsOutline())
		{
			boolean useFloorTile = config.thrallsUseFloorTileOutline();
			Color thrallsColor = config.thrallsOutlineColor();

			for (NPC npc : client.getNpcs())
			{
				if (npc != null && VisibilityEnhancer.THRALL_IDS.contains(npc.getId()))
				{
					if (useFloorTile)
					{
						renderFloorTile(graphics, npc, thrallsColor);
					}
					else
					{
						// Note: If thralls are hidden via config.hideThralls(), Model outlines might not draw
						// but the floor tile option above will still always draw successfully!
						renderOutlineLayers(npc, thrallsColor);
					}
				}
			}
		}

		if (local != null && config.selfOutline())
		{
			Model localModel = local.getModel();
			if (localModel == null || localModel.getOverrideAmount() == 0)
			{
				if (config.selfUseFloorTileOutline())
				{
					renderFloorTile(graphics, local, config.selfOutlineColor());
				}
				else
				{
					renderOutlineLayers(local, config.selfOutlineColor());
				}
			}
		}

		if (config.othersOutline())
		{
			renderedTiles.clear();
			boolean hideStacked = config.hideStackedOutlines();
			boolean useFloorTile = config.othersUseFloorTileOutline();
			Color othersColor = config.othersOutlineColor();

			sortedGhosts.clear();
			sortedGhosts.addAll(plugin.getGhostedPlayers());

			if (localLocalPoint != null)
			{
				sortedGhosts.sort((p1, p2) ->
				{
					LocalPoint lp1 = p1.getLocalLocation();
					LocalPoint lp2 = p2.getLocalLocation();
					if (lp1 == null || lp2 == null) return 0;

					return Integer.compare(lp2.distanceTo(localLocalPoint), lp1.distanceTo(localLocalPoint));
				});
			}

			for (Player player : sortedGhosts)
			{
				WorldPoint playerPoint = player.getWorldLocation();
				if (playerPoint == null) continue;

				Model pModel = player.getModel();
				if (pModel != null && pModel.getOverrideAmount() != 0)
				{
					continue;
				}

				if (hideStacked)
				{
					if (localPoint != null && playerPoint.equals(localPoint)) continue;
					if (renderedTiles.contains(playerPoint)) continue;
					renderedTiles.add(playerPoint);
				}

				if (useFloorTile)
				{
					renderFloorTile(graphics, player, othersColor);
				}
				else
				{
					renderOutlineLayers(player, othersColor);
				}
			}
		}

		boolean othersCustomPrayers = config.othersTransparentPrayers();

		if (othersCustomPrayers)
		{
			Set<WorldPoint> renderedPrayerTiles = new HashSet<>();
			List<Rectangle> renderedTextBounds = new ArrayList<>(); // Tracks drawn text to prevent overlap

			// --- Track native text from non-ghosted players (like yourself) ---
			for (Player p : client.getPlayers())
			{
				if (p != null && !plugin.getGhostedPlayers().contains(p))
				{
					String text = p.getOverheadText();
					if (text != null && !text.isEmpty())
					{
						int zOffset = 20;
						Point textPoint = p.getCanvasTextLocation(graphics, text, p.getLogicalHeight() + zOffset);
						if (textPoint != null)
						{
							graphics.setFont(FontManager.getRunescapeBoldFont());
							FontMetrics fontMetrics = graphics.getFontMetrics();

							String cleanText = Text.removeTags(text);
							int textWidth = fontMetrics.stringWidth(cleanText);
							int textHeight = fontMetrics.getHeight();
							int drawX = textPoint.getX() - 1;
							int drawY = textPoint.getY() + 6;

							renderedTextBounds.add(new Rectangle(drawX, drawY - textHeight, textWidth, textHeight));
						}
					}
				}
			}
			// -----------------------------------------------------------------------

			for (Player player : plugin.getGhostedPlayers())
			{
				drawOverheadText(graphics, player, renderedTextBounds);

				WorldPoint playerPoint = player.getWorldLocation();

				if (playerPoint != null)
				{
					if (localPoint != null && playerPoint.equals(localPoint))
					{
						continue;
					}

					if (renderedPrayerTiles.contains(playerPoint))
					{
						continue;
					}

					renderedPrayerTiles.add(playerPoint);
				}

				drawTransparentPrayer(graphics, player, config.prayersOpacity());

				int ratio = player.getHealthRatio();
				int scale = player.getHealthScale();
				if (ratio > -1 && scale > 0)
				{
					drawTransparentHpBar(graphics, player, ratio, scale, config.hpBarOpacity());
				}

				List<VisibilityEnhancer.CustomHitsplat> hitsplats = plugin.getCustomHitsplats().get(player);
				if (hitsplats != null && !hitsplats.isEmpty())
				{
					drawTransparentHitsplats(graphics, player, hitsplats, config.hitsplatsOpacity());
				}
			}
		}

		return null;
	}

	private void renderOutlineLayers(Player player, Color color)
	{
		if (config.enableGlow())
		{
			modelOutlineRenderer.drawOutline(player, config.glowWidth(), color, config.glowFeather());
		}
		modelOutlineRenderer.drawOutline(player, config.outlineWidth(), color, config.outlineFeather());
	}

	// Overload for NPCs (Thralls)
	private void renderOutlineLayers(NPC npc, Color color)
	{
		if (config.enableGlow())
		{
			modelOutlineRenderer.drawOutline(npc, config.glowWidth(), color, config.glowFeather());
		}
		modelOutlineRenderer.drawOutline(npc, config.outlineWidth(), color, config.outlineFeather());
	}

	private void renderFloorTile(Graphics2D graphics, Actor actor, Color color)
	{
		Polygon poly = Perspective.getCanvasTilePoly(client, actor.getLocalLocation());
		if (poly != null)
		{
			if (cachedColor == null || !cachedColor.equals(color))
			{
				cachedColor = color;
				cachedGlowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, color.getAlpha() - 100));
				cachedFillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
			}

			if (cachedOutlineWidth != config.outlineWidth())
			{
				cachedOutlineWidth = config.outlineWidth();
				primaryStroke = new BasicStroke(cachedOutlineWidth);
			}

			if (cachedGlowWidth != config.glowWidth() || cachedOutlineWidth != config.outlineWidth())
			{
				cachedGlowWidth = config.glowWidth();
				glowStroke = new BasicStroke(cachedOutlineWidth + cachedGlowWidth);
			}

			if (config.enableGlow())
			{
				graphics.setColor(cachedGlowColor);
				graphics.setStroke(glowStroke);
				graphics.draw(poly);
			}

			graphics.setColor(cachedColor);
			graphics.setStroke(primaryStroke);
			graphics.draw(poly);

			if (config.fillFloorTile())
			{
				graphics.setColor(cachedFillColor);
				graphics.fill(poly);
			}
		}
	}

	private void drawTransparentPrayer(Graphics2D graphics, Player player, int opacityPercent)
	{
		HeadIcon icon = player.getOverheadIcon();
		if (icon == null) return;

		int spriteId = getSpriteId(icon);
		if (spriteId == -1) return;

		BufferedImage prayerImage = spriteManager.getSprite(spriteId, 0);
		if (prayerImage == null) return;

		int zOffset = 20;
		Point point = player.getCanvasImageLocation(prayerImage, player.getLogicalHeight() + zOffset);
		if (point == null) return;

		int drawX = point.getX();
		int drawY = point.getY() - 25;

		float alpha = opacityPercent / 100f;
		Composite originalComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		graphics.drawImage(prayerImage, drawX, drawY, null);

		graphics.setComposite(originalComposite);
	}

	private void drawTransparentHpBar(Graphics2D graphics, Player player, int ratio, int scale, int opacityPercent)
	{
		int alpha = (int) ((opacityPercent / 100f) * 255);
		if (alpha <= 0) return;

		Point point = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight() + 15);
		if (point == null) return;

		int width = 30;
		int height = 5;
		int fill = (int) (((float) ratio / scale) * width);
		int x = point.getX() - (width / 2);
		int y = point.getY() - 2;

		graphics.setColor(new Color(255, 0, 0, alpha));
		graphics.fillRect(x, y, width, height);

		graphics.setColor(new Color(0, 255, 0, alpha));
		graphics.fillRect(x, y, fill, height);
	}

	private void drawTransparentHitsplats(Graphics2D graphics, Player player, List<VisibilityEnhancer.CustomHitsplat> hitsplats, int opacityPercent)
	{
		int alpha = (int) ((opacityPercent / 100f) * 255);

		if (alpha <= 0) return;

		int bgAlpha = config.hideHitsplatBackground() ? 0 : (int) (alpha * 0.8f);
		int boxOutlineAlpha = config.hideHitsplatBackground() ? 0 : alpha;

		LocalPoint lp = player.getLocalLocation();
		if (lp == null) return;

		Point basePoint = Perspective.localToCanvas(client, lp, client.getPlane(), player.getLogicalHeight() / 2);
		if (basePoint == null) return;

		graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
		FontMetrics fm = graphics.getFontMetrics();

		int boxTextHeight = fm.getAscent() - 1;

		int paddingX = 1;
		int paddingY = 1;
		int boxHeight = boxTextHeight + (paddingY * 2);
		int ySpacing = boxHeight + 2;

		int size = hitsplats.size();

		int shiftDown = 0;
		Point hpPoint = player.getCanvasTextLocation(graphics, "", player.getLogicalHeight() + 15);
		if (hpPoint != null)
		{
			int hpBarBottom = (hpPoint.getY() - 2) + 5;
			int ceilingY = hpBarBottom + 2;

			int highestOffsetY = 0;
			if (size == 2) highestOffsetY = -(ySpacing / 2);
			else if (size == 4) highestOffsetY = -ySpacing;
			else if (size > 1) {
				int totalRows = (size + 1) / 2;
				highestOffsetY = -((totalRows - 1) * ySpacing / 2);
			}

			int highestBoxY = basePoint.getY() + highestOffsetY - (boxHeight / 2) - 10;

			if (highestBoxY < ceilingY) {
				shiftDown = ceilingY - highestBoxY;
			}
		}

		int maxTextWidth = 0;
		for (VisibilityEnhancer.CustomHitsplat hit : hitsplats)
		{
			int w = fm.stringWidth(String.valueOf(hit.getAmount()));
			if (w > maxTextWidth)
			{
				maxTextWidth = w;
			}
		}

		int maxBoxWidth = maxTextWidth + 1 + (paddingX * 2);
		int xSpacing = maxBoxWidth + 2;

		for (int i = 0; i < size; i++)
		{
			VisibilityEnhancer.CustomHitsplat hit = hitsplats.get(i);
			String text = String.valueOf(hit.getAmount());
			int textWidth = fm.stringWidth(text);
			int boxWidth = textWidth + 1 + (paddingX * 2);

			int offsetX = 0;
			int offsetY = 0;

			if (size == 1)
			{
				offsetX = 0;
				offsetY = 0;
			}
			else if (size == 2)
			{
				offsetX = 0;
				offsetY = (i == 0) ? -(ySpacing / 2) : (ySpacing / 2);
			}
			else if (size == 4)
			{
				if (i == 0)      { offsetX = 0; offsetY = -ySpacing; }
				else if (i == 1) { offsetX = -(xSpacing / 2); offsetY = 0; }
				else if (i == 2) { offsetX = (xSpacing / 2); offsetY = 0; }
				else if (i == 3) { offsetX = 0; offsetY = ySpacing; }
			}
			else
			{
				int row = i / 2;
				int col = i % 2;
				int totalRows = (size + 1) / 2;
				if (row == totalRows - 1 && size % 2 != 0)
				{
					offsetX = 0;
				}
				else
				{
					offsetX = (col == 0) ? -(xSpacing / 2) : (xSpacing / 2);
				}

				offsetY = (row * ySpacing) - ((totalRows - 1) * ySpacing / 2);
			}

			int boxX = basePoint.getX() + offsetX - (boxWidth / 2);
			int boxY = basePoint.getY() + offsetY - (boxHeight / 2) - 10 + shiftDown;

			if (bgAlpha > 0)
			{
				Color backColor = hit.getAmount() == 0 ?
						new Color(50, 90, 160, bgAlpha) : new Color(180, 40, 40, bgAlpha);
				graphics.setColor(backColor);
				graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 2, 2);
			}

			int textDrawX = boxX + paddingX;
			int textDrawY = boxY + boxTextHeight + paddingY + 1;

			if (alpha > 0)
			{
				Color textShadowColor = new Color(0, 0, 0, alpha);
				Color textColor = new Color(255, 255, 255, alpha);

				graphics.setColor(textShadowColor);
				graphics.drawString(text, textDrawX + 1, textDrawY + 1);

				graphics.setColor(textColor);
				graphics.drawString(text, textDrawX, textDrawY);
			}
		}
	}

	private void drawOverheadText(Graphics2D graphics, Player player, List<Rectangle> renderedTextBounds)
	{
		String text = player.getOverheadText();
		if (text == null || text.isEmpty()) return;

		int zOffset = 20;
		Point textPoint = player.getCanvasTextLocation(graphics, text, player.getLogicalHeight() + zOffset);
		if (textPoint == null) return;

		graphics.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fontMetrics = graphics.getFontMetrics();

		String cleanText = Text.removeTags(text);
		int textWidth = fontMetrics.stringWidth(cleanText);
		int textHeight = fontMetrics.getHeight();

		int drawX = textPoint.getX() - 1;
		int drawY = textPoint.getY() + 6;

		Rectangle currentBounds = new Rectangle(drawX, drawY - textHeight, textWidth, textHeight);

		boolean isOverlapping = true;
		while (isOverlapping)
		{
			isOverlapping = false;
			for (Rectangle drawnBounds : renderedTextBounds)
			{
				if (currentBounds.intersects(drawnBounds))
				{
					drawY -= (textHeight + 2);
					currentBounds.setLocation(drawX, drawY - textHeight);
					isOverlapping = true;
					break;
				}
			}
		}

		renderedTextBounds.add(currentBounds);

		Point adjustedPoint = new Point(drawX, drawY);

		OverlayUtil.renderTextLocation(graphics, adjustedPoint, text, Color.YELLOW);
	}

	private int getSpriteId(HeadIcon icon)
	{
		switch (icon)
		{
			case MELEE: return SpriteID.PRAYER_PROTECT_FROM_MELEE;
			case RANGED: return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			case MAGIC: return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			case RETRIBUTION: return SpriteID.PRAYER_RETRIBUTION;
			case SMITE: return SpriteID.PRAYER_SMITE;
			case REDEMPTION: return SpriteID.PRAYER_REDEMPTION;
			default: return -1;
		}
	}
}