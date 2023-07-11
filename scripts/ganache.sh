#!/bin/sh -e
mkdir -p ~/.cache/ganache/db/
exec npx -- ganache -d -h 0.0.0.0 --database.dbPath ~/.cache/ganache/db/ -i 1337
