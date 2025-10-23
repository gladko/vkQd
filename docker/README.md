## build
`docker build -t mux -f docker/mux/Dockerfile .`

## run
```
docker network create vk-network

docker run --name mux-root --network vk-network --rm -it -p 7000:7000 -p 5000:5000 mux
docker run --name mux1 --network vk-network --rm -it -e UP_STREAM_ADDRESS="(mux-root:5000)" -p 7001:7000 -p 5001:5000 mux
docker run --name mux2 --network vk-network --rm -it -e UP_STREAM_ADDRESS="(mux-root:5000)" -p 7002:7000 -p 5002:5000 mux

./qds connect localhost:5001 Quote IBM
./qds post localhost:7000
 -> Quote IBM 20:00:11 2 5 6 22:00:11 7
```

