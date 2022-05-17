package com.xxmicloxx.NoteBlockAPI;

import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitWorker;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class; contains methods for playing and adjusting songs for players
 */
public class NoteBlockAPI implements Listener {

	static Plugin getInstance;
	private static NoteBlockAPI api;

	private Map<UUID, ArrayList<SongPlayer>> playingSongs = new ConcurrentHashMap<UUID, ArrayList<SongPlayer>>();
	private Map<UUID, Byte> playerVolume = new ConcurrentHashMap<UUID, Byte>();

	private HashMap<Plugin, Boolean> dependentPlugins = new HashMap<>();

	/**
	 *
	 *   Constructor
	 *
	 */

	public NoteBlockAPI(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		getInstance = plugin;
		api = this;

		for (Plugin pl : Bukkit.getServer().getPluginManager().getPlugins()){
			if (pl.getDescription().getDepend().contains("NoteBlockAPI") || pl.getDescription().getSoftDepend().contains("NoteBlockAPI")){
				dependentPlugins.put(pl, false);
			}
		}

		new NoteBlockPlayerMain().onEnable();

		Bukkit.getServer().getScheduler().runTaskLater(getInstance, new Runnable() {
			@Override
			public void run() {
				Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
				Type[] types = new Type[]{PlayerRangeStateChangeEvent.class, SongDestroyingEvent.class, SongEndEvent.class, SongStoppedEvent.class };
				for (Plugin plugin : plugins) {
					ArrayList<RegisteredListener> rls = HandlerList.getRegisteredListeners(plugin);
					for (RegisteredListener rl : rls) {
						Method[] methods = rl.getListener().getClass().getDeclaredMethods();
						for (Method m : methods) {
							Type[] params = m.getParameterTypes();
							param:
							for (Type paramType : params) {
								for (Type type : types){
									if (paramType.equals(type)) {
										dependentPlugins.put(plugin, true);
										break param;
									}
								}
							}
						}

					}
				}

			}
		}, 1);

	}

	private void onDisable() {
		Bukkit.getScheduler().cancelTasks(getInstance);
		List<BukkitWorker> workers = Bukkit.getScheduler().getActiveWorkers();
		for (BukkitWorker worker : workers){
			if (!worker.getOwner().equals(this))
				continue;
			worker.getThread().interrupt();
		}
		NoteBlockPlayerMain.plugin.onDisable();
	}

	@EventHandler
	private void onDisablePluginEvent(PluginDisableEvent Event) {
		if (!Event.getPlugin().getName().equals(getInstance.getName())) return;

		onDisable();
	}

	public void doSync(Runnable runnable) {
		Bukkit.getServer().getScheduler().runTask(getInstance, runnable);
	}

	public void doAsync(Runnable runnable) { Bukkit.getServer().getScheduler().runTaskAsynchronously(getInstance, runnable); }

	public static NoteBlockAPI getAPI(){ return api; }


	/**
	 *
	 *   SongPlayerAPI
	 *
	 */

	/**
	 * Returns true if a Player is currently receiving a song
	 * @param player
	 * @return is receiving a song
	 */
	public static boolean isReceivingSong(Player player) {
		return isReceivingSong(player.getUniqueId());
	}

	/**
	 * Returns true if a Player with specified UUID is currently receiving a song
	 * @param uuid
	 * @return is receiving a song
	 */
	public static boolean isReceivingSong(UUID uuid) {
		ArrayList<SongPlayer> songs = api.playingSongs.get(uuid);
		return (songs != null && !songs.isEmpty());
	}

	/**
	 * Stops the song for a Player
	 * @param player
	 */
	public static void stopPlaying(Player player) {
		stopPlaying(player.getUniqueId());
	}

	/**
	 * Stops the song for a Player
	 * @param uuid
	 */
	public static void stopPlaying(UUID uuid) {
		ArrayList<SongPlayer> songs = api.playingSongs.get(uuid);
		if (songs == null) {
			return;
		}
		for (SongPlayer songPlayer : songs) {
			songPlayer.removePlayer(uuid);
		}
	}

	/**
	 * Sets the volume for a given Player
	 * @param player
	 * @param volume
	 */
	public static void setPlayerVolume(Player player, byte volume) {
		setPlayerVolume(player.getUniqueId(), volume);
	}

	/**
	 * Sets the volume for a given Player
	 * @param uuid
	 * @param volume
	 */
	public static void setPlayerVolume(UUID uuid, byte volume) {
		api.playerVolume.put(uuid, volume);
	}

	/**
	 * Gets the volume for a given Player
	 * @param player
	 * @return volume (byte)
	 */
	public static byte getPlayerVolume(Player player) {
		return getPlayerVolume(player.getUniqueId());
	}

	/**
	 * Gets the volume for a given Player
	 * @param uuid
	 * @return volume (byte)
	 */
	public static byte getPlayerVolume(UUID uuid) {
		Byte byteObj = api.playerVolume.get(uuid);
		if (byteObj == null) {
			byteObj = 100;
			api.playerVolume.put(uuid, byteObj);
		}
		return byteObj;
	}

	public static ArrayList<SongPlayer> getSongPlayersByPlayer(Player player){
		return getSongPlayersByPlayer(player.getUniqueId());
	}

	public static ArrayList<SongPlayer> getSongPlayersByPlayer(UUID player){
		return api.playingSongs.get(player);
	}

	public static void setSongPlayersByPlayer(Player player, ArrayList<SongPlayer> songs){
		setSongPlayersByPlayer(player.getUniqueId(), songs);
	}

	public static void setSongPlayersByPlayer(UUID player, ArrayList<SongPlayer> songs){
		api.playingSongs.put(player, songs);
	}
	
}
