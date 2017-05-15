package ncei.catalog.service

import groovy.util.logging.Slf4j
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.MetadataRecord
import ncei.catalog.domain.MetadataSchema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Slf4j
@Component
class RepoService {

  @Autowired
  MessageService messageService

  Map save(HttpServletResponse response, CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    log.info("Attempting to save ${metadataRecord.recordTable()} with id: ${metadataRecord.id}")
    log.debug("Metadata record ${metadataRecord.id}- ${metadataRecord.asMap()}")
    Map saveDetails = [:]
    saveDetails.meta = [action: 'insert']
    UUID metadataId = metadataRecord.id
    //Make sure record does not already exist
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      log.info("Conflicting record: ${metadataId} already exists")
      log.debug("Existing record: ${result.first()}")
      response.status = HttpServletResponse.SC_CONFLICT
      saveDetails.errors = ['Record already exists']
      saveDetails.meta += [code: HttpServletResponse.SC_CONFLICT, success: false]
      return saveDetails
    } else { //create a new one
      //save the row
      log.info("Saving new record: ${metadataRecord.id}")
      MetadataRecord saveResult = repositoryObject.save(metadataRecord)
      log.debug("Response from cassandra for record with id ${metadataRecord.id}: $saveResult")
      saveDetails.data = []
      saveDetails.data.add(createDataItem(metadataRecord))
      saveDetails.meta += [recordsCreated: 1, code: HttpServletResponse.SC_CREATED, success: true]
      response.status = HttpServletResponse.SC_CREATED
      messageService.notifyIndex(saveDetails)
    }
    saveDetails
  }

  Map update(HttpServletResponse response, CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    log.info("Attempting to update ${metadataRecord.recordTable()} with id: ${metadataRecord.id}")
    Map updateDetails = [:]
    updateDetails.meta = [action: 'update']
    UUID metadataId = metadataRecord.id
    //get existing row
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      //optimistic lock
      if (result.first().last_update != metadataRecord.last_update) {
        log.info("Failing update for out-of-date version: $metadataId")
        updateDetails.errors = ['You are not editing the most recent version.']
        updateDetails.meta += [code:HttpServletResponse.SC_CONFLICT, success: false]
        response.status = HttpServletResponse.SC_CONFLICT
        return updateDetails
      } else {
        log.info("Updating record with id: $metadataId")
        log.debug("Updated record: ${metadataRecord}")
        metadataRecord.last_update = new Date()
        MetadataRecord record = repositoryObject.save(metadataRecord)
        updateDetails.data = [createDataItem(record)]
        updateDetails.meta += [totalResultsUpdated : 1, code: HttpServletResponse.SC_OK, success: true]
        response.status = HttpServletResponse.SC_OK
        messageService.notifyIndex(updateDetails)
      }
    } else {
      log.debug("No record found for id: $metadataId")
      updateDetails.errors = ['Record does not exist.']
      updateDetails.meta += [success : false, code: HttpServletResponse.SC_NOT_FOUND]
      response.status = HttpServletResponse.SC_NOT_FOUND
      return updateDetails
    }
    updateDetails
  }

  Map list(HttpServletResponse response, CassandraRepository repositoryObject, Map params = null) {
    log.info("Fulfilling list request with params: $params")
    Map listDetails = [:]
    listDetails.meta = [action:'read']
    Boolean showVersions = params?.showVersions
    Boolean showDeleted = params?.showDeleted
    UUID metadataId = params?.id ? UUID.fromString(params.id) : null

    //the iterable returned from the repository class
    Iterable<MetadataRecord> allResults

    //find by id if specified, else select all
    if (metadataId) {
      if (showVersions) {
        log.info("Querying for all results with id: $metadataId")
        allResults = repositoryObject.findByMetadataId(metadataId)
      } else {
        log.info("Querying for latest result with id: $metadataId")
        allResults = repositoryObject.findByMetadataIdLimitOne(metadataId)
      }
    } else {
      log.info("Querying for all records")
      allResults = repositoryObject.findAll()
    }

    //filter deleted and versions - or don't
    if (showDeleted && showVersions) {
      log.debug("Returning all records, including old versions and deleted records")
      listDetails.data = allResults.collect{createDataItem(it)}
    } else if (showVersions) {
      log.debug("Filtering deleted records")
      List deletedList = []
      listDetails.data = []
      allResults.each{ mR ->
        UUID id = mR.id
        if (mR.deleted) {
          deletedList.add(id)
        }
        else if(!(id in deletedList)){
          listDetails.data.add(createDataItem(mR))
        }
      }
    } else {
      log.debug("Filtering old versions and deleted records")
      listDetails.data =  getMostRecent(allResults, showDeleted)
    }

    //build response
    if(listDetails.data){
      response.status = HttpServletResponse.SC_OK
      listDetails.meta += [totalResults : listDetails.data.size, code: HttpServletResponse.SC_OK, success: true]
    }else{
      listDetails.remove('data')
      listDetails.errors = ['No results found.']
      response.status = HttpServletResponse.SC_NOT_FOUND
      listDetails.meta += [totalResults : 0, code: HttpServletResponse.SC_NOT_FOUND, success: false]
    }
    listDetails
  }

  List getMostRecent(Iterable<MetadataRecord> allResults, Boolean showDeleted) {
    Map<UUID, MetadataRecord> idMostRecentMap = [:]
    List mostRecent
    List deletedList = []
    allResults.each { mR ->
      if (!showDeleted && mR.deleted) {
        deletedList.add(mR.id)
      }
      if (!(mR.id in deletedList)) {
        String metadataId = mR.id
        if (idMostRecentMap[metadataId]) {
          if (idMostRecentMap[metadataId].last_update < mR.last_update) {
            idMostRecentMap[metadataId] = mR
          }
        } else {
          idMostRecentMap[metadataId] = mR
        }
      }
    }
    mostRecent = idMostRecentMap.collect { key, value ->
      createDataItem(value)
    }
    mostRecent
  }

  Map purge(HttpServletResponse response, CassandraRepository repositoryObject, Map params) {
    UUID metadataId = UUID.fromString(params.id)
    log.warn("Purging all records with id: $metadataId")
    Map purgeDetails = [:]
    purgeDetails.meta = [action: 'delete']
    int count = 0
    Iterable<MetadataRecord> items

    if (metadataId) {
      items = repositoryObject.findByMetadataId(metadataId)
    } else {
      log.warn("Failing purge request")
      purgeDetails.errors = ['An ID parameter is required to purge']
      purgeDetails.meta += [totalResultsDeleted : count, code: HttpServletResponse.SC_BAD_REQUEST, success: false]
      response.status = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    if(items){
      purgeDetails.data = []
      items.each {
        purgeDetails.data.add(createDataItem(it))
        UUID id = it.id
        delete(repositoryObject, id, it.last_update)
        count++
      }
      purgeDetails.meta += [id:metadataId, totalResultsDeleted: count, code: HttpServletResponse.SC_OK, success: true]
      response.status = HttpServletResponse.SC_OK
    }else{
      purgeDetails.errors =['No records exist with id: ' + metadataId]
      purgeDetails.meta += [id:metadataId, totalResultsDeleted: count, code: HttpServletResponse.SC_NOT_FOUND, success: false]
      response.status = HttpServletResponse.SC_NOT_FOUND
    }
    purgeDetails
  }

  def delete(CassandraRepository repositoryObject, UUID id, Date timestamp = null) {
    log.info("Deleting record with id: $id, timestamp: $timestamp")
    if (timestamp) {
      repositoryObject.deleteByMetadataIdAndLastUpdate(id, timestamp)
    } else {
      Iterable mR = repositoryObject.findByMetadataId(id as UUID)
      if (mR) {
        timestamp = mR.first().last_update as Date
      } else {
        throw RuntimeException("No such id")
      }
      repositoryObject.deleteByMetadataIdAndLastUpdate(id, timestamp)
    }
  }

  Map softDelete(HttpServletResponse response, CassandraRepository repositoryObject, UUID id, Date timestamp) {
    log.info("Attempting soft delete record with id: $id, timestamp: $timestamp")
    Map deleteDetails = [:]
    deleteDetails.meta = [action: 'delete']
    Iterable rowToBeDeleted = repositoryObject.findByMetadataIdLimitOne(id)
    if (rowToBeDeleted) {
      def record = rowToBeDeleted.first()
      if (record.last_update != timestamp) {
        log.info("Failing soft delete on out-of-date record with id: $id, timestamp: $timestamp")
        deleteDetails.meta += [success: false, code: HttpServletResponse.SC_CONFLICT]
        deleteDetails.errors = ['You are not deleting the most recent version.']
        response.status = HttpServletResponse.SC_CONFLICT
      }else{
        record.last_update = new Date()
        record.deleted = true
        log.info("Soft delete successful for record with id: $id")
        MetadataRecord newRecord = repositoryObject.save(record)
        deleteDetails.data = [createDataItem(newRecord)]
        deleteDetails.meta += [success: true, message: ('Successfully deleted row with id: ' + id) , code: HttpServletResponse.SC_OK]
        response.status = HttpServletResponse.SC_OK
        messageService.notifyIndex(deleteDetails)
      }
      return deleteDetails
    } else {
      log.warn("Failing soft delete for non-existant record with id: $id")
      deleteDetails.meta += [success: false, message: ('Failed to deleted row with id: ' + id) , code: HttpServletResponse.SC_NOT_FOUND]
      deleteDetails.errors = ['No record found with id: ' + id]
      response.status = HttpServletResponse.SC_NOT_FOUND
      return deleteDetails
    }
  }

  Map createDataItem(MetadataRecord metadataRecord){
    [id: metadataRecord.id, type: metadataRecord.recordTable(), attributes: metadataRecord]
  }

}
