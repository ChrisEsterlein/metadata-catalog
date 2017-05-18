package ncei.catalog.service

import spock.lang.Shared
import spock.lang.Specification

class MessageServiceSpec extends Specification {

  MessageService messageService = new MessageService()
  Service service = Mock(Service)

  @Shared
  Map insertMessage = [
      id        : '1',
      type      : "junk",
      attributes: [dataset: "testDataset", fileName: "testFileName1"]
  ]

  @Shared
  Map insertReturn = [
      data: [
          id        : insertMessage.id,
          type      : insertMessage.type,
          attributes: insertMessage.attributes,
          meta      : [created: true]
      ]
  ]

  def setup() {
    messageService.service = service
  }

  def 'Rabbit message where insert return is #insertReturnType is gracefully dealt with'() {

    when:
    messageService.handleMessage(insertMessage)

    then:
    1 * service.insert(insertMessage) >> mockedInsertReturn
    noExceptionThrown()

    where:
    insertReturnType | mockedInsertReturn
    'a valid result' | insertReturn
    'null'           | null
    'an exception'   | {throw new IOException()}
  }
}