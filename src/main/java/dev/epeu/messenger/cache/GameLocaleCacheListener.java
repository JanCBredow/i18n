package dev.epeu.messenger.cache;

import com.google.common.base.Preconditions;
import dev.epeu.servercore.db.Redis;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.UUID;

public final class GameLocaleCacheListener implements Listener {
  public static GameLocaleCacheListener createWithCache(LocaleCache cache) {
    Preconditions.checkNotNull(cache);
    return new GameLocaleCacheListener(cache);
  }

  private final LocaleCache cache;

  private GameLocaleCacheListener(LocaleCache cache) {
    this.cache = cache;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void cachePlayer(PlayerLoginEvent login) {
    UUID uuid = login.getPlayer().getUniqueId();

    Locale locale = findLanguageInRedisOrDefault(uuid);
    cache.addLocale(uuid, locale);
  }

  private static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  private Locale findLanguageInRedisOrDefault(UUID uuid) {
    String value = Redis.emptyBuilder().find(createKey(uuid));
    return value != null
      ? Locale.forLanguageTag(value)
      : DEFAULT_LOCALE;
  }

  private static final String KEY = "messenger.locale.%s";

  private String createKey(UUID uuid) {
    return String.format(KEY, uuid);
  }

  @EventHandler
  public void removeFromCache(PlayerQuitEvent quit) {
    UUID uuid = quit.getPlayer().getUniqueId();

    cache.removeLocale(uuid);
  }
}