# https://nats.io/blog/docker-swarm-plus-nats/
# https://dzone.com/articles/building-a-microservices-control-plane-using-nats

docker service create --name ruby-nats --network smartmeter wallyqs/ruby-nats:ruby-2.3.1-nats-v0.8.0 -e '
 NATS.on_error do |e|
   puts "ERROR: #{e}"
 end
 NATS.start(:servers => ["nats://nats:4222"]) do |nc|
   inbox = NATS.create_inbox
   puts "[#{Time.now}] Connected to NATS at #{nc.connected_server}, inbox: #{inbox}"

   nc.subscribe(inbox) do |msg, reply|
     puts "[#{Time.now}] Received reply - #{msg}"
   end

   nc.subscribe("hello") do |msg, reply|
     next if reply == inbox
     puts "[#{Time.now}] Received greeting - #{msg} - #{reply}"
     nc.publish(reply, "world")
   end

   EM.add_periodic_timer(1) do
     puts "[#{Time.now}] Saying hi (servers in pool: #{nc.server_pool}"
     nc.publish("hello", "hi", inbox)
   end
 end
'

docker service scale ruby-nats=3
