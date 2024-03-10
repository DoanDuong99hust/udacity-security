module com.udacity.parent.application {
    requires com.udacity.parent.security;
    requires com.udacity.parent.image;
    requires miglayout.swing;
    requires java.desktop;

    exports com.udacity.parent.application to com.udacity.parent.security;
}