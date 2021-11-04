/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("ru.study21.jcsv.xxl.java-library-conventions")
}

dependencies {
    compileOnly(group="org.projectlombok", name="lombok", version="1.18.22")
    annotationProcessor(group="org.projectlombok", name="lombok", version="1.18.22")

    testCompileOnly(group="org.projectlombok", name="lombok", version="1.18.22")
    testAnnotationProcessor(group="org.projectlombok", name="lombok", version="1.18.22")

    implementation("com.typesafe:config:1.4.1")
}