module com.udacity.parent.security {
    requires com.udacity.parent.image;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    requires java.desktop;
    requires miglayout.swing;

    opens com.udacity.parent.security.util;
    exports com.udacity.parent.security.util to com.udacity.parent.application;
    exports com.udacity.parent.security.constant to com.udacity.parent.application;
}