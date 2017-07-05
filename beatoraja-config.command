#!/bin/bash
cd "$(dirname "$0")"
exec java -Xms1g -Xmx4g -jar beatoraja.jar -c
