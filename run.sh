#!/bin/bash

# Stop all container
# docker rm $(docker ps -a -q) -f

# Start platform
docker-compose up

# Injection de messages dans kafka
docker exec -it $(docker-compose ps -q kafka) kafka-console-producer.sh --broker-list localhost:9092 --topic events
{"action":"create","timestamp":"2017-10-05T23:01:17Z"}
{"action":"update","timestamp":"2017-10-05T23:01:19Z"}
{"action":"update","timestamp":"2017-10-05T23:02:51Z"}
{"action":"update","timestamp":"2017-10-05T23:02:51Z","newid":"newvalue"}
{"action":"update","timestamp":"2017-10-05T
{"action":"update","timestamp":"2017-11-05T23:01:17Z"}éé

# Lecture des messages dans kafka
docker exec -it $(docker-compose ps -q kafka) kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic error --from-beginning

# List topics in Kafka
docker exec -it $(docker-compose ps -q kafka) kafka-topics.sh --zookeeper zookeeper:2181 --list

# List topic configuration in Kafka
 docker exec -it $(docker-compose ps -q kafka) kafka-topics.sh --zookeeper zookeeper:2181  --describe

# Check Vertica Database
docker exec -it $(docker-compose ps -q vertica) /opt/vertica/bin/vsql -U dbadmin -c "CREATE SCHEMA s1; CREATE TABLE s1.test (action VARCHAR(32), timestamp VARCHAR(32));"
docker exec -it $(docker-compose ps -q vertica) /opt/vertica/bin/vsql -U dbadmin -c "SELECT * FROM s1.test;"


# Build & Deploy scala app
sbt assembly && docker cp build/ $(docker ps -a | grep spark-master | awk '{print $1}'):/ && docker exec -it $(docker ps -a | grep spark-master | awk '{print $1}') /spark/bin/spark-submit --class com.courouge.KafkaSparkDemo --master spark://$(docker ps -a | grep spark-master | awk '{print $1}'):7077
