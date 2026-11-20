#!/bin/sh
set -eu

umask 077

exec java -jar /app/auth-users.jar
