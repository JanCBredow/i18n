package dev.epeu.messenger.cache;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import dev.epeu.objects.UserCriterion;
import dev.epeu.proxycore.Redis;
import dev.epeu.repo.repos.UserRepository;

import java.util.Locale;
import java.util.UUID;

public final class ProxyLocaleCacheListener {
  public static ProxyLocaleCacheListener createWith(
    UserRepository repository,
    LocaleCache cache
  ) {
    Preconditions.checkNotNull(repository);
    Preconditions.checkNotNull(cache);
    return new ProxyLocaleCacheListener(repository, cache);
  }

  private final UserRepository repository;
  private final LocaleCache cache;

  private ProxyLocaleCacheListener(
    UserRepository repository,
    LocaleCache cache
  ) {
    this.repository = repository;
    this.cache = cache;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void addToCache(LoginEvent login) {
    UUID uuid = login.getPlayer().getUniqueId();

    if (!existsInRedis(uuid)) {
      repository.find(UserCriterion.UUID.criterion(), uuid.toString()).ifPresent(user -> {
        String locale = formatLocale(new Locale(user.locale())).toLanguageTag();
        addToRedis(uuid, locale);
        cache.addLocale(uuid, Locale.forLanguageTag(locale));
      });
    } else {
      cache.addLocale(uuid, Locale.forLanguageTag(Redis.emptyBuilder()
        .find(createKey(uuid))));
    }
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

  private void addToRedis(UUID uuid, String locale) {
    Redis.emptyBuilder().set(createKey(uuid), locale);
  }

  private boolean existsInRedis(UUID uuid) {
    return Redis.emptyBuilder().find(createKey(uuid)) != null;
  }

  @Subscribe
  public void removeFromCache(DisconnectEvent disconnect) {
    UUID uuid = disconnect.getPlayer().getUniqueId();

    cache.removeLocale(uuid);
    Redis.emptyBuilder().delete(createKey(uuid));
  }

  private static final String KEY = "messenger.locale.%s";

  private String createKey(UUID uuid) {
    return String.format(KEY, uuid);
  }
}