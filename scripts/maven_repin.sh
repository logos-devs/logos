#!/bin/sh -e

REPIN=1 bazel run @unpinned_maven//:pin
