/**
 * Copyright (c) OSGi Alliance (2004, 2007). All Rights Reserved.
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.version;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Version implements Comparable<Version> {
    public static final String SNAPSHOT = "-SNAPSHOT";
    private final int major;
    private final int minor;
    private final int micro;
    private final String qualifier;
    private final boolean isSnapshot;
    private static final String SEPARATOR = ".";
    /**
     * The empty version "0.0.0". Equivalent to calling
     * <code>new Version(0,0,0)</code>.
     */
    public static final Version emptyVersion = new Version(0, 0, 0);

    /**
     * Creates a version identifier from the specified numerical components.
     * <br>
     * <br>
     * The qualifier is set to the empty string.
     *
     * @param major Major component of the version identifier.
     * @param minor Minor component of the version identifier.
     * @param micro Micro component of the version identifier.
     * @throws IllegalArgumentException If the numerical components are
     *                                  negative.
     */
    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    public Version(int major, int minor, int micro, String qualifier) {
        this(major, minor, micro, qualifier, false);
    }

    /**
     * Creates a version identifier from the specifed components.
     *
     * @param major     Major component of the version identifier.
     * @param minor     Minor component of the version identifier.
     * @param micro     Micro component of the version identifier.
     * @param qualifier Qualifier component of the version identifier. If
     *                  <code>null</code> is specified, then the qualifier will be set
     *                  to the empty string.
     * @param snapshot  true if the version is a snapshot.
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    public Version(int major, int minor, int micro, String qualifier, boolean snapshot) {
        if (qualifier == null) {
            qualifier = ""; //$NON-NLS-1$
        }
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        this.isSnapshot = snapshot;
        validate();
    }

    /**
     * Created a version identifier from the specified string.
     * <br>
     * <br>
     * Here is the grammar for version strings.
     * <br>
     * <pre>
     * version ::= major('.'minor('.'micro('.'qualifier)?)?)?
     * major ::= digit+
     * minor ::= digit+
     * micro ::= digit+
     * qualifier ::= (alpha|digit|'_'|'-')+
     * digit ::= [0..9]
     * alpha ::= [a..zA..Z]
     * </pre>
     * <br>
     * There must be no whitespace in version.
     *
     * @param version String representation of the version identifier.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public Version(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Provided version string is null");
        }
        int major = 0;
        int minor = 0;
        int micro = 0;
        String qualifier = ""; //$NON-NLS-1$
        if (version.endsWith(SNAPSHOT)) {
            this.isSnapshot = true;
            version = version.substring(0, version.length() - 9);
        } else {
            isSnapshot = false;
        }

        try {
            StringTokenizer st = new StringTokenizer(version, SEPARATOR+"-", true);
            major = Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) {
                st.nextToken(); // consume delimiter
                minor = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) {
                    st.nextToken(); // consume delimiter
                    micro = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        st.nextToken(); // consume delimiter
                        qualifier = st.nextToken();
                        if (st.hasMoreTokens()) {
                            throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
        }
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        validate();
    }

    /**
     * Called by the Version constructors to validate the version components.
     *
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    private void validate() {
        if (major < 0) {
            throw new IllegalArgumentException("negative major"); //$NON-NLS-1$
        }
        if (minor < 0) {
            throw new IllegalArgumentException("negative minor"); //$NON-NLS-1$
        }
        if (micro < 0) {
            throw new IllegalArgumentException("negative micro"); //$NON-NLS-1$
        }
        int length = qualifier.length();
        for (int i = 0; i < length; i++) {
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".indexOf(qualifier.charAt(i)) == -1) { //$NON-NLS-1$
                throw new IllegalArgumentException("invalid qualifier"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Parses a version identifier from the specified string.
     * <br>
     * <br>
     * See <code>Version(String)</code> for the format of the version string.
     *
     * @param version String representation of the version identifier. Leading
     *                and trailing whitespace will be ignored.
     * @return A <code>Version</code> object representing the version
     * identifier. If <code>version</code> is <code>null</code> or
     * the empty string then <code>emptyVersion</code> will be
     * returned.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public static Version parseVersion(String version) {
        if (version == null) {
            return emptyVersion;
        }
        version = version.trim();
        if (version.length() == 0) {
            return emptyVersion;
        }
        return new Version(version);
    }

    /**
     * Returns the major component of this version identifier.
     *
     * @return The major component.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor component of this version identifier.
     *
     * @return The minor component.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the micro component of this version identifier.
     *
     * @return The micro component.
     */
    public int getMicro() {
        return micro;
    }

    /**
     * Returns the qualifier component of this version identifier.
     *
     * @return The qualifier component.
     */
    public String getQualifier() {
        return qualifier;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    /**
     * Returns the string representation of this version identifier.
     * <br>
     * <br>
     * The format of the version string will be <code>major.minor.micro</code>
     * if qualifier is the empty string or
     * <code>major.minor.micro-qualifier</code> otherwise.
     *
     * @return The string representation of this version identifier.
     */
    @Override
    public String toString() {
        String base = major + SEPARATOR + minor + SEPARATOR + micro;
        if (qualifier.length() == 0) { //$NON-NLS-1$
            return isSnapshot ? base + SNAPSHOT : base;
        } else {
            return isSnapshot ? base + "-" + qualifier + SNAPSHOT : base + "-" + qualifier;
        }
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return An integer which is a hash code value for this object.
     */
    public int hashCode() {
        return ((major << 24) + (minor << 16) + (micro << 8) + qualifier.hashCode()) * (isSnapshot ? -1 : 1);
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <br>
     * <br>
     * A version is considered to be <b>equal to </b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.equals</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return <code>true</code> if <code>object</code> is a
     * <code>Version</code> and is equal to this object;
     * <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        if (object == this) { // quicktest
            return true;
        }
        if (!(object instanceof Version other)) {
            return false;
        }
        return (major == other.major) && (minor == other.minor)
                && (micro == other.micro) && qualifier.equals(other.qualifier);
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <br>
     * <br>
     * A version is considered to be <b>less than </b> another version if its
     * major component is less than the other version's major component, or the
     * major components are equal and its minor component is less than the other
     * version's minor component, or the major and minor components are equal
     * and its micro component is less than the other version's micro component,
     * or the major, minor and micro components are equal and it's qualifier
     * component is less than the other version's qualifier component (using
     * <code>String.compareTo</code>).
     * <br>
     * <br>
     * A version is considered to be <b>equal to</b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.compareTo</code>).
     *
     * @param other The <code>Version</code> object to be compared.
     * @return A negative integer, zero, or a positive integer if this object is
     * less than, equal to, or greater than the specified
     * <code>Version</code> object.
     * @throws ClassCastException If the specified object is not a
     *                            <code>Version</code>.
     */
    public int compareTo(Version other) {
        if (other == this) { // quicktest
            return 0;
        }
        int result = major - other.major;
        if (result != 0) {
            return result;
        }
        result = minor - other.minor;
        if (result != 0) {
            return result;
        }
        result = micro - other.micro;
        if (result != 0) {
            return result;
        }
        result = qualifier.compareTo(other.qualifier);
        if (result != 0) {
            return result;
        }
        return (this.isSnapshot ? 0 : 1) - (other.isSnapshot ? 0 : 1);
    }


}