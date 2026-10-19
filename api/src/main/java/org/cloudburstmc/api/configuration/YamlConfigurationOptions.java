package org.cloudburstmc.api.configuration;

/**
 * Controls YAML formatting, comment processing, and document limits.
 */
public class YamlConfigurationOptions {
    private int indent = 2;
    private int width = 80;
    private boolean parseComments = true;
    private int codePointLimit = 3000000;
    private int maxAliases = 50;
    private int nestingDepthLimit = 50;

    /**
     * Returns indentation in spaces. The default is 2.
     */
    public int indent() {
        return indent;
    }

    /**
     * Sets indentation in spaces, from 2 through 9.
     *
     * @param value spaces per indentation level
     * @return these options
     * @throws IllegalArgumentException if the value is outside 2 through 9
     */
    public YamlConfigurationOptions indent(int value) {
        if (value < 2 || value > 9) {
            throw new IllegalArgumentException("Invalid indent: " + value);
        }

        indent = value;
        return this;
    }

    /**
     * Returns the preferred output line width. The default is 80.
     */
    public int width() {
        return width;
    }

    /**
     * Sets preferred output line width, greater than zero.
     *
     * @param value the preferred width, not a hard limit on line length
     * @return these options
     * @throws IllegalArgumentException if the value is less than 1
     */
    public YamlConfigurationOptions width(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Invalid width: " + value);
        }

        width = value;
        return this;
    }

    /**
     * Returns whether comments are read and written. Enabled by default.
     */
    public boolean parseComments() {
        return parseComments;
    }

    /**
     * Sets whether comments are read and written.
     *
     * @param value whether to preserve comments
     * @return these options
     */
    public YamlConfigurationOptions parseComments(boolean value) {
        parseComments = value;
        return this;
    }

    /**
     * Returns the maximum document size in Unicode code points. The default is 3,000,000.
     */
    public int codePointLimit() {
        return codePointLimit;
    }

    /**
     * Sets maximum document size in Unicode code points.
     *
     * @param value the maximum size, greater than zero
     * @return these options
     * @throws IllegalArgumentException if the value is less than 1
     */
    public YamlConfigurationOptions codePointLimit(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Invalid codePointLimit: " + value);
        }

        codePointLimit = value;
        return this;
    }

    /**
     * Returns the maximum collection aliases allowed in a document. The default is 50.
     */
    public int maxAliases() {
        return maxAliases;
    }

    /**
     * Sets maximum collection aliases allowed in a document.
     *
     * @param value the maximum count, or zero to reject all collection aliases
     * @return these options
     * @throws IllegalArgumentException if the value is negative
     */
    public YamlConfigurationOptions maxAliases(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid maxAliases: " + value);
        }

        maxAliases = value;
        return this;
    }

    /**
     * Returns the maximum YAML collection nesting depth. The default is 50.
     */
    public int nestingDepthLimit() {
        return nestingDepthLimit;
    }

    /**
     * Sets maximum YAML collection nesting depth.
     *
     * @param value the maximum depth, greater than zero
     * @return these options
     * @throws IllegalArgumentException if the value is less than 1
     */
    public YamlConfigurationOptions nestingDepthLimit(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Invalid nestingDepthLimit: " + value);
        }

        nestingDepthLimit = value;
        return this;
    }

}
