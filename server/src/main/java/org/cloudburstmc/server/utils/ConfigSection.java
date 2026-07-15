package org.cloudburstmc.server.utils;

import java.util.*;

/**
 * A hierarchical configuration section with support for dot-separated paths.
 * Nested maps are normalized to {@code ConfigSection} instances.
 */
public class ConfigSection extends LinkedHashMap<String, Object> {
    public ConfigSection() {
        super();
    }

    public ConfigSection(String key, Object value) {
        this();
        this.set(key, value);
    }

    public ConfigSection(Map<String, ?> map) {
        this();
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                super.put(entry.getKey(), fromMap(nestedMap));
            } else if (entry.getValue() instanceof List<?> list) {
                super.put(entry.getKey(), parseList(list));
            } else {
                super.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static ConfigSection fromMap(Map<?, ?> map) {
        ConfigSection section = new ConfigSection();
        map.forEach((key, value) -> {
            if (!(key instanceof String stringKey)) {
                throw new IllegalArgumentException("Configuration keys must be strings");
            }
            section.set(stringKey, normalize(value));
        });
        return section;
    }

    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            return fromMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            list.forEach(element -> normalized.add(normalize(element)));
            return normalized;
        }
        return value;
    }

    private List<Object> parseList(List<?> list) {
        List<Object> newList = new ArrayList<>();

        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                newList.add(fromMap(map));
            } else {
                newList.add(o);
            }
        }

        return newList;
    }

    public Map<String, Object> getAllMap() {
        return new LinkedHashMap<>(this);
    }

    public ConfigSection getAll() {
        return new ConfigSection(this);
    }

    public Object get(String key) {
        return this.get(key, null);
    }

    public Object get(String key, Object defaultValue) {
        if (key == null || key.isEmpty()) return defaultValue;
        if (super.containsKey(key)) return super.get(key);
        String[] keys = key.split("\\.", 2);
        if (!super.containsKey(keys[0])) return defaultValue;
        Object value = super.get(keys[0]);
        if (value instanceof ConfigSection) {
            ConfigSection section = (ConfigSection) value;
            return section.get(keys[1], defaultValue);
        }
        return defaultValue;
    }

    public void set(String key, Object value) {
        String[] subKeys = key.split("\\.", 2);
        if (subKeys.length > 1) {
            ConfigSection childSection = new ConfigSection();
            if (this.containsKey(subKeys[0]) && super.get(subKeys[0]) instanceof ConfigSection)
                childSection = (ConfigSection) super.get(subKeys[0]);
            childSection.set(subKeys[1], value);
            super.put(subKeys[0], childSection);
        } else super.put(subKeys[0], value);
    }

    public boolean isSection(String key) {
        Object value = this.get(key);
        return value instanceof ConfigSection;
    }

    public ConfigSection getSection(String key) {
        Object value = this.get(key, new ConfigSection());
        return value instanceof ConfigSection section ? section : new ConfigSection();
    }

    public ConfigSection getSections() {
        return getSections(null);
    }

    /**
     * Returns only the direct child sections at the requested path.
     */
    public ConfigSection getSections(String key) {
        ConfigSection sections = new ConfigSection();
        ConfigSection parent = key == null || key.isEmpty() ? this.getAll() : getSection(key);
        if (parent == null) return sections;
        parent.forEach((key1, value) -> {
            if (value instanceof ConfigSection)
                sections.put(key1, value);
        });
        return sections;
    }

    public int getInt(String key) {
        return this.getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return ((Number) this.get(key, defaultValue)).intValue();
    }

    public boolean isInt(String key) {
        Object var = get(key);
        return var instanceof Integer;
    }

    public long getLong(String key) {
        return this.getLong(key, 0);
    }

    public long getLong(String key, long defaultValue) {
        return ((Number) this.get(key, defaultValue)).longValue();
    }

    public boolean isLong(String key) {
        Object var = get(key);
        return var instanceof Long;
    }

    public double getDouble(String key) {
        return this.getDouble(key, 0);
    }

    public double getDouble(String key, double defaultValue) {
        return ((Number) this.get(key, defaultValue)).doubleValue();
    }

    public boolean isDouble(String key) {
        Object var = get(key);
        return var instanceof Double;
    }

    public String getString(String key) {
        return this.getString(key, "");
    }

    public String getString(String key, String defaultValue) {
        Object result = this.get(key, defaultValue);
        return String.valueOf(result);
    }

    public boolean isString(String key) {
        Object var = get(key);
        return var instanceof String;
    }

    public boolean getBoolean(String key) {
        return this.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = this.get(key, defaultValue);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    public boolean isBoolean(String key) {
        Object var = get(key);
        return var instanceof Boolean;
    }

    public List<?> getList(String key) {
        return this.getList(key, null);
    }

    public List<?> getList(String key, List<?> defaultList) {
        Object value = this.get(key, defaultList);
        return value instanceof List<?> list ? list : defaultList;
    }

    public boolean isList(String key) {
        Object var = get(key);
        return var instanceof List;
    }

    public List<String> getStringList(String key) {
        List<?> value = this.getList(key);
        if (value == null) {
            return new ArrayList<>(0);
        }
        List<String> result = new ArrayList<>();
        for (Object o : value) {
            if (o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character) {
                result.add(String.valueOf(o));
            }
        }
        return result;
    }

    public List<Integer> getIntegerList(String key) {
        List<?> list = getList(key);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Integer> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Integer) {
                result.add((Integer) object);
            } else if (object instanceof String) {
                try {
                    result.add(Integer.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((int) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        }
        return result;
    }

    public List<Boolean> getBooleanList(String key) {
        List<?> list = getList(key);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Boolean> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            } else if (object instanceof String) {
                if (Boolean.TRUE.toString().equals(object)) {
                    result.add(true);
                } else if (Boolean.FALSE.toString().equals(object)) {
                    result.add(false);
                }
            }
        }
        return result;
    }

    public List<Double> getDoubleList(String key) {
        List<?> list = getList(key);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Double> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Double) {
                result.add((Double) object);
            } else if (object instanceof String) {
                try {
                    result.add(Double.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((double) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        }
        return result;
    }

    public List<Float> getFloatList(String key) {
        List<?> list = getList(key);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Float> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Float) {
                result.add((Float) object);
            } else if (object instanceof String) {
                try {
                    result.add(Float.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((float) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        }
        return result;
    }

    public List<Long> getLongList(String key) {
        List<?> list = getList(key);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Long> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Long) {
                result.add((Long) object);
            } else if (object instanceof String) {
                try {
                    result.add(Long.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((long) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        }
        return result;
    }

    public List<Byte> getByteList(String key) {
        List<?> list = getList(key);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Byte> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Byte) {
                result.add((Byte) object);
            } else if (object instanceof String) {
                try {
                    result.add(Byte.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((byte) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        }

        return result;
    }

    public List<Character> getCharacterList(String key) {
        List<?> list = getList(key);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Character> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String) {
                String str = (String) object;

                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        }

        return result;
    }

    public List<Short> getShortList(String key) {
        List<?> list = getList(key);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Short> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Short) {
                result.add((Short) object);
            } else if (object instanceof String) {
                try {
                    result.add(Short.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((short) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        }

        return result;
    }

    public List<Map<String, Object>> getMapList(String key) {
        List<?> list = getList(key);
        List<Map<String, Object>> result = new ArrayList<>();

        if (list == null) {
            return result;
        }

        for (Object object : list) {
            if (object instanceof ConfigSection section) {
                result.add(section);
            } else if (object instanceof Map<?, ?> map) {
                result.add(fromMap(map));
            }
        }

        return result;
    }

    public boolean exists(String key, boolean ignoreCase) {
        if (ignoreCase) key = key.toLowerCase();
        for (String existKey : this.getKeys(true)) {
            if (ignoreCase) existKey = existKey.toLowerCase();
            if (existKey.equals(key)) return true;
        }
        return false;
    }

    public boolean exists(String key) {
        return exists(key, false);
    }

    public void remove(String key) {
        if (key == null || key.isEmpty()) return;
        if (super.containsKey(key)) super.remove(key);
        else if (this.containsKey(".")) {
            String[] keys = key.split("\\.", 2);
            if (super.get(keys[0]) instanceof ConfigSection) {
                ConfigSection section = (ConfigSection) super.get(keys[0]);
                section.remove(keys[1]);
            }
        }
    }

    public Set<String> getKeys(boolean child) {
        Set<String> keys = new LinkedHashSet<>();
        this.forEach((key, value) -> {
            keys.add(key);
            if (value instanceof ConfigSection) {
                if (child)
                    ((ConfigSection) value).getKeys(true).forEach(childKey -> keys.add(key + "." + childKey));
            }
        });
        return keys;
    }

    public Set<String> getKeys() {
        return this.getKeys(true);
    }
}
