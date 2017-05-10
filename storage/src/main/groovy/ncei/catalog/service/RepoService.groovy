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

  Map save(CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    log.info("Attempting to save ${metadataRecord.class} with id: ${metadataRecord.id}")
    log.debug("Metadata record ${metadataRecord.id}- ${metadataRecord.asMap()}")
    Map saveDetails = [:]
    UUID metadataId = metadataRecord.id
    //Make sure record does not already exist
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      log.info("Conflicting record: ${metadataId} already exists")
      log.debug("Existing record: ${result.first()}")
      saveDetails.code = HttpServletResponse.SC_CONFLICT
      saveDetails.error = "Record already exists"
      saveDetails.success = false
      return saveDetails
    } else { //create a new one
      //save the row
      log.info("Saving new record: ${metadataRecord.id}")
      MetadataRecord saveResult = repositoryObject.save(metadataRecord)
      log.debug("Response from cassandra for record with id ${metadataRecord.id}: $saveResult")
      saveDetails.records = []
      saveDetails.records.add(saveResult)
      saveDetails.recordsCreated = 1
      saveDetails.code = HttpServletResponse.SC_CREATED
      saveDetails.success = true
      messageService.notifyIndex('insert', (getTableFromClass(metadataRecord)), saveResult.id as String, metadataRecord.asMap())
    }
    saveDetails
  }

  Map update(CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    log.info("Attempting to update ${metadataRecord.class} with id: ${metadataRecord.id}")
    Map updateDetails = [:]
    UUID metadataId = metadataRecord.id
    //get existing row
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      //optimistic lock
      if (result.first().last_update != metadataRecord.last_update) {
        log.info("Failing update for out-of-date version: $metadataId")
        updateDetails.error = 'You are not editing the most recent version.'
        updateDetails.code = HttpServletResponse.SC_CONFLICT
        updateDetails.success = false
        return updateDetails
      } else {
        log.info("Updating record with id: $metadataId")
        log.debug("Updated record: ${metadataRecord}")
        updateDetails.records = []
        updateDetails.totalResultsUpdated = 1
        updateDetails.code = HttpServletResponse.SC_OK
        updateDetails.success = true

        metadataRecord.last_update = new Date()
        MetadataRecord record = repositoryObject.save(metadataRecord)
        updateDetails.records.add(record)
        messageService.notifyIndex('update', getTableFromClass(metadataRecord), metadataRecord.id as String, metadataRecord.asMap())
      }
    } else {
      log.debug("No record found for id: $metadataId")
      updateDetails.error = 'Record does not exist.'
      updateDetails.code = HttpServletResponse.SC_NOT_FOUND
      updateDetails.success = false
      return updateDetails
    }
    updateDetails
  }

  Map list(CassandraRepository repositoryObject, Map params = null) {
    log.info("Fulfilling ${params.table} list request with params: $params")
    Map listDetails = [:]
    Boolean showVersions = params?.showVersions
    Boolean showDeleted = params?.showDeleted
    UUID metadataId = params?.id ? UUID.fromString(params.id) : null

    //the iterable returned from the repository class
    Iterable allResults

    //find my id if specified, else select all
    if (metadataId) {
      if (showVersions) {
        log.info("Querying for all results with id: $metadataId")
        allResults = repositoryObject.findByMetadataId(metadataId)
      } else {
        log.info("Querying for latest result with id: $metadataId")
        allResults = repositoryObject.findByMetadataIdLimitOne(metadataId)
      }
    } else {
      log.info("Querying for all $params.table records")
      allResults = repositoryObject.findAll()
    }

    //filter deleted and versions - or don't
    if (showDeleted && showVersions) {
      log.debug("Returning all records, including old versions and deleted records")
      listDetails.records = allResults.asList()
    } else if (showVersions) {
      log.debug("Filtering deleted records")
      List deletedList = []
      listDetails.records = allResults.findAll { mR -> //mR for metadataRecord
        UUID id = mR.id
        if (mR.deleted) {
          deletedList.add(id)
        }
        (!mR.deleted && !(id in deletedList))
      }
    } else {
      log.debug("Filtering old versions and deleted records")
      listDetails.records =  getMostRecent(allResults, showDeleted)
    }

    //build service response
    if(listDetails.records){
      listDetails.code = HttpServletResponse.SC_OK
      listDetails.success = true
    }else{
      listDetails.code = HttpServletResponse.SC_NOT_FOUND
      listDetails.success = false
    }
    listDetails
  }

  List getMostRecent(Iterable allResults, Boolean showDeleted) {
    Map idMostRecentMap = [:]
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
      value
    }
    mostRecent
  }

  Map purge(CassandraRepository repositoryObject, Map params) {
    UUID metadataId = UUID.fromString(params.id)
    log.warn("Purging all records with id: $metadataId")

    Map purgeDetails = [:]
    int count = 0
    Iterable<MetadataRecord> items

    if (metadataId) {
      items = repositoryObject.findByMetadataId(metadataId)
    } else {
      log.warn("Failing purge request")
      purgeDetails.error = 'An ID parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      purgeDetails.success = false
      return purgeDetails
    }

    purgeDetails.records = []

    items.each {
      purgeDetails.records.add(it)
      UUID id = it.id
      delete(repositoryObject, id, it.last_update)
      messageService.notifyIndex('delete', getTableFromClass(it), it.id as String, it.asMap())
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails.success = true
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

  Map softDelete(CassandraRepository repositoryObject, UUID id, Date timestamp) {
    log.info("Attempting soft delete record with id: $id, timestamp: $timestamp")
    Map deleteDetails = [:]
    Iterable rowToBeDeleted = repositoryObject.findByMetadataId(id)
    if (rowToBeDeleted) {
      def record = rowToBeDeleted.first()
      if (record.last_update != timestamp) {
        log.debug("Failing soft delete on out-of-date record with id: $id, timestamp: $timestamp")
        deleteDetails.success = false
        deleteDetails.error = 'You are not editing the most recent version.'
        deleteDetails.code = HttpServletResponse.SC_CONFLICT
        return deleteDetails
      }
      record.last_update = new Date()
      record.deleted = true
      log.info("Soft delete successful for record with id: $id")
      MetadataRecord newRecord = repositoryObject.save(record)
      messageService.notifyIndex('delete', getTableFromClass(newRecord), newRecord.id as String, newRecord.asMap())
      deleteDetails.records = newRecord
      deleteDetails.success = true
      deleteDetails.message = 'Successfully deleted row with id: ' + id
      deleteDetails.code = HttpServletResponse.SC_OK
      return deleteDetails
    } else {
      log.warn("Failing soft delete for non-existant record with id: $id")
      deleteDetails.success = false
      deleteDetails.error = 'No record found with id: ' + id
      deleteDetails.code = HttpServletResponse.SC_OK
      return deleteDetails
    }
  }

  def getTableFromClass(MetadataRecord metadataRecord) {
    switch (metadataRecord.class) {
      case CollectionMetadata:
        return 'collection'
        break
      case GranuleMetadata:
        return 'granule'
        break
      case MetadataSchema:
        return 'schema'
        break
    }
  }

}
