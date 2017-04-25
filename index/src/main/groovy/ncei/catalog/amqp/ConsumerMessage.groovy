package ncei.catalog.amqp

import ncei.catalog.model.Metadata

class ConsumerMessage {
  String task

  Metadata metadata

  String toString() {
    "task: $task, metadata: $metadata"
  }
}
