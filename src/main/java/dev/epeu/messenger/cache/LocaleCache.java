package dev.epeu.messenger.cache;

import com.google.common.collect.Maps;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class LocaleCache {
  public static LocaleCache createEmptyCache() {
    return new LocaleCache(Maps.newHashMap());
  }

  private final Map<UUID, Locale> locales;

  private LocaleCache(Map<UUID, Locale> locales) {
    this.locales = locales;
  }

  public void addLocale(UUID uuid, Locale locale) {
    locales.put(uuid, locale);
  }

  public void updateLocale(UUID uuid, Locale locale) {
    locales.replace(uuid, locale);
  }

  public void removeLocale(UUID uuid) {
    locales.remove(uuid);
  }

  private static final Locale DEFAULT_LOCALE = Locale.GERMAN;

  public Locale findLocaleOrDefault(UUID uuid) {
    return locales.get(uuid) != null
      ? locales.get(uuid)
      : DEFAULT_LOCALE;
  }
}