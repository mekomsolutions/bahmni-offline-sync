package org.bahmni.module.bahmniOfflineSync.strategy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.Concept;

public class AllPatientsSyncStrategy extends AbstractOfflineSyncStrategy {

	public static String FILTER_UUID = "0e4638e7-7639-4da3-8782-b29d54bb3e22";

	public AllPatientsSyncStrategy() throws SQLException {
		super();
	}

	public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
		Map<String, List<String>> categoryFilterMap = new HashMap<String, List<String>>();

		ArrayList<String> filters = new ArrayList<String>();
		filters.add(FILTER_UUID);

		categoryFilterMap.put("patient", filters);
		categoryFilterMap.put("encounter", filters);
		categoryFilterMap.put("addressHierarchy", filters);
		categoryFilterMap.put("offline-concepts", filters);
		categoryFilterMap.put("forms", filters);

		return categoryFilterMap;
	}


	@Override
	public List<String> getEventCategoriesList() {
		List<String> eventCategoryList = new ArrayList<String>();

		eventCategoryList.add("patient");
		eventCategoryList.add("encounter");
		eventCategoryList.add("addressHierarchy");
		eventCategoryList.add("offline-concepts");
		eventCategoryList.add("forms");

		return eventCategoryList;
	}


	@Override
	public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
		List<EventLog> eventLogs = new ArrayList<EventLog>();

		for (EventRecord er : eventRecords) {
			EventLog eventLog = new EventLog(er.getUuid(),er.getCategory(),er.getTimeStamp(),er.getContents(), er.getUuid(), er.getUuid());
			String category = er.getCategory();
			String uuid = getUuidFromURL(er.getContents());
			String filter = null;

			if (!uuid.isEmpty()) {
				if (category.equalsIgnoreCase("all-concepts")) {
					if (isOfflineConceptEvent(uuid)) {
						eventLog.setCategory("offline-concepts");
					} else {
						eventLog.setCategory("concepts");
					}
				}

				if (category.equalsIgnoreCase("Patient")|| category.equalsIgnoreCase("LabOrderResults"))
					filter = evaluateFilterForPatient(uuid);
				else if (category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter"))
					filter = evaluateFilterForEncounter(uuid);
				else if (category.equalsIgnoreCase("addressHierarchy"))
					filter = evaluateFilterForAddressHierarchy(uuid);
			}
			eventLog.setFilter(filter);
			eventLogs.add(eventLog);
		}
		return eventLogs;
	}

	private boolean isOfflineConceptEvent(String eventUuid) {
		final Concept concept = conceptService.getConceptByUuid(eventUuid);
		final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
		return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
	}

	protected String evaluateFilterForPatient(String uuid) {
		return FILTER_UUID;
	}

	private String evaluateFilterForEncounter(String uuid) {
		return FILTER_UUID;
	}

	private String evaluateFilterForAddressHierarchy(String uuid) {
		return FILTER_UUID;
	}

}
