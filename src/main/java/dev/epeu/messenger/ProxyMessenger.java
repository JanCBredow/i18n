package dev.epeu.messenger;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.epeu.messenger.cache.LocaleCache;
import dev.epeu.messenger.cache.ProxyLocaleCacheUpdateListener;
import dev.epeu.messenger.cache.ProxyLocaleCacheListener;
import dev.epeu.proxycore.ProxyCorePlugin;
import dev.epeu.proxycore.Redis;
import dev.epeu.repo.repos.UserRepository;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.JedisPool;

import java.util.Locale;
import java.util.UUID;

public class ProxyMessenger extends Messenger {
  public static ProxyMessenger createWithServerType(
    String serverType,
    ProxyServer server,
    Object plugin,
    UserRepository repository
  ) {
    Preconditions.checkNotNull(serverType);
    Preconditions.checkNotNull(server);
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(repository);
    ProxyMessenger messenger = new ProxyMessenger(serverType, server, plugin,
      repository, LocaleCache.createEmptyCache(), ProxyCorePlugin.pool);
    messenger.register();
    return messenger;
  }

  private final UserRepository repository;
  private final ProxyServer server;
  private final LocaleCache cache;
  private final JedisPool pool;
  private final Object plugin;

  private ProxyMessenger(
    String serverType,
    ProxyServer server,
    Object plugin,
    UserRepository repository,
    LocaleCache cache,
    JedisPool pool
  ) {
    super(serverType);
    this.server = server;
    this.plugin = plugin;
    this.cache = cache;
    this.pool = pool;
    this.repository = repository;
  }

  @Override
  public void sendMessage(UUID uuid, String localeKey, Object... substitutes) {
    server
      .getPlayer(uuid)
        .ifPresent(player -> player.sendMessage(
          LegacyComponentSerializer.legacy('§')
            .deserialize(translate(uuid, localeKey, substitutes))));
  }

  @Override
  public void updateMessage(UUID uuid, Locale newLocale) {
    Locale locale = formatLocale(newLocale);
    Redis.emptyBuilder().publish(ProxyLocaleCacheUpdateListener.REDIS_CHANNEL,
      Redis.create(ProxyLocaleCacheUpdateListener.ACTION, uuid.toString(),
        locale.toLanguageTag()));
    Redis.emptyBuilder().set(createKey(uuid), locale.toLanguageTag());
  }

  private static final String UNDEFINED = "und";
  private static final String SPLIT = "_";

  private Locale formatLocale(Locale locale) {
    if (locale.getLanguage().contains(SPLIT)) {
      return Locale.forLanguageTag(locale.getLanguage().split(SPLIT)[0]
        + "-" + locale.getLanguage().split(SPLIT)[1].toUpperCase());
    } else if (locale.toLanguageTag().contains(UNDEFINED)) {
      return Locale.GERMAN;
    }
    return locale;
  }

  private static final String KEY = "messenger.locale.%s";

  private String createKey(UUID uuid) {
    return String.format(KEY, uuid);
  }

  private void register() {
    var manager = server.getEventManager();
    manager.register(plugin, ProxyLocaleCacheListener.createWith(repository,
      cache));
    ProxyLocaleCacheUpdateListener.createWithCacheAndPool(cache, pool).start();
  }

  @Override
  public Locale resolveLocaleFor(UUID uuid) {
    return cache.findLocaleOrDefault(uuid);
  }
}