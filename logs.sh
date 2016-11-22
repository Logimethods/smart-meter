docker logs $(docker ps | grep "$1" | rev | cut -d' ' -f1 | rev)
