package ncei.catalog.filter.utils

import groovy.json.JsonOutput
import ncei.catalog.filters.utils.RequestConversionUtil
import org.codehaus.jettison.json.JSONObject
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class RequestConversionUtilSpec extends Specification {

  final Map metadataRecordItem = [
      dataset     : 'csb',
      trackingId  : 'eb62bc6b-ef72-49ec-9de7-08a3f7153e99',
      filename    : 'myfile.txt',
      fileSize    : 42,
      geometry    : 'POLYGON((0 0) (0 1) (1 1) (1 0))',
      fileMetadata: JsonOutput.toJson([type: 'FeatureCollection', platform: 'seadog island', ship: 'sea-witch']),
      extraIgnore : 'ignore me' // This is just to verify a field that doesn't exist in the DB won't get saved.
  ]

  def 'Happy path: legacy transform of POST body works'() {
    Map expPostBody = metadataRecordItem.clone()
    expPostBody.id = expPostBody.trackingId
    expPostBody.size_bytes = expPostBody.fileSize
    expPostBody.metadata = expPostBody.fileMetadata
    expPostBody.remove('trackingId')
    expPostBody.remove('fileSize')
    expPostBody.remove('fileMetadata')
    expPostBody = expPostBody.sort()

    expect:
    RequestConversionUtil.transformLegacyPostBody(metadataRecordItem) == (expPostBody.sort() as JSONObject) as String
  }

  def 'Corner cases: legacy transform of POST body=#postBody works'() {

    expect:
    RequestConversionUtil.transformLegacyPostBody(postBody) == expPostBody

    where:
    postBody | expPostBody
    null     | '{}'
    [:]      | '{}'
  }

  def 'Happy path: legacy transform of GET response body works'() {
    Map responseBody = [
        data: [
            [
                id        : metadataRecordItem.trackingId,
                type      : 'granule',
                attributes: [
                    id             : metadataRecordItem.trackingId,
                    last_update    : '1497540221194',
                    dataset        : metadataRecordItem.dataset,
                    metadata_schema: null,
                    filename       : metadataRecordItem.filename,
                    type           : null,
                    access_protocol: 'FILE',
                    file_path      : '/blah',
                    size_bytes     : metadataRecordItem.fileSize,
                    metadata       : metadataRecordItem.fileMetadata,
                    geometry       : metadataRecordItem.geometry,
                    collections    : null,
                    deleted        : false
                ]
            ]
        ],
        meta: [
            totalResults: 5,
            searchTerms : [
                q   : "dataset:${metadataRecordItem.dataset}",
                from: 0,
                size: 1
            ],
            code        : 200
        ]
    ]

    Map expItem = (Map) metadataRecordItem.clone()
    expItem.remove('extraIgnore')
    expItem.accessProtocol = responseBody.data[0].attributes.access_protocol
    expItem.filePath = responseBody.data[0].attributes.file_path
    expItem.collections = responseBody.data[0].attributes.collections
    expItem.deleted = responseBody.data[0].attributes.deleted
    expItem.lastUpdate = responseBody.data[0].attributes.last_update
    expItem.metadataSchema = responseBody.data[0].attributes.metadata_schema
    expItem.type = responseBody.data[0].attributes.type

    Map expResponseBody = [
        dataset     : responseBody.data[0].attributes.dataset,
        items       : [
            expItem.sort()
        ],
        totalResults: responseBody.data.size(),
        searchTerms : [
            q      : "dataset:${responseBody.data[0].attributes.dataset}",
            offset : responseBody.meta.searchTerms.from,
            max    : responseBody.meta.searchTerms.size,
            dataset: responseBody.data[0].attributes.dataset,
        ],
        code        : responseBody.meta.code
    ]

    expect:
    RequestConversionUtil.transformLegacyGetResponse(responseBody, responseBody.meta.code as Integer) == (expResponseBody.sort() as JSONObject) as String
  }

  def 'Corner cases: legacy transform of GET response body #description works'() {

    expect:
    RequestConversionUtil.transformLegacyGetResponse((Map) responseBody, 1) == (expResponse.sort() as JSONObject) as String

    where:
    description                 | responseBody               | expResponse
    "that's empty"              | [:]                        | [dataset: 'null', items: [], code: 1, totalResults: 0, searchTerms: [:]]
    "that's meta is empty"      | [meta: [:]]                | [dataset: 'null', items: [], code: 1, totalResults: 0, searchTerms: [:]]
    "that's data is empty"      | [data: [:]]                | [dataset: 'null', items: [], code: 1, totalResults: 0, searchTerms: [:]]
    "that's missing attributes" | [data: [[id: 1], [id: 2]]] | [dataset: 'null', items: [], code: 1, totalResults: 0, searchTerms: [:]]
  }

  def 'Happy path: legacy transform of GET params body works'() {

    expect:
    RequestConversionUtil.transformParams(params) == expTransformedParams

    where:
    params                                        | expTransformedParams
    [dataset: ["csb"], max: ["0"], offset: ["0"]] | [q: ["dataset:csb"], max: ["0"], offset: ["0"]]
  }

  def 'Corner cases: legacy transform of GET params #params transforms as expected'() {

    expect:
    RequestConversionUtil.transformParams(params) == expTransformedParams

    where:
    params | expTransformedParams
    null   | [:]
    [:]    | [:]
  }

  def 'Corner cases: legacy acquiring of dataset from query string of "#queryString" works'() {

    expect:
    RequestConversionUtil.getDatasetFromQueryString(queryString) == expDataset
    where:
    queryString     | expDataset
    null            | 'null'
    'dataset: csb'  | 'csb'
    'dataset:csb'   | 'csb'
    ' dataset: csb' | 'csb' // Unsure if even possible via elasticsearch simple query
  }

  def 'Corner cases: legacy transform of search terms #searchTerms works'() {

    expect:
    RequestConversionUtil.getSearchTerms(searchTerms, dataset).sort() == expSearchTerms.sort()

    where:
    searchTerms | dataset | expSearchTerms
    null        | 'null'  | [:]
    null        | 'csb'   | [dataset: 'csb']
    [:]         | 'null'  | [:]
    [size: 2]   | 'null'  | [max: 2]
    [from: 1]   | 'null'  | [offset: 1]

  }
}
