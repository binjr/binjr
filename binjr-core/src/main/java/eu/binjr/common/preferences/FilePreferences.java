/*
 * Licence:
 * CC0 1.0 Universal (CC0 1.0)
 * Public Domain Dedication
 *
 * The person who associated a work with this deed has dedicated
 * the work to the public domain by waiving all of his or her rights
 * to the work worldwide under copyright law, including all related
 * and neighboring rights, to the extent allowed by law.
 *
 */

package eu.binjr.common.preferences;

import eu.binjr.common.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Preferences implementation that stores to a user-defined file.
 *
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 */
public class FilePreferences extends AbstractPreferences {
    private static final Logger logger = Logger.create(FilePreferences.class);
    private final File backingFile;
    private final Map<String, String> root;
    private final Map<String, FilePreferences> children;
    private boolean isRemoved = false;

    public FilePreferences(File backingFile, AbstractPreferences parent, String name) {
        super(parent, name);
        this.backingFile = backingFile;

        logger.trace("Instantiating node " + name);

        root = new TreeMap<>();
        children = new TreeMap<>();

        try {
            sync();
        } catch (BackingStoreException e) {
            logger.error("Unable to sync on creation of node " + name, e);
        }
    }

    protected void putSpi(String key, String value) {
        root.put(key, value);
        try {
            flush();
        } catch (BackingStoreException e) {
            logger.error("Unable to flush after putting " + key, e);
        }
    }

    protected String getSpi(String key) {
        return root.get(key);
    }

    protected void removeSpi(String key) {
        root.remove(key);
        try {
            flush();
        } catch (BackingStoreException e) {
            logger.error("Unable to flush after removing " + key, e);
        }
    }

    protected void removeNodeSpi() throws BackingStoreException {
        isRemoved = true;
        flush();
    }

    protected String[] keysSpi() throws BackingStoreException {
        return root.keySet().toArray(new String[root.keySet().size()]);
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[children.keySet().size()]);
    }

    protected FilePreferences childSpi(String name) {
        FilePreferences child = children.get(name);
        if (child == null || child.isRemoved()) {
            child = new FilePreferences(this.backingFile, this, name);
            children.put(name, child);
        }
        return child;
    }


    protected void syncSpi() throws BackingStoreException {
        if (isRemoved()){
            return;
        }

        if (!backingFile.exists()) {
            return;
        }
        synchronized (backingFile) {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(backingFile));

                StringBuilder sb = new StringBuilder();
                getPath(sb);
                String path = sb.toString();

                final Enumeration<?> pnen = p.propertyNames();
                while (pnen.hasMoreElements()) {
                    String propKey = (String) pnen.nextElement();
                    if (propKey.startsWith(path)) {
                        String subKey = propKey.substring(path.length());
                        // Only load immediate descendants
                        if (subKey.indexOf('.') == -1) {
                            root.put(subKey, p.getProperty(propKey));
                        }
                    }
                }
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    private void getPath(StringBuilder sb) {
        final FilePreferences parent = (FilePreferences) parent();
        if (parent == null) return;

        parent.getPath(sb);
        sb.append(name()).append('.');
    }

    protected void flushSpi() throws BackingStoreException {
        synchronized (backingFile) {
            Properties p = new Properties();
            try {

                StringBuilder sb = new StringBuilder();
                getPath(sb);
                String path = sb.toString();

                if (backingFile.exists()) {
                    p.load(new FileInputStream(backingFile));

                    List<String> toRemove = new ArrayList<String>();

                    // Make a list of all direct children of this node to be removed
                    final Enumeration<?> pnen = p.propertyNames();
                    while (pnen.hasMoreElements()) {
                        String propKey = (String) pnen.nextElement();
                        if (propKey.startsWith(path)) {
                            String subKey = propKey.substring(path.length());
                            // Only do immediate descendants
                            if (subKey.indexOf('.') == -1) {
                                toRemove.add(propKey);
                            }
                        }
                    }

                    // Remove them now that the enumeration is done with
                    for (String propKey : toRemove) {
                        p.remove(propKey);
                    }
                }

                // If this node hasn't been removed, add back in any values
                if (!isRemoved) {
                    for (String s : root.keySet()) {
                        p.setProperty(path + s, root.get(s));
                    }
                }

                p.store(new FileOutputStream(backingFile), "FilePreferences");
            } catch (IOException e) {
                throw new BackingStoreException(e);
            }
        }
    }
}
