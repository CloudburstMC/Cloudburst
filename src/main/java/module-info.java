open module org.cloudburstmc.server {
    requires org.cloudburstmc.api;
    requires it.unimi.dsi.fastutil;
    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires com.nukkitx.math;
    requires com.nukkitx.natives;
    requires com.nukkitx.nbt;
    requires com.nukkitx.network.common;
    requires com.nukkitx.network.raknet;
    requires com.nukkitx.protocol.bedrock.common;
    requires com.nukkitx.protocol.bedrock.v544;
    requires com.nukkitx.protocol.common;
    requires common;
    requires static lombok;
    requires static javax.inject;
    requires static jsr305;
    requires io.netty.buffer;
    requires io.netty.common;
    requires io.netty.transport;
    requires upnp;
    requires math;
    requires completable.futures;
    requires leveldb.mcpe.jni;
    requires leveldb;
    requires leveldb.api;
    requires com.google.common;
    requires com.google.guice;
    requires com.nimbusds.jose.jwt;
    requires com.fasterxml.jackson.dataformat.javaprop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires net.minecrell.terminalconsole;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.jline.reader;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.slf4j;
    requires static org.checkerframework.checker.qual;
    requires block.state.updater;
    requires noise;
    requires random;
    requires jopt.simple;
    requires natives;
    requires jdk.unsupported;
    requires java.desktop;

    exports co.aikar.timings;
    exports org.cloudburstmc.server;
    exports org.cloudburstmc.server.block;
    exports org.cloudburstmc.server.block.behavior;
    exports org.cloudburstmc.server.block.serializer;
    exports org.cloudburstmc.server.block.serializer.util;
    exports org.cloudburstmc.server.block.trait;
    exports org.cloudburstmc.server.block.trait.serializer;
    exports org.cloudburstmc.server.block.util;
    exports org.cloudburstmc.server.blockentity;
    exports org.cloudburstmc.server.command;
    exports org.cloudburstmc.server.command.data;
    exports org.cloudburstmc.server.command.defaults;
    exports org.cloudburstmc.server.command.simple;
    exports org.cloudburstmc.server.config;
    exports org.cloudburstmc.server.config.serializer;
    exports org.cloudburstmc.server.console;
    exports org.cloudburstmc.server.crafting;
    exports org.cloudburstmc.server.dispenser;
    exports org.cloudburstmc.server.enchantment;
    exports org.cloudburstmc.server.enchantment.behavior;
    exports org.cloudburstmc.server.enchantment.behavior.bow;
    exports org.cloudburstmc.server.enchantment.behavior.damage;
    exports org.cloudburstmc.server.enchantment.behavior.loot;
    exports org.cloudburstmc.server.enchantment.behavior.protection;
    exports org.cloudburstmc.server.enchantment.behavior.trident;
    exports org.cloudburstmc.server.entity;
    exports org.cloudburstmc.server.entity.data;
    exports org.cloudburstmc.server.entity.hostile;
    exports org.cloudburstmc.server.entity.misc;
    exports org.cloudburstmc.server.entity.passive;
    exports org.cloudburstmc.server.entity.projectile;
    exports org.cloudburstmc.server.entity.vehicle;
    exports org.cloudburstmc.server.event;
    exports org.cloudburstmc.server.event.firehandler;
    exports org.cloudburstmc.server.event.player;
    exports org.cloudburstmc.server.event.server;
    exports org.cloudburstmc.server.form;
    exports org.cloudburstmc.server.form.element;
    exports org.cloudburstmc.server.form.response;
    exports org.cloudburstmc.server.form.util;
    exports org.cloudburstmc.server.inject;
    exports org.cloudburstmc.server.inject.provider;
    exports org.cloudburstmc.server.inject.provider.config;
    exports org.cloudburstmc.server.inventory;
    exports org.cloudburstmc.server.inventory.transaction;
    exports org.cloudburstmc.server.inventory.transaction.action;
    exports org.cloudburstmc.server.inventory.transaction.data;
    exports org.cloudburstmc.server.item;
    exports org.cloudburstmc.server.item.data.serializer;
    exports org.cloudburstmc.server.item.food;
    exports org.cloudburstmc.server.item.provider;
    exports org.cloudburstmc.server.item.randomitem;
    exports org.cloudburstmc.server.item.serializer;
    exports org.cloudburstmc.server.level;
    exports org.cloudburstmc.server.level.biome;
    exports org.cloudburstmc.server.level.chunk;
    exports org.cloudburstmc.server.level.chunk.bitarray;
    exports org.cloudburstmc.server.level.feature;
    exports org.cloudburstmc.server.level.feature.tree;
    exports org.cloudburstmc.server.level.generator;
    exports org.cloudburstmc.server.level.generator.impl;
    exports org.cloudburstmc.server.level.generator.standard;
    exports org.cloudburstmc.server.level.generator.standard.biome;
    exports org.cloudburstmc.server.level.generator.standard.biome.map;
    exports org.cloudburstmc.server.level.generator.standard.biome.map.complex;
    exports org.cloudburstmc.server.level.generator.standard.biome.map.complex.filter;
    exports org.cloudburstmc.server.level.generator.standard.finish;
    exports org.cloudburstmc.server.level.generator.standard.generation.decorator;
    exports org.cloudburstmc.server.level.generator.standard.generation.density;
    exports org.cloudburstmc.server.level.generator.standard.generation.noise;
    exports org.cloudburstmc.server.level.generator.standard.misc;
    exports org.cloudburstmc.server.level.generator.standard.misc.filter;
    exports org.cloudburstmc.server.level.generator.standard.misc.selector;
    exports org.cloudburstmc.server.level.generator.standard.population;
    exports org.cloudburstmc.server.level.generator.standard.population.cluster;
    exports org.cloudburstmc.server.level.generator.standard.population.plant;
    exports org.cloudburstmc.server.level.generator.standard.population.tree;
    exports org.cloudburstmc.server.level.generator.standard.registry;
    exports org.cloudburstmc.server.level.generator.standard.store;
    exports org.cloudburstmc.server.level.manager;
    exports org.cloudburstmc.server.level.particle;
    exports org.cloudburstmc.server.level.provider;
    exports org.cloudburstmc.server.level.provider.anvil;
    exports org.cloudburstmc.server.level.provider.anvil.palette;
    exports org.cloudburstmc.server.level.provider.leveldb;
    exports org.cloudburstmc.server.level.provider.leveldb.serializer;
    exports org.cloudburstmc.server.level.storage;
    exports org.cloudburstmc.server.locale;
    exports org.cloudburstmc.server.math;
    exports org.cloudburstmc.server.metrics;
    exports org.cloudburstmc.server.network;
    exports org.cloudburstmc.server.network.protocol.types;
    exports org.cloudburstmc.server.network.query;
    exports org.cloudburstmc.server.pack;
    exports org.cloudburstmc.server.pack.loader;
    exports org.cloudburstmc.server.permission;
    exports org.cloudburstmc.server.player;
    exports org.cloudburstmc.server.player.handler;
    exports org.cloudburstmc.server.player.manager;
    exports org.cloudburstmc.server.plugin;
    exports org.cloudburstmc.server.plugin.loader;
    exports org.cloudburstmc.server.plugin.loader.java;
    exports org.cloudburstmc.server.plugin.util;
    exports org.cloudburstmc.server.potion;
    exports org.cloudburstmc.server.registry;
    exports org.cloudburstmc.server.registry.behavior;
    exports org.cloudburstmc.server.scheduler;
    exports org.cloudburstmc.server.timings;
    exports org.cloudburstmc.server.utils;
    exports org.cloudburstmc.server.utils.bugreport;
    exports org.cloudburstmc.server.registry.behavior.proxy;
}