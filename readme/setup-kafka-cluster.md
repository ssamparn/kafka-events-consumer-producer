# Local Kafka Cluster setup with 3 brokers using Docker Compose

## Set up broker and zookeeper

- Navigate to the path where the **kafka-cluster-setup.yml** is located.

- Run the below command and this will spin up a kafka cluster with 3 brokers.

```bash
$ docker-compose -f kafka-cluster-setup.yml up
```

## Produce and Consume the Messages

- Log in to the container by running the below command.

```bash
$ docker ps
$ docker exec -it <container-name> bash
$ docker exec -it kafka-1 bash
```

- Create topic with the replication factor as 3

```bash
$ kafka-topics --bootstrap-server kafka-1:19092 \
  --create \
  --topic test-topic \
  --replication-factor 1 --partitions 1
```

- Produce Messages to the topic.

```bash
$ kafka-console-producer --bootstrap-server kafka-1:19092 \
  --topic test-topic
```

- Consume Messages from the topic.

```bash
$ kafka-console-consumer --bootstrap-server kafka-1:9092 \
  --topic test-topic \
  --from-beginning
```

- Consume Messages from dead letter topic.

```bash
$ kafka-console-consumer --bootstrap-server kafka-1:29092 \
--topic item-event-topic.dlt \
--from-beginning
```

- Consume Messages from retry topic.

```bash
$ kafka-console-consumer --bootstrap-server kafka-1:29092 \
--topic item-event-topic.retry \
--from-beginning
```

#### Log files in Multi Kafka Cluster

- Log files will be created for each partition in each of the broker instance of the Kafka cluster.

- Login to the container **kafka-1**.
  
```bash
$ docker exec -it kafka-1 bash
```

-  Login to the container **kafka-2**.
  
```bash
$ docker exec -it kafka-2 bash
```

- Shutdown the kafka cluster

```bash
$ docker-compose -f kafka-cluster-setup.yml down
```

### Setting up min.insync.replica

- Topic - test-topic

```bash
$ kafka-configs --bootstrap-server kafka-1:19092 --entity-type topics --entity-name test-topic \
--alter --add-config min.insync.replicas=2
```

- Topic - library-events

```bash
$ kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name library-events \
--alter --add-config min.insync.replicas=2
```

## Advanced Kafka Commands

### List the topics in a cluster

```bash
$ kafka-topics --bootstrap-server kafka-1:29092 --list
```

### Describe topic

- Command to describe all the Kafka topics.

```bash
$ kafka-topics --bootstrap-server kafka-1:29092 --describe
```

- Command to describe a specific Kafka topic.

```bash
$ kafka-topics --bootstrap-server kafka-1:19092 --describe \
--topic test-topic
```

### Alter topic Partitions

```bash
$ kafka-topics --bootstrap-server kafka-1:19092 \
--alter --topic test-topic --partitions 40
```

### How to view consumer groups

```bash
$ kafka-consumer-groups --bootstrap-server kafka-1:19092 --list
```

#### Consumer Groups and their Offset

```bash
$ kafka-consumer-groups --bootstrap-server kafka-1:19092 \
--describe --group <console-consumer-name>
```

## Log file and related config

- Log into the container.

```bash
$ docker exec -it kafka-1 bash
```

- The config file is present in the below path.

```
/etc/kafka/server.properties
```

- The log file is present in the below path.

```
/var/lib/kafka/data/
```

### How to view the commit log?

```bash
$ kafka-run-class kafka.tools.DumpLogSegments \
--deep-iteration \
--files /var/lib/kafka/data/test-topic-0/00000000000000000000.log
```
