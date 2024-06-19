#!/bin/sh

make -C busybox defconfig
sed -i '/# CONFIG_STATIC is not set/c\CONFIG_STATIC=y' busybox/.config