root="root"
metrics="matrics $root"
inject="inject $root $cassandra"
spark="spark $root"
streaming="streaming $root $spark"
cassandra="cassandra $root"

eval echo "\$$@" \
  | sed s/["^ "]*/\&-secrets" "\&/g \
  | xargs -n1 | sort -u | xargs \
  | sed s/["^ "]*/docker-compose-\&.yml/g
#echo "$spark"