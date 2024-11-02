package dev.epeu.messenger;

import com.google.common.base.Preconditions;
import dev.epeu.messenger.cache.GameLocaleCacheListener;
import dev.epeu.messenger.cache.GameServerLocaleCacheUpdateListener;
import dev.epeu.messenger.cache.LocaleCache;
import dev.epeu.messenger.cache.ProxyLocaleCacheUpdateListener;
import dev.epeu.servercore.db.Redis;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;

import java.util.Locale;
import java.util.UUID;

public final class GameServerMessenger extends Messenger {
  private static final String UNDEFINED = "und";
  private static final String SPLIT = "_";
  private static final Locale DEFAULT_LOCALE = Locale.GERMAN;
  private static final String KEY = "messenger.locale.%s";


  public static GameServerMessenger createWithServerTypeAndPlugin(
    String serverType,
    Plugin plugin
  ) {
    Preconditions.checkNotNull(serverType);
    Preconditions.checkNotNull(plugin);

    GameServerMessenger messenger = new GameServerMessenger(serverType, plugin,
      Redis.pool,
      LocaleCache.createEmptyCache());
    messenger.register();
    return messenger;
  }

  private final LocaleCache cache;
  private final JedisPool pool;
  private final Plugin plugin;

  private GameServerMessenger(
    String serverType,
    Plugin plugin,
    JedisPool pool,
    LocaleCache cache
  ) {
    super(serverType);
    this.plugin = plugin;
    this.pool = pool;
    this.cache = cache;
  }

  @Override
  public void sendMessage(UUID uuid, String localeKey, Object... substitutes) {
    Player player = plugin.getServer().getPlayer(uuid);
    if (player != null) {
      player.sendMessage(component(uuid, localeKey, substitutes));
    }
  }

  private String component(UUID uuid, String localeKey, Object... substitutes){
    return ChatColor.translateAlternateColorCodes('&', translate(uuid, localeKey, substitutes));
  }

  @Override
  public void updateMessage(UUID uuid, Locale newLocale) {
    Locale locale = formatLocale(newLocale);
    Redis.emptyBuilder().publish(ProxyLocaleCacheUpdateListener.REDIS_CHANNEL,
      Redis.create(ProxyLocaleCacheUpdateListener.ACTION, uuid.toString(),
        locale.toLanguageTag()));
    Redis.emptyBuilder().set(createKey(uuid), locale.toLanguageTag());
  }


  private Locale formatLocale(Locale locale) {
    if (locale.getLanguage().contains(SPLIT)) {
      return Locale.forLanguageTag(locale.getLanguage().split(SPLIT)[0]
        + "-" + locale.getLanguage().split(SPLIT)[1].toUpperCase());
    } else if (locale.toLanguageTag().contains(UNDEFINED)) {
      return DEFAULT_LOCALE;
    }
    return locale;
  }



  private String createKey(UUID uuid) {
    return String.format(KEY, uuid);
  }

  private void register() {
    var manager = plugin.getServer().getPluginManager();
    manager.registerEvents(GameLocaleCacheListener.createWithCache(cache), plugin);
    GameServerLocaleCacheUpdateListener.createWithCacheAndPool(cache, pool).start();
  }

  @Override
  public Locale resolveLocaleFor(UUID uuid) {
    return cache.findLocaleOrDefault(uuid);
  }
}