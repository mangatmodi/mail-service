#!/bin/bash
JAR_NAME="${JAR_NAME:?JAR_NAME variable not defined}"
java ${JAVA_OPTS} -jar "$JAR_NAME" $@
