package org.cloudburstmc.api.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonSerialize(using = ToStringSerializer.class)
public final class Identifier implements Comparable<Identifier> {
    private static final char NAMESPACE_SEPARATOR = ':';

    public static final Identifier EMPTY = new Identifier("", "", String.valueOf(NAMESPACE_SEPARATOR));

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "^(?>minecraft:)?(?>([a-z0-9_.]*)" + NAMESPACE_SEPARATOR + ")?([a-zA-Z0-9_.]*)$");

    private static final ThreadLocal<Matcher> MATCHER_THREAD_LOCAL = ThreadLocal.withInitial(
            () -> IDENTIFIER_PATTERN.matcher(""));

    private static final Lock READ_LOCK;
    private static final Lock WRITE_LOCK;

    private static final Map<String, Identifier> VALUES = new TreeMap<>();

    static {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        READ_LOCK = lock.readLock();
        WRITE_LOCK = lock.writeLock();
    }

    private final String namespace;
    private final String name;
    private final String fullName;

    private final transient int hashCode;

    private Identifier(String namespace, String name, String fullName) {
        this.namespace = namespace;
        this.name = name;
        this.fullName = fullName;
        this.hashCode = System.identityHashCode(this); // Precompute identity hash code and store it in a local field, this is ever so slightly faster
    }

    @JsonCreator
    public static Identifier parse(String identifier) {
        if (Preconditions.checkNotNull(identifier, "identifier").isEmpty()) {
            //check for empty before using matcher
            return EMPTY;
        }
        Matcher matcher = MATCHER_THREAD_LOCAL.get().reset(identifier);
        Preconditions.checkArgument(matcher.find(), "Invalid identifier: \"%s\"", identifier);

        Identifier id;
        READ_LOCK.lock();
        try {
            id = VALUES.get(identifier);
        } finally {
            READ_LOCK.unlock();
        }

        if (id == null) {
            String namespace = matcher.group(1);
            String name = matcher.group(2);
            String fullName = namespace == null && !identifier.startsWith("minecraft:") ? "minecraft:" + name : identifier;

            //create new identifier instance
            WRITE_LOCK.lock();
            try {
                //try get again in case identifier was created while obtaining write lock
                if ((id = VALUES.get(identifier)) == null) {
                    id = new Identifier(namespace == null ? "minecraft" : namespace, name, fullName);
                    if (namespace == null) {
                        //also put it into the map without minecraft: in the key to facilitate faster lookups when the prefix is omitted
                        VALUES.put(name, id);
                    }
                    VALUES.put(fullName, id);
                }
            } finally {
                WRITE_LOCK.unlock();
            }
        }
        return id;
    }

    public static Identifier from(String space, String name) {
        if (Strings.isNullOrEmpty(space)) {
            if (Strings.isNullOrEmpty(name)) {
                return EMPTY;
            } else {
                //assume minecraft namespace
                return parse(name);
            }
        }
        return parse(space + NAMESPACE_SEPARATOR + name);
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public int compareTo(Identifier o) {
        return this.fullName.compareTo(o.fullName);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
