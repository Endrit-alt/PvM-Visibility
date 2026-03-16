package com.visibilityenhancer;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList; // Added
import java.util.Arrays;
import java.util.Comparator; // Added
import java.util.HashSet;
import java.util.List; // Added
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "Visibility Enhancer",
		description = "Adjusts teammate opacity and silhouettes for raids.",
		tags = {"raid", "opacity", "ghost", "silhouette"}
)
public class VisibilityEnhancer extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private VisibilityEnhancerConfig config;

	private final Map<Model, SavedModelState> selfCache = new WeakHashMap<>();
	private final Set<Player> ghostedPlayers = new HashSet<>();

	@Override
	protected void shutDown()
	{
		Player local = client.getLocalPlayer();
		if (local != null)
		{
			restoreSelf(local);
		}

		selfCache.clear();
		ghostedPlayers.clear();
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}

		Model model = player.getModel();
		if (model != null)
		{
			selfCache.remove(model);
		}

		ghostedPlayers.remove(player);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			ghostedPlayers.clear();
			return;
		}

		int playerOpacity = config.playerOpacity();
		int proximityRange = config.proximityRange();
		boolean ignoreFriends = config.ignoreFriends();

		if (playerOpacity >= 100)
		{
			restoreTrackedPlayers();
			return;
		}

		WorldPoint localPoint = local.getWorldLocation();
		if (localPoint == null)
		{
			restoreTrackedPlayers();
			return;
		}

		// 1. Identify all players within range
		List<Player> eligiblePlayers = new ArrayList<>();
		for (Player player : client.getPlayers())
		{
			if (player == null || player == local) continue;

			if (ignoreFriends && isFriend(player))
			{
				if (ghostedPlayers.contains(player))
				{
					restoreOtherOpacity(player);
				}
				continue;
			}

			WorldPoint playerPoint = player.getWorldLocation();
			if (playerPoint != null && localPoint.distanceTo(playerPoint) <= proximityRange)
			{
				eligiblePlayers.add(player);
			}
		}

		// 2. Apply Optimization: Sort by distance and sublist to max count
		if (config.limitAffectedPlayers() && eligiblePlayers.size() > config.maxAffectedPlayers())
		{
			eligiblePlayers.sort(Comparator.comparingInt(p ->
					localPoint.distanceTo(p.getWorldLocation())));

			eligiblePlayers = eligiblePlayers.subList(0, config.maxAffectedPlayers());
		}

		// 3. Apply the effect to the chosen subset
		Set<Player> playersInRange = new HashSet<>(eligiblePlayers);
		for (Player player : playersInRange)
		{
			applyOtherOpacity(player, playerOpacity);
		}

		// 4. Restore players who are no longer in the "top X closest" list
		Set<Player> noLongerGhosted = new HashSet<>(ghostedPlayers);
		noLongerGhosted.removeAll(playersInRange);
		for (Player player : noLongerGhosted)
		{
			restoreOtherOpacity(player);
		}

		ghostedPlayers.clear();
		ghostedPlayers.addAll(playersInRange);
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		int selfOpacity = config.selfOpacity();
		boolean selfSilhouette = config.selfSilhouette();

		if (selfOpacity < 100 || selfSilhouette)
		{
			handleSelf(local, selfOpacity, selfSilhouette, config.silhouetteColor());
		}
		else
		{
			restoreSelf(local);
		}
	}

	private void handleSelf(Player local, int selfOpacity, boolean selfSilhouette, Color silhouetteColor)
	{
		Model model = local.getModel();
		if (model == null)
		{
			return;
		}

		cacheSelfState(model);

		if (selfOpacity < 100)
		{
			applyModelOpacity(model, selfOpacity);
		}
		else
		{
			restoreSelfOpacity(model);
		}

		if (selfSilhouette)
		{
			applyModelSilhouette(model, silhouetteColor);
		}
		else
		{
			restoreSelfColors(model);
		}
	}

	private void restoreSelf(Player local)
	{
		Model model = local.getModel();
		if (model == null)
		{
			return;
		}

		restoreSelfOpacity(model);
		restoreSelfColors(model);
	}

	private void cacheSelfState(Model model)
	{
		if (!selfCache.containsKey(model))
		{
			selfCache.put(model, new SavedModelState(
					copy(model.getFaceTransparencies()),
					copy(model.getFaceColors1()),
					copy(model.getFaceColors2()),
					copy(model.getFaceColors3()),
					copy(model.getFaceRenderPriorities()),
					copy(model.getFaceTextures())
			));
		}
	}

	private boolean isFriend(Player player)
	{
		String name = player.getName();
		return player.isFriend() || (name != null && client.isFriended(name, false));
	}

	private void applyOtherOpacity(Player player, int opacityPercent)
	{
		Model model = player.getModel();
		if (model == null)
		{
			return;
		}

		byte[] trans = model.getFaceTransparencies();
		if (trans == null || trans.length == 0)
		{
			return;
		}

		int alpha = clampAlpha(opacityPercent);
		if ((trans[0] & 0xFF) == alpha)
		{
			return;
		}

		Arrays.fill(trans, (byte) alpha);
	}

	private void restoreOtherOpacity(Player player)
	{
		Model model = player.getModel();
		if (model == null)
		{
			return;
		}

		byte[] trans = model.getFaceTransparencies();
		if (trans == null || trans.length == 0)
		{
			return;
		}

		if ((trans[0] & 0xFF) == 0)
		{
			return;
		}

		Arrays.fill(trans, (byte) 0);
	}

	private void restoreTrackedPlayers()
	{
		for (Player player : ghostedPlayers)
		{
			restoreOtherOpacity(player);
		}
		ghostedPlayers.clear();
	}

	private void applyModelOpacity(Model model, int opacityPercent)
	{
		byte[] trans = model.getFaceTransparencies();
		if (trans == null || trans.length == 0)
		{
			return;
		}

		int alpha = clampAlpha(opacityPercent);
		if ((trans[0] & 0xFF) == alpha)
		{
			return;
		}

		Arrays.fill(trans, (byte) alpha);
	}

	private void restoreSelfOpacity(Model model)
	{
		SavedModelState state = selfCache.get(model);
		if (state == null || state.trans == null)
		{
			return;
		}

		byte[] trans = model.getFaceTransparencies();
		if (trans == null || trans.length != state.trans.length)
		{
			return;
		}

		if (Arrays.equals(trans, state.trans))
		{
			return;
		}

		System.arraycopy(state.trans, 0, trans, 0, state.trans.length);
	}

	private void applyModelSilhouette(Model model, Color color)
	{
		int hsl = colorToHSL(color);

		int[] c1 = model.getFaceColors1();
		if (c1 != null && (c1.length == 0 || c1[0] != hsl))
		{
			Arrays.fill(c1, hsl);
		}

		int[] c2 = model.getFaceColors2();
		if (c2 != null && (c2.length == 0 || c2[0] != hsl))
		{
			Arrays.fill(c2, hsl);
		}

		int[] c3 = model.getFaceColors3();
		if (c3 != null && (c3.length == 0 || c3[0] != hsl))
		{
			Arrays.fill(c3, hsl);
		}

		byte[] priorities = model.getFaceRenderPriorities();
		if (priorities != null && (priorities.length == 0 || priorities[0] != 10))
		{
			Arrays.fill(priorities, (byte) 10);
		}

		short[] textures = model.getFaceTextures();
		if (textures != null && (textures.length == 0 || textures[0] != -1))
		{
			Arrays.fill(textures, (short) -1);
		}
	}

	private void restoreSelfColors(Model model)
	{
		SavedModelState state = selfCache.get(model);
		if (state == null)
		{
			return;
		}

		if (model.getFaceColors1() != null && state.c1 != null && model.getFaceColors1().length == state.c1.length)
		{
			System.arraycopy(state.c1, 0, model.getFaceColors1(), 0, state.c1.length);
		}
		if (model.getFaceColors2() != null && state.c2 != null && model.getFaceColors2().length == state.c2.length)
		{
			System.arraycopy(state.c2, 0, model.getFaceColors2(), 0, state.c2.length);
		}
		if (model.getFaceColors3() != null && state.c3 != null && model.getFaceColors3().length == state.c3.length)
		{
			System.arraycopy(state.c3, 0, model.getFaceColors3(), 0, state.c3.length);
		}
		if (model.getFaceRenderPriorities() != null && state.priorities != null
				&& model.getFaceRenderPriorities().length == state.priorities.length)
		{
			System.arraycopy(state.priorities, 0, model.getFaceRenderPriorities(), 0, state.priorities.length);
		}
		if (model.getFaceTextures() != null && state.textures != null
				&& model.getFaceTextures().length == state.textures.length)
		{
			System.arraycopy(state.textures, 0, model.getFaceTextures(), 0, state.textures.length);
		}
	}

	private int clampAlpha(int opacityPercent)
	{
		int alpha = (int) ((100 - opacityPercent) * 2.55);
		return Math.max(0, Math.min(255, alpha));
	}

	private int colorToHSL(Color color)
	{
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		return ((int) (hsb[0] * 63) << 10) | ((int) (hsb[1] * 7) << 7) | (int) (hsb[2] * 127);
	}

	private byte[] copy(byte[] a)
	{
		return a == null ? null : Arrays.copyOf(a, a.length);
	}

	private int[] copy(int[] a)
	{
		return a == null ? null : Arrays.copyOf(a, a.length);
	}

	private short[] copy(short[] a)
	{
		return a == null ? null : Arrays.copyOf(a, a.length);
	}

	private static class SavedModelState
	{
		final byte[] trans;
		final int[] c1;
		final int[] c2;
		final int[] c3;
		final byte[] priorities;
		final short[] textures;

		SavedModelState(byte[] t, int[] color1, int[] color2, int[] color3, byte[] p, short[] tex)
		{
			trans = t;
			c1 = color1;
			c2 = color2;
			c3 = color3;
			priorities = p;
			textures = tex;
		}
	}

	@Provides
	VisibilityEnhancerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(VisibilityEnhancerConfig.class);
	}
}