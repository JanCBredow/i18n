package dev.epeu.messenger;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Messenger {
  private final String serverType;

  Messenger(String serverType) {
    this.serverType = serverType;
  }

  public abstract void sendMessage(UUID uuid, String localeKey, Object... substitutes);

  public abstract Locale resolveLocaleFor(UUID uuid);
  public abstract void updateMessage(UUID uuid, Locale newLocale);

  public String testBundle(Locale locale, String key) {
    String fromBundle = translate(locale, key);
    if (fromBundle == null) {
      return "This key is not recognized.";
    }
    return fromBundle;
  }

  public boolean existsKey(String key, UUID uuid) {
    Locale locale = resolveLocaleFor(uuid);
    return existsKey(key, locale);
  }

  public boolean existsKey(String key, Locale locale) {
    try {
      ResourceBundle resourceBundle = fetchBundleByLocale(locale);
      resourceBundle.getString(key);
      return true;
    } catch (Throwable throwable) {
      return false;
    }
  }

  public List<String> resolveKeysForParentKey(String baseKey, UUID uuid) {
    Locale locale = resolveLocaleFor(uuid);
    return resolveKeysForParentKey(baseKey, locale);
  }

  public List<String> resolveKeysForParentKey(String baseKey, Locale locale) {
    try {
      ResourceBundle resourceBundle = fetchBundleByLocale(locale);

      return Lists.newArrayList(resourceBundle.getKeys().asIterator()).stream()
        .filter(key -> key.startsWith(baseKey))
        .collect(Collectors.toList());
    } catch (Throwable throwable) {
      return Lists.newArrayList();
    }
  }

  public String translate(UUID uuid, String localeKey, Object... options) {
    if (REPLACEMENTS.isEmpty()) {
      initReplacements();
    }
    try {
      Locale playerLocale = resolveLocaleFor(uuid);
      ResourceBundle resourceBundle = fetchBundleByLocale(playerLocale);
      String fromBundle;

      try {
        fromBundle = resourceBundle.getString(localeKey);
      } catch (MissingResourceException | NullPointerException notFoundFailure) {
        fromBundle = localeKey;
      }

      return processString(fromBundle, options);
    } catch (Throwable failure) {
      return localeKey;
    }
  }

  public String translate(Locale locale, String key) {
    if (REPLACEMENTS.isEmpty()) {
      initReplacements();
    }
    try {
      ResourceBundle resourceBundle = fetchBundleByLocale(locale);
      String fromBundle;

      try {
        fromBundle = resourceBundle.getString(key);
      } catch (MissingResourceException | NullPointerException notFoundFailure) {
        fromBundle = key;
      }

      return processString(fromBundle);
    } catch (Throwable failure) {
      return null;
    }
  }

  private static final Map<String, String> REPLACEMENTS = new HashMap<>();
  private static final String RESOURCE_BUNDLE_REPLACEMENT_FILE_PATH =
    "/opt/resource-bundles/replacements.properties";

  private void initReplacements() {
    try (BufferedReader reader =
      new BufferedReader(new FileReader(RESOURCE_BUNDLE_REPLACEMENT_FILE_PATH)))
    {
      String readLine;
      while ((readLine = reader.readLine()) != null) {
        String[] split = readLine.split("=");
        REPLACEMENTS.put(split[0], split[1].replace("%SPACE%", " "));
      }
    } catch (IOException readReplacementsFailure) {
      readReplacementsFailure.printStackTrace();
    }
  }

  public void refreshGlobalReplacements() {
    REPLACEMENTS.clear();
    initReplacements();
  }

  private String processString(String value, Object... options) {
    int lastIndex = value.indexOf("%");
    List<Integer> indices = new ArrayList<>();
    while (lastIndex != -1) {
      indices.add(lastIndex);
      lastIndex = value.indexOf("%", lastIndex + 1);
    }
    String toReplace = value;

    for (int index = 0; index < indices.size(); index += 2) {
      if (index == indices.size() - 1) {
        break;
      }
      int currentIndex = indices.get(index);
      int nextIndex = indices.get(index + 1);
      String substring = value.substring(currentIndex + 1, nextIndex);
      String replacement = Messenger.REPLACEMENTS.get(substring);
      toReplace = toReplace
        .replace("%" + substring + "%",
          (replacement == null ? "NOT_SET" : replacement));
    }
    return MessageFormat.format(toReplace, options);
  }

  private static final String RESOURCE_BUNDLE_PATH = "/opt/resource-bundles/";

  protected ResourceBundle fetchBundleByLocale(Locale locale) {
    File file = new File(RESOURCE_BUNDLE_PATH + serverType + "/");
    try {
      URL[] urls = new URL[]{
        file.toURI().toURL()
      };
      ClassLoader classLoader = new URLClassLoader(urls);

      return ResourceBundle.getBundle("messages", locale, classLoader);
    } catch (MalformedURLException | MissingResourceException findFailure) {
      File fallback = new File(RESOURCE_BUNDLE_PATH);
      try {
        URL[] urls = new URL[]{
          fallback.toURI().toURL()
        };
        ClassLoader classLoader = new URLClassLoader(urls);

        return ResourceBundle.getBundle("fallback", Locale.GERMAN, classLoader);
      } catch (MalformedURLException | MissingResourceException failure) {
        failure.printStackTrace();
      }
    }
    return ResourceBundle.getBundle("internal_fallback");
  }
}