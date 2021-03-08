#!/bin/sh

# Go to project root
cd `dirname $0`/..

# Create jar file
mvn package

# Feedback
echo "Done."
