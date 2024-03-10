module com.udacity.parent.image {
    exports com.udacity.parent.image.service to com.udacity.parent.security, com.udacity.parent.application;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;
    requires org.slf4j;
}