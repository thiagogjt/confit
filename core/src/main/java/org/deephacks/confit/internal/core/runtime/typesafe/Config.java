/**
 *   Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package org.deephacks.confit.internal.core.runtime.typesafe;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An immutable map from typesafe paths to typesafe values.
 *
 * <p>
 * Contrast with {@link ConfigObject} which is a map from typesafe <em>keys</em>,
 * rather than paths, to typesafe values. A {@code Config} contains a tree of
 * {@code ConfigObject}, and {@link Config#root()} returns the tree's root
 * object.
 *
 * <p>
 * Throughout the API, there is a distinction between "keys" and "paths". A key
 * is a key in a JSON object; it's just a string that's the key in a map. A
 * "path" is a parseable expression with a syntax and it refers to a series of
 * keys. Path expressions are described in the <a
 * href="https://github.com/typesafehub/typesafe/blob/master/HOCON.md">spec for
 * Human-Optimized Config Object Notation</a>. In brief, a path is
 * period-separated so "a.b.c" looks for key c in object b in object a in the
 * root object. Sometimes double quotes are needed around special characters in
 * path expressions.
 *
 * <p>
 * The API for a {@code Config} is in terms of path expressions, while the API
 * for a {@code ConfigObject} is in terms of keys. Conceptually, {@code Config}
 * is a one-level map from <em>paths</em> to values, while a
 * {@code ConfigObject} is a tree of nested maps from <em>keys</em> to values.
 *
 * <p>
 * Use {@link ConfigUtil#joinPath} and {@link ConfigUtil#splitPath} to convert
 * between path expressions and individual path elements (keys).
 *
 * <p>
 * Another difference between {@code Config} and {@code ConfigObject} is that
 * conceptually, {@code ConfigValue}s with a {@link ConfigValue#valueType()
 * valueType()} of {@link ConfigValueType#NULL NULL} exist in a
 * {@code ConfigObject}, while a {@code Config} treats null values as if they
 * were missing.
 *
 * <p>
 * {@code Config} is an immutable object and thus safe to use from multiple
 * threads. There's never a need for "defensive copies."
 *
 * <p>
 * The "getters" on a {@code Config} list work in the same way. They never return
 * null, nor do they return a {@code ConfigValue} with
 * {@link ConfigValue#valueType() valueType()} of {@link ConfigValueType#NULL
 * NULL}. Instead, they throw {@link ConfigException.Missing} if the value is
 * completely absent or set to null. If the value is set to null, a subtype of
 * {@code ConfigException.Missing} called {@link ConfigException.Null} will be
 * thrown. {@link ConfigException.WrongType} will be thrown anytime you ask for
 * a type and the value has an incompatible type. Reasonable type conversions
 * are performed for you though.
 *
 * <p>
 * If you want to iterate over the contents of a {@code Config}, you can get its
 * {@code ConfigObject} with {@link #root()}, and then iterate over the
 * {@code ConfigObject} (which implements <code>java.util.Map</code>). Or, you
 * can use {@link #entrySet()} which recurses the object tree for you and builds
 * up a <code>Set</code> of list path-value pairs where the value is not null.
 *
 * <p>Before using a {@code Config} it's necessary to call {@link Config#resolve()}
 * to handle substitutions (though {@link ConfigFactory#load()} and similar methods
 * will do the resolve for you already).
 *
 * <p> You can find an example app and library <a
 * href="https://github.com/typesafehub/typesafe/tree/master/examples">on
 * GitHub</a>.  Also be sure to read the <a
 * href="package-summary.html#package_description">package
 * overview</a> which describes the big picture as shown in those
 * examples.
 *
 * <p>
 * <em>Do not implement {@code Config}</em>; it should only be implemented by
 * the typesafe library. Arbitrary implementations will not work because the
 * library internals assume a specific concrete implementation. Also, this
 * interface is likely to grow new methods over time, so third-party
 * implementations will break.
 */
public interface Config extends ConfigMergeable {
    /**
     * Gets the {@code Config} as a tree of {@link ConfigObject}. This is a
     * constant-time operation (it is not proportional to the number of values
     * in the {@code Config}).
     *
     * @return the root object in the configuration
     */
    ConfigObject root();

    /**
     * Gets the origin of the {@code Config}, which may be a file, or a file
     * with a line number, or just a descriptive phrase.
     *
     * @return the origin of the {@code Config} for use in error messages
     */
    ConfigOrigin origin();

    @Override
    Config withFallback(ConfigMergeable other);

    /**
     * Returns a replacement typesafe with list substitutions (the
     * <code>${foo.bar}</code> syntax, see <a
     * href="https://github.com/typesafehub/typesafe/blob/master/HOCON.md">the
     * spec</a>) resolved. Substitutions are looked up using this
     * <code>Config</code> as the root object, that is, a substitution
     * <code>${foo.bar}</code> will be replaced with the result of
     * <code>getValue("foo.bar")</code>.
     *
     * <p>
     * This method uses {@link ConfigResolveOptions#defaults()}, there is
     * another variant {@link Config#resolve(ConfigResolveOptions)} which lets
     * you specify non-default options.
     *
     * <p>
     * A given {@link Config} must be resolved before using it to retrieve
     * typesafe values, but ideally should be resolved one time for your entire
     * stack of fallbacks (see {@link Config#withFallback}). Otherwise, some
     * substitutions that could have resolved with list fallbacks available may
     * not resolve, which will be a user-visible oddity.
     *
     * <p>
     * <code>resolve()</code> should be invoked on root typesafe objects, rather
     * than on a subtree (a subtree is the result of something like
     * <code>typesafe.getConfig("foo")</code>). The problem with
     * <code>resolve()</code> on a subtree is that substitutions are relative to
     * the root of the typesafe and the subtree will have no way to get values
     * from the root. For example, if you did
     * <code>typesafe.getConfig("foo").resolve()</code> on the below typesafe file,
     * it would not work:
     *
     * <pre>
     *   common-value = 10
     *   foo {
     *      whatever = ${common-value}
     *   }
     * </pre>
     *
     * <p>
     * Many methods on {@link ConfigFactory} such as {@link
     * ConfigFactory#load()} automatically resolve the loaded
     * <code>Config</code> on the loaded stack of typesafe files.
     *
     * <p> Resolving an already-resolved typesafe is a harmless
     * no-op, but again, it is best to resolve an entire stack of
     * fallbacks (such as list your typesafe files combined) rather
     * than resolving each one individually.
     *
     * @return an immutable object with substitutions resolved
     * @throws ConfigException.UnresolvedSubstitution
     *             if any substitutions refer to nonexistent paths
     * @throws ConfigException
     *             some other typesafe exception if there are other problems
     */
    Config resolve();

    /**
     * Like {@link Config#resolve()} but allows you to specify non-default
     * options.
     *
     * @param options
     *            resolve options
     * @return the resolved <code>Config</code>
     */
    Config resolve(ConfigResolveOptions options);

    /**
     * Validates this typesafe against a reference typesafe, throwing an exception
     * if it is invalid. The purpose of this method is to "fail early" with a
     * comprehensive list of problems; in general, anything this method can find
     * would be detected later when trying to use the typesafe, but it's often
     * more user-friendly to fail right away when loading the typesafe.
     *
     * <p>
     * Using this method is always optional, since you can "fail late" instead.
     *
     * <p>
     * You must restrict validation to paths you "own" (those whose meaning are
     * defined by your code module). If you validate globally, you may trigger
     * errors about paths that happen to be in the typesafe but have nothing to do
     * with your module. It's best to allow the modules owning those paths to
     * validate them. Also, if every module validates only its own stuff, there
     * isn't as much redundant work being done.
     *
     * <p>
     * If no paths are specified in <code>checkValid()</code>'s parameter list,
     * validation is for the entire typesafe.
     *
     * <p>
     * If you specify paths that are not in the reference typesafe, those paths
     * are ignored. (There's nothing to validate.)
     *
     * <p>
     * Here's what validation involves:
     *
     * <ul>
     * <li>All paths found in the reference typesafe must be present in this
     * typesafe or an exception will be thrown.
     * <li>
     * Some changes in type from the reference typesafe to this typesafe will cause
     * an exception to be thrown. Not list potential type problems are detected,
     * in particular it's assumed that strings are compatible with everything
     * except objects and lists. This is because string types are often "really"
     * some other type (system properties always start out as strings, or a
     * string like "5ms" could be used with {@link #getMilliseconds}). Also,
     * it's allowed to set any type to null or override null with any type.
     * <li>
     * Any unresolved substitutions in this typesafe will cause a validation
     * failure; both the reference typesafe and this typesafe should be resolved
     * before validation. If the reference typesafe is unresolved, it's a bug in
     * the caller of this method.
     * </ul>
     *
     * <p>
     * If you want to allow a certain setting to have a flexible type (or
     * otherwise want validation to be looser for some settings), you could
     * either remove the problematic setting from the reference typesafe provided
     * to this method, or you could intercept the validation exception and
     * screen out certain problems. Of course, this will only work if list other
     * callers of this method are careful to restrict validation to their own
     * paths, as they should be.
     *
     * <p>
     * If validation fails, the thrown exception contains a list of list problems
     * found. See {@link ConfigException.ValidationFailed#problems}. The
     * exception's <code>getMessage()</code> will have list the problems
     * concatenated into one huge string, as well.
     *
     * <p>
     * Again, <code>checkValid()</code> can't guess every domain-specific way a
     * setting can be invalid, so some problems may arise later when attempting
     * to use the typesafe. <code>checkValid()</code> is limited to reporting
     * generic, but common, problems such as missing settings and blatant type
     * incompatibilities.
     *
     * @param reference
     *            a reference configuration
     * @param restrictToPaths
     *            only validate values underneath these paths that your code
     *            module owns and understands
     * @throws ConfigException.ValidationFailed
     *             if there are any validation issues
     * @throws ConfigException.NotResolved
     *             if this typesafe is not resolved
     * @throws ConfigException.BugOrBroken
     *             if the reference typesafe is unresolved or caller otherwise
     *             misuses the API
     */
    void checkValid(Config reference, String... restrictToPaths);

    /**
     * Checks whether a value is present and non-null at the given path. This
     * differs in two ways from {@code Map.containsKey()} as implemented by
     * {@link ConfigObject}: it looks for a path expression, not a key; and it
     * returns false for null values, while {@code containsKey()} returns true
     * indicating that the object contains a null value for the key.
     *
     * <p>
     * If a path exists according to {@link #hasPath(String)}, then
     * {@link #getValue(String)} will never throw an exception. However, the
     * typed getters, such as {@link #getInt(String)}, will still throw if the
     * value is not convertible to the requested type.
     *
     * @param path
     *            the path expression
     * @return true if a non-null value is present at the path
     * @throws ConfigException.BadPath
     *             if the path expression is invalid
     */
    boolean hasPath(String path);

    /**
     * Returns true if the {@code Config}'s root object contains no key-value
     * pairs.
     *
     * @return true if the configuration is empty
     */
    boolean isEmpty();

    /**
     * Returns the set of path-value pairs, excluding any null values, found by
     * recursing {@link #root() the root object}. Note that this is very
     * different from <code>root().entrySet()</code> which returns the set of
     * immediate-child keys in the root object and includes null values.
     *
     * @return set of paths with non-null values, built up by recursing the
     *         entire tree of {@link ConfigObject}
     */
    Set<Map.Entry<String, ConfigValue>> entrySet();

    /**
     *
     * @param path
     *            path expression
     * @return the boolean value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to boolean
     */
    boolean getBoolean(String path);

    /**
     * @param path
     *            path expression
     * @return the numeric value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a number
     */
    Number getNumber(String path);

    /**
     * @param path
     *            path expression
     * @return the 32-bit integer value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to an int (for example it is out
     *             of range, or it's a boolean value)
     */
    int getInt(String path);

    /**
     * @param path
     *            path expression
     * @return the 64-bit long value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a long
     */
    long getLong(String path);

    /**
     * @param path
     *            path expression
     * @return the floating-point value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a double
     */
    double getDouble(String path);

    /**
     * @param path
     *            path expression
     * @return the string value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a string
     */
    String getString(String path);

    /**
     * @param path
     *            path expression
     * @return the {@link ConfigObject} value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to an object
     */
    ConfigObject getObject(String path);

    /**
     * @param path
     *            path expression
     * @return the nested {@code Config} value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a Config
     */
    Config getConfig(String path);

    /**
     * Gets the value at the path as an unwrapped Java boxed value (
     * {@link java.lang.Boolean Boolean}, {@link java.lang.Integer Integer}, and
     * so on - see {@link ConfigValue#unwrapped()}).
     *
     * @param path
     *            path expression
     * @return the unwrapped value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     */
    Object getAnyRef(String path);

    /**
     * Gets the value at the given path, unless the value is a
     * null value or missing, in which case it throws just like
     * the other getters. Use {@code get()} on the {@link
     * Config#root()} object (or other object in the tree) if you
     * want an unprocessed value.
     *
     * @param path
     *            path expression
     * @return the value at the requested path
     * @throws ConfigException.Missing
     *             if value is absent or null
     */
    ConfigValue getValue(String path);

    /**
     * Gets a value as a size in bytes (parses special strings like "128M"). If
     * the value is already a number, then it's left alone; if it's a string,
     * it's parsed understanding unit suffixes such as "128K", as documented in
     * the <a
     * href="https://github.com/typesafehub/typesafe/blob/master/HOCON.md">the
     * spec</a>.
     *
     * @param path
     *            path expression
     * @return the value at the requested path, in bytes
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to Long or String
     * @throws ConfigException.BadValue
     *             if value cannot be parsed as a size in bytes
     */
    Long getBytes(String path);

    /**
     * Get value as a duration in milliseconds. If the value is already a
     * number, then it's left alone; if it's a string, it's parsed understanding
     * units suffixes like "10m" or "5ns" as documented in the <a
     * href="https://github.com/typesafehub/typesafe/blob/master/HOCON.md">the
     * spec</a>.
     *
     * @deprecated  As of release 1.1, replaced by {@link #getDuration(String, TimeUnit)}
     *
     * @param path
     *            path expression
     * @return the duration value at the requested path, in milliseconds
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to Long or String
     * @throws ConfigException.BadValue
     *             if value cannot be parsed as a number of milliseconds
     */
    @Deprecated Long getMilliseconds(String path);

    /**
     * Get value as a duration in nanoseconds. If the value is already a number
     * it's taken as milliseconds and converted to nanoseconds. If it's a
     * string, it's parsed understanding unit suffixes, as for
     * {@link #getDuration(String, TimeUnit)}.
     *
     * @deprecated  As of release 1.1, replaced by {@link #getDuration(String, TimeUnit)}
     *
     * @param path
     *            path expression
     * @return the duration value at the requested path, in nanoseconds
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to Long or String
     * @throws ConfigException.BadValue
     *             if value cannot be parsed as a number of nanoseconds
     */
    @Deprecated Long getNanoseconds(String path);

    /**
     * Gets a value as a duration in a specified
     * {@link java.util.concurrent.TimeUnit TimeUnit}. If the value is already a
     * number, then it's taken as milliseconds and then converted to the
     * requested TimeUnit; if it's a string, it's parsed understanding units
     * suffixes like "10m" or "5ns" as documented in the <a
     * href="https://github.com/typesafehub/typesafe/blob/master/HOCON.md">the
     * spec</a>.
     *
     * @since 1.1
     *
     * @param path
     *            path expression
     * @param unit
     *            convert the return value to this time unit
     * @return the duration value at the requested path, in the given TimeUnit
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to Long or String
     * @throws ConfigException.BadValue
     *             if value cannot be parsed as a number of the given TimeUnit
     */
    Long getDuration(String path, TimeUnit unit);

    /**
     * Gets a list value (with any element type) as a {@link ConfigList}, which
     * implements {@code java.util.List<ConfigValue>}. Throws if the path is
     * unset or null.
     *
     * @param path
     *            the path to the list value.
     * @return the {@link ConfigList} at the path
     * @throws ConfigException.Missing
     *             if value is absent or null
     * @throws ConfigException.WrongType
     *             if value is not convertible to a ConfigList
     */
    ConfigList getList(String path);

    List<Boolean> getBooleanList(String path);

    List<Number> getNumberList(String path);

    List<Integer> getIntList(String path);

    List<Long> getLongList(String path);

    List<Double> getDoubleList(String path);

    List<String> getStringList(String path);

    List<? extends ConfigObject> getObjectList(String path);

    List<? extends Config> getConfigList(String path);

    List<? extends Object> getAnyRefList(String path);

    List<Long> getBytesList(String path);

    /**
     * @deprecated  As of release 1.1, replaced by {@link #getDurationList(String, TimeUnit)}
     */
    @Deprecated List<Long> getMillisecondsList(String path);

    /**
     * @deprecated  As of release 1.1, replaced by {@link #getDurationList(String, TimeUnit)}
     */
    @Deprecated List<Long> getNanosecondsList(String path);

    /**
     * Gets a list, converting each value in the list to a duration, using the
     * same rules as {@link #getDuration(String, TimeUnit)}.
     *
     * @since 1.1
     * @param path
     *            a path expression
     * @param unit
     *            time units of the returned values
     * @return list of durations, in the requested units
     */
    List<Long> getDurationList(String path, TimeUnit unit);

    /**
     * Clone the typesafe with only the given path (and its children) retained;
     * list sibling paths are removed.
     *
     * @param path
     *            path to keep
     * @return a copy of the typesafe minus list paths except the one specified
     */
    Config withOnlyPath(String path);

    /**
     * Clone the typesafe with the given path removed.
     *
     * @param path
     *            path to remove
     * @return a copy of the typesafe minus the specified path
     */
    Config withoutPath(String path);

    /**
     * Places the typesafe inside another {@code Config} at the given path.
     *
     * @param path
     *            path to store this typesafe at.
     * @return a {@code Config} instance containing this typesafe at the given
     *         path.
     */
    Config atPath(String path);

    /**
     * Places the typesafe inside a {@code Config} at the given key. See also
     * atPath().
     *
     * @param key
     *            key to store this typesafe at.
     * @return a {@code Config} instance containing this typesafe at the given
     *         key.
     */
    Config atKey(String key);

    /**
     * Returns a {@code Config} based on this one, but with the given path set
     * to the given value. Does not modify this instance (since it's immutable).
     * If the path already has a value, that value is replaced. To remove a
     * value, use withoutPath().
     *
     * @param path
     *            path to add
     * @param value
     *            value at the new path
     * @return the new instance with the new map entry
     */
    Config withValue(String path, ConfigValue value);
}
