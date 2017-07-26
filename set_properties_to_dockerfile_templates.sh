#!/bin/bash

set -a # turn on auto-export
. properties/configuration.properties
set -a # turn off auto-export

while IFS= read -r -d '' filename; do
echo "-------------- $filename --------------"
eval "cat <<EOF
# GENERATED FILE, please do not modify nor store into Git #
$(<$filename)
EOF
" > "${filename%.*}"
done < <(find ./dockerfile-*/* -name 'Dockerfile.tpl' -print0)
