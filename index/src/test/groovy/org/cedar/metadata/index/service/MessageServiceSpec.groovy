package org.cedar.metadata.index.service

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class MessageServiceSpec extends Specification {

  Service mockService = Mock(Service)
  MessageService messageService = new MessageService(mockService)

  def 'handleMessage gracefully handles a message with #inserts inserts and #deleted deletes'() {
    when:
    messageService.handleMessage(buildPayload(inserts, deletes))

    then:
    inserts * mockService.upsert(_ as Map) >> [meta: [created: true]]
    deletes * mockService.delete(_ as Map) >> [meta: [deleted: true]]

    and:
    noExceptionThrown()

    where:
    inserts | deletes
    0       | 0
    1       | 0
    0       | 1
    1       | 1
  }

  def 'handleMessage gracefully handles upsert/delete failures'() {
    when:
    messageService.handleMessage(buildPayload(1, 1))

    then: 'upsert and delete fail'
    1 * mockService.upsert(_ as Map) >> failure
    1 * mockService.delete(_ as Map) >> failure

    and: 'we never throw an exception and get into an infinite rabbit loop'
    noExceptionThrown()

    where: 'different ways of failing'
    failure << [
        null,
        { throw new IOException() }
    ]
  }

  def 'processResources handles a payload with #inserts inserts and #deleted deletes'() {
    when:
    def result = messageService.processResources(buildPayload(inserts, deletes))

    then:
    inserts * mockService.upsert(_ as Map) >> [meta: [created: true]]
    deletes * mockService.delete(_ as Map) >> [meta: [deleted: true]]

    and:
    noExceptionThrown()
    result == [meta: [created: inserts, updated: 0, deleted: deletes, failed: 0]]

    where:
    inserts | deletes
    0       | 0
    1       | 0
    0       | 1
    1       | 1
    2       | 0
    0       | 2
    2       | 2
  }

  def 'processResources detects updates vs inserts'() {
    when:
    def result = messageService.processResources(buildPayload(2, 0))

    then: 'one insert inserts and one updates'
    1 * mockService.upsert(_ as Map) >> [meta: [created: true]]
    1 * mockService.upsert(_ as Map) >> [meta: [created: false]]

    and:
    noExceptionThrown()
    result == [meta: [created: 1, updated: 1, deleted: 0, failed: 0]]
  }

  def 'processResources handles upsert/delete failures'() {
    when:
    def result = messageService.processResources(buildPayload(2, 2))

    then: 'upsert and delete both called twice and each fail once'
    1 * mockService.upsert(_ as Map) >> [meta: [created: true]]
    1 * mockService.upsert(_ as Map) >> failure
    1 * mockService.delete(_ as Map) >> [meta: [deleted: true]]
    1 * mockService.delete(_ as Map) >> failure

    and: 'successes and failures are recorded'
    noExceptionThrown()
    result == [meta: [created: 1, updated: 0, deleted: 1, failed: 2]]

    where: 'different ways of failing'
    failure << [
        null,
        [whatIsThis: 'a incorrectly structured map!'],
        { throw new RuntimeException() }
    ]
  }

  private static buildPayload(int inserts, int deletes) {
    def data = []
    inserts.times { i ->
      data << [id: "$i", type: 'granule', attributes: [name: "$i"], meta: [action: 'insert']]
    }
    deletes.times { i ->
      data << [id: "$i", type: 'granule', attributes: [name: "$i"], meta: [action: 'delete']]
    }
    return [data: data]
  }
}
