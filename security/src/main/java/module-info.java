module com.udacity.parent.security {
    requires com.udacity.parent.image;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires miglayout.swing;
    requires java.prefs;

    opens com.udacity.parent.security.util;
}