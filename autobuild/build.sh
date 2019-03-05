#!/bin/sh

# Go to project root
cd `dirname $0`/..

if [ ! -f version.txt ]; then
    echo "Missing version.txt file, must contain onle line with the current version of the source."
    exit 1
fi

VERSION=`cat version.txt`

#Get Version from arguments if present
if [ -n "$1" ]; then
    VERSION=$1
fi

echo "Building Brainclouds2s... "
mkdir -p build
javac -d ./build `find ./src -name "*.java"`
cp src/LICENSE build/

echo "Packacking Brainclouds2s-$VERSION.jar..."
mkdir -p dist
cd ./build
jar -cf ../dist/Brainclouds2s-$VERSION.jar `find . -name "*.class"` LICENSE
echo "Done."
