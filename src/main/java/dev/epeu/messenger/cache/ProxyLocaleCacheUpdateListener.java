package dev.epeu.messenger.cache;

import com.google.common.base.Preconditions;
import dev.epeu.proxycore.db.Redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Locale;
import java.util.UUID;

/**
 * epeu.dev I18n Framework
 * <p>
 * Last updated: Aug. 2024
 * <p>
 * <p>
 * Author:
 * <p>
 * Jan Christopher Bredow
 * <p>
 * runs async
 */
public final class ProxyLocaleCacheUpdateListener extends JedisPubSub {
  public static final String REDIS_CHANNEL = "messenger";

  public static ProxyLocaleCacheUpdateListener createWithCacheAndPool(
    LocaleCache cache,
    JedisPool pool
  ) {
    Preconditions.checkNotNull(cache);
    Preconditions.checkNotNull(pool);
    return new ProxyLocaleCacheUpdateListener(cache, pool);
  }

  private final LocaleCache cache;
  private final JedisPool pool;

  private ProxyLocaleCacheUpdateListener(
    LocaleCache cache,
    JedisPool pool
  ) {
    this.cache = cache;
    this.pool = pool;
  }

  private static final String THREAD_NAME = REDIS_CHANNEL + "update-listener";

  public void start() {
    new Thread(this::run, THREAD_NAME).start();
  }

  private void run() {
    try (Jedis jedis = pool.getResource()) {
      jedis.subscribe(this, REDIS_CHANNEL);
    }
  }

  public static final String ACTION = "UPDATE_MESSAGE";

  @Override
  public void onMessage(String channel, String message) {
    if (channel.equals(REDIS_CHANNEL)) {
      Redis request = Redis.parse(message);

      if (request.action().equals(ACTION)) {
        updateLocale(request.data());
      }
    }
  }

  private void updateLocale(Object[] data) {
    try {
      UUID uuid = UUID.fromString(String.valueOf(data[0]));
      Locale locale = Locale.forLanguageTag(String.valueOf(data[1]));
      cache.updateLocale(uuid, locale);
    } catch (Throwable failure) {
      failure.printStackTrace();
    }
  }
}