#!/bin/sh
APP_HOME=$(cd "${0%/*}" 2>/dev/null; pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
