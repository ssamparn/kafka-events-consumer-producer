# To start local kafka services:
confluent local services start

# To stop local kafka services:
confluent local services stop

# To delete kafka topic and data produced:
confluent local destroy

# Create a Kafka Topic
kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic <topic-name>
kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic users


# Create a Kafka Producer
kafka-console-producer --topic <kafka-topic-name> --broker-list localhost:9092
kafka-console-producer --topic users --broker-list localhost:9092

# Create a Kafka Consumer
kafka-console-consumer --topic <kafka-topic-name> --bootstrap-server localhost:9092 --from-beginning
kafka-console-consumer --topic users --bootstrap-server localhost:9092 --from-beginning
kafka-console-consumer --topic users --bootstrap-server localhost:9092 --from-beginning --property print.key=true --property key.separator=":"
kafka-console-consumer --topic item-topic --bootstrap-server localhost:9092 --from-beginning

# To list all the topics configured
kafka-topics --zookeeper localhost:2181 --list

