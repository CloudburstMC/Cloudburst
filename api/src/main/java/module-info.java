module org.cloudburstmc.api {
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.dataformat.javaprop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.checkerframework.checker.qual;
    requires com.google.common;
    requires lombok;
    requires javax.inject;
    requires math;
    requires com.google.guice;
    requires java.desktop;

    exports org.cloudburstmc.api;
}