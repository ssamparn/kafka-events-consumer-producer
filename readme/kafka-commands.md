# Setting Up Kafka 3.0.0

<details><summary>Mac</summary>
<p>

- Make sure you are navigated inside the bin directory.

## Start Zookeeper and Kafka Broker

-   Start up the Zookeeper.

```bash
$ ./zookeeper-server-start.sh ../config/zookeeper.properties
```

- Add the below properties in the server.properties

```
listeners=PLAINTEXT://localhost:9092
auto.create.topics.enable=false
```

-   Start up the Kafka Broker

```bash
$ ./kafka-server-start.sh ../config/server.properties
```

## How to create a topic ?

```bash
$ ./kafka-topics.sh --create --topic test-topic --replication-factor 1 --partitions 4 --bootstrap-server localhost:9092
```

## How to instantiate a Console Producer?

### Without Key

```bash
$ ./kafka-console-producer.sh --broker-list localhost:9092 --topic test-topic
```

### With Key

```bash
$ ./kafka-console-producer.sh --broker-list localhost:9092 --topic test-topic --property "key.separator=-" --property "parse.key=true"
```

## How to instantiate a Console Consumer?

### Without Key

```bash
$ ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

### With Key

```bash
$ ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning -property "key.separator= - " --property "print.key=true"
```

### With Consumer Group

```bash
$ ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --group <group-name>
```

### Consume messages With Kafka Headers

```bash
$ ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic library-events.DLT --from-beginning --property print.headers=true --property print.timestamp=true
```

</p>

</details>

<details><summary>Windows</summary>
<p>

- Make sure you are inside the **bin/windows** directory.

## Start Zookeeper and Kafka Broker

-   Start up the Zookeeper.

```bash
$ zookeeper-server-start.bat ..\..\config\zookeeper.properties
```

-   Start up the Kafka Broker.

```bash
$ kafka-server-start.bat ..\..\config\server.properties
```

## How to create a topic ?

```bash
$ kafka-topics.bat --create --topic test-topic  --replication-factor 1 --partitions 4 --bootstrap-server localhost:9092
```

## How to instantiate a Console Producer?

### Without Key

```bash
$ kafka-console-producer.bat --broker-list localhost:9092 --topic test-topic
```

### With Key

```bash
$ kafka-console-producer.bat --broker-list localhost:9092 --topic test-topic --property "key.separator=-" --property "parse.key=true"
```

## How to instantiate a Console Consumer?

### Without Key

```bash
$ kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

### With Key

```bash
$ kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test-topic --from-beginning -property "key.separator= - " --property "print.key=true"
```

### With Consumer Group

```bash
$ kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test-topic --group <group-name>
```

### Consume messages With Kafka Headers

```bash
$ kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic library-events.DLT --from-beginning --property print.headers=true --property print.timestamp=true
```

</p>

</details>

## Setting Up Multiple Kafka Brokers

- The first step is to add a new **server.properties**.

- We need to modify three properties to start up a multi broker set up.

```
broker.id=<unique-broker-d>
listeners=PLAINTEXT://localhost:<unique-port>
log.dirs=/tmp/<unique-kafka-folder>
auto.create.topics.enable=false
```

- Example config will be like below.

```
broker.id=1
listeners=PLAINTEXT://localhost:9093
log.dirs=/tmp/kafka-logs-1
auto.create.topics.enable=false
```

### Starting up the new Broker

- Provide the new **server.properties** thats added.

```bash
$ ./kafka-server-start.sh ../config/server-1.properties
```

```bash
$ ./kafka-server-start.sh ../config/server-2.properties
```

# Advanced Kafka CLI operations:

<details><summary>Mac</summary>
<p>

## List the topics in a cluster

```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 --list
```

## Describe topic

- The below command can be used to describe all the topics.

```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 --describe
```

- The below command can be used to describe a specific topic.

```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic <topic-name>
```

## Alter the min insync replica

```bash
$ ./kafka-configs.sh  --bootstrap-server localhost:9092 --entity-type topics --entity-name library-events --alter --add-config min.insync.replicas=2
```

## Alter the partitions of a topic

```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic test-topic --partitions 40
```

## Delete a topic

```bash
$ ./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic test-topic
```
## How to view consumer groups

```bash
$ ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

### Consumer Groups and their Offset

```bash
$ ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group console-consumer-27773
```

## Viewing the Commit Log

```bash
$ ./kafka-run-class.sh kafka.tools.DumpLogSegments --deep-iteration --files /tmp/kafka-logs/test-topic-0/00000000000000000000.log
```

## Setting the Minimum Insync Replica

```bash
$ ./kafka-configs.sh --alter --bootstrap-server localhost:9092 --entity-type topics --entity-name test-topic --add-config min.insync.replicas=2
```
</p>
</details>


<details><summary>Windows</summary>
<p>

- Make sure you are inside the **bin/windows** directory.

## List the topics in a cluster

```bash
$ kafka-topics.bat --bootstrap-server localhost:9092 --list
```

## Describe topic

- The below command can be used to describe all the topics.

```bash
$ kafka-topics.bat --bootstrap-server localhost:9092 --describe
```

- The below command can be used to describe a specific topic.

```bash
$ kafka-topics.bat --bootstrap-server localhost:9092 --describe --topic <topic-name>
```

## Alter the min insync replica

```bash
$ kafka-configs.bat --bootstrap-server localhost:9092 --entity-type topics --entity-name library-events --alter --add-config min.insync.replicas=2
```
## Alter the partitions of a topic

```bash
$ kafka-configs.bat --bootstrap-server localhost:9092 --alter --topic test-topic --partitions 40
```

## Delete a topic

```bash
$ kafka-topics.bat --bootstrap-server localhost:9092 --delete --topic <topic-name>
```


## How to view consumer groups

```bash
$ kafka-consumer-groups.bat --bootstrap-server localhost:9092 --list
```

### Consumer Groups and their Offset

```bash
$ kafka-consumer-groups.bat --bootstrap-server localhost:9092 --describe --group console-consumer-27773
```

## Viewing the Commit Log

```bash
$ kafka-run-class.bat kafka.tools.DumpLogSegments --deep-iteration --files /tmp/kafka-logs/test-topic-0/00000000000000000000.log
```
</p>
</details>
