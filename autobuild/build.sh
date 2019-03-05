#!/bin/sh

# Go to project root
cd `dirname $0`/..

mkdir -p build
javac -d ./build `find ./src -name "*.java"`
mkdir -p dist
pushd ./build
jar -cf ../dist/Brainclouds2s.jar `find . -name "*.class"`

