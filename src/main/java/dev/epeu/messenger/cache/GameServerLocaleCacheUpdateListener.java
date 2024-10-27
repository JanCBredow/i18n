package dev.epeu.messenger.cache;

import com.google.common.base.Preconditions;
import dev.epeu.servercore.Redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Locale;
import java.util.UUID;

public final class GameServerLocaleCacheUpdateListener extends JedisPubSub {
  public static final String REDIS_CHANNEL = "messenger";
  private static final String THREAD_NAME = REDIS_CHANNEL + "update-listener";
  public static final String ACTION = "UPDATE_MESSAGE";

  public static GameServerLocaleCacheUpdateListener createWithCacheAndPool(
    LocaleCache cache,
    JedisPool pool
  ) {
    Preconditions.checkNotNull(cache);
    Preconditions.checkNotNull(pool);
    return new GameServerLocaleCacheUpdateListener(cache, pool);
  }

  private final LocaleCache cache;
  private final JedisPool pool;

  private GameServerLocaleCacheUpdateListener(
    LocaleCache cache,
    JedisPool pool
  ) {
    this.cache = cache;
    this.pool = pool;
  }


  public void start() {
    new Thread(this::run, THREAD_NAME).start();
  }

  private void run() {
    try (Jedis jedis = pool.getResource()) {
      jedis.subscribe(this, REDIS_CHANNEL);
    }
  }


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