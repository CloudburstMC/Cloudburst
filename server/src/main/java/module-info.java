module org.cloudburstmc.server {
    requires org.cloudburstmc.api;
    requires it.unimi.dsi.fastutil;
    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires nbt;
    requires com.nukkitx.protocol.bedrock.v465;
    requires com.nukkitx.protocol.bedrock.common;
    requires com.nukkitx.protocol.common;
    requires com.nukkitx.network.common;
    requires com.nukkitx.network.raknet;
    requires com.nukkitx.math;
    requires com.nukkitx.natives;
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
    requires terminalconsoleappender;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.jline.reader;
    requires org.objectweb.asm;
    requires org.slf4j;
    requires static org.checkerframework.checker.qual;
    requires block.state.updater;
    requires noise;
    requires random;
    requires jopt.simple;
    requires natives;

    exports org.cloudburstmc.server;
}