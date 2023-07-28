# Local kafka set up using Docker

## Set up broker and zookeeper

- Navigate to the path where the **kafka-single-node-setup.yml** is located and then run the below command.

```bash
$ docker-compose -f kafka-single-node-setup.yml up
```

## Produce and Consume the Messages

- Log in to the container by running the below command.

```bash
$ docker ps
$ docker exec -it <container-name> bash
$ docker exec -it kafka bash
```

- Create a Kafka topic using the **kafka-topics** command.
  - **kafka:19092** refers to the **KAFKA_ADVERTISED_LISTENERS** in the kafka-single-node-setup.yml file.

```bash
$ kafka-topics --bootstrap-server kafka:19092 \
  --create \
  --topic test-topic \
  --replication-factor 1 --partitions 1
```

- Produce Messages to the topic.

```bash
$ kafka-console-producer --bootstrap-server kafka:19092 \
  --topic test-topic
```

- Consume Messages from the topic.

```bash
$ kafka-console-consumer --bootstrap-server kafka:19092 \
  --topic test-topic \
  --from-beginning
```

## Producer and Consume the Messages With Key and Value

- Produce Messages with Key and Value to the topic.

```bash
$ kafka-console-producer --bootstrap-server kafka:19092 \
  --topic test-topic \
  --property "key.separator=:" --property "parse.key=true"
```

- Consuming messages with Key and Value from a topic.

```bash
$ kafka-console-consumer --bootstrap-server kafka:19092 \
  --topic test-topic \
  --from-beginning \
  --property "key.separator= : " --property "print.key=true"
```

### Consume Messages using Consumer Groups

```bash
$ kafka-console-consumer --bootstrap-server kafka:19092 \
                       --topic test-topic --group console-consumer-41911\
                       --property "key.separator= : " --property "print.key=true"
```

- Example Messages:

```text
a:abc
b:bus
```

