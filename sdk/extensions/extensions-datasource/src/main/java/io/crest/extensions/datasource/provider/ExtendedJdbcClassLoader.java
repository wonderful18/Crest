package io.crest.extensions.datasource.provider;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * URL class loader that prefers extension JDBC driver classes before parent classes.
 */
public class ExtendedJdbcClassLoader extends URLClassLoader {

    /**
     * Fully qualified JDBC driver class name associated with this loader.
     */
    private String driver;
    /**
     * Returns the configured JDBC driver class name.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the JDBC driver class name for later datasource registration.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }


    /**
     * Creates a class loader for the supplied driver URLs.
     */
    public ExtendedJdbcClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Loads classes child-first so bundled JDBC driver dependencies can override parent classes.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);

            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            try {
                c = findClass(name);
                if (c != null) {
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }


            try {
                if (getParent() != null) {
                    c = super.loadClass(name, resolve);
                    if (c != null) {
                        if (resolve) {
                            resolveClass(c);
                        }
                        return c;
                    }
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
            try {
                c = findSystemClass(name);
                if (c != null) {
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
            throw new ClassNotFoundException(name);
        }
    }



    /**
     * Adds a file path to the class loader search URLs.
     */
    public void addFile(String s) throws IOException {
        File f = new File(s);
        addFile(f);
    }

    /**
     * Adds a local file to the class loader search URLs.
     */
    public void addFile(File f) throws IOException {
        addFile(f.toURI().toURL());
    }

    /**
     * Adds a URL to the class loader search path.
     */
    public void addFile(URL u) throws IOException {
        try {
            this.addURL(u);
        } catch (Throwable t) {
            io.crest.utils.LogUtil.error(t.getMessage(), t);
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
}
