package org.bahmni.module.bahmniOfflineSync.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.codehaus.jackson.map.ObjectMapper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.PlatformTransactionManager;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class AddressEntriesSyncStrategyTest {

	private AddressEntriesSyncStrategy addressEntriesSyncStrategy;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private PatientService patientService;
	@Mock
	private EncounterService encounterService;
	@Mock
	private PlatformTransactionManager platformTransactionManager;
	@Mock
	private AddressHierarchyService addressHierarchyService;
	@Mock
	private AdministrationService adminService;
	@Mock
	private LocationService locationService;
	private Patient patient;
	Encounter encounter;
	private String encounterUuid = "ff17adba-9750-462e-be29-e35091af93df";
	private String patientUuid = "ff17adba-9750-462e-be29-e35091af93dd";

	private AddressHierarchyEntry europe;
	private AddressHierarchyEntry france;
	private AddressHierarchyEntry paris;
	private AddressHierarchyEntry champsElysees;
	
	@Before
	public void setUp() throws Exception {
		initMocks(this);
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getPatientService()).thenReturn(patientService);
		Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
		Mockito.when(Context.getLocationService()).thenReturn(locationService);
		Mockito.when(Context.getAdministrationService()).thenReturn(adminService);
		Mockito.when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

		List<PlatformTransactionManager> registeredComponents = new ArrayList<PlatformTransactionManager>();
		registeredComponents.add(platformTransactionManager);
		Mockito.when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);
		
		addressEntriesSyncStrategy = new AddressEntriesSyncStrategy();
		patient = new Patient();
		patient.setUuid(patientUuid);
		encounter = new Encounter();
		encounter.setPatient(patient);
		encounter.setUuid(encounterUuid);

		// Setup an AH of 4 Address Entries of different levels
		europe = new AddressHierarchyEntry();
		AddressHierarchyLevel l1 = new AddressHierarchyLevel();
		l1.setLevelId(1);
		europe.setLevel(l1);
		europe.setName("Europe");
		europe.setId(1);
		europe.setUserGeneratedId("763f4c7b-01a0-4f7c-8d7c-b8b620f2bab5");
		europe.setUuid("cb9483eb-c125-4dcf-a215-db88d6e4c59e");
		
		france = new AddressHierarchyEntry();
		AddressHierarchyLevel l2 = new AddressHierarchyLevel();
		l2.setLevelId(2);
		france.setLevel(l2);
		france.setName("France");
		france.setParent(europe);
		france.setId(2);
		france.setUserGeneratedId("107e1300-c946-481d-88bd-8062f8c20f83");
		france.setUuid("8b3f02f2-d4fc-442f-9d74-ce73605106ab");

		paris = new AddressHierarchyEntry();
		AddressHierarchyLevel l3 = new AddressHierarchyLevel();
		l3.setLevelId(3);
		paris.setLevel(l3);
		paris.setName("Paris");
		paris.setParent(france);
		paris.setId(3);
		paris.setUserGeneratedId("9f120e7d-9da6-4756-9b40-5c13a257318c");
		paris.setUuid("6eba31d7-2048-4c53-a466-43848fb50c66");

		champsElysees = new AddressHierarchyEntry();
		AddressHierarchyLevel l4 = new AddressHierarchyLevel();
		l4.setLevelId(4);
		champsElysees.setLevel(l4);
		champsElysees.setName("Champs-Elysées");
		champsElysees.setParent(paris);
		champsElysees.setId(4);
		champsElysees.setUserGeneratedId("1313ac4e-7763-46a6-b6da-cb35f16873bb");
		champsElysees.setUuid("9b4b8b13-ed65-4b98-a158-c1c78d7da6ed");

	}

	@Test
	public void shouldGetCategoryList() throws Exception {
		List<String> categories = addressEntriesSyncStrategy.getEventCategoriesList();
		assertTrue(categories.contains("patient"));
		assertTrue(categories.contains("encounter"));
		assertTrue(categories.contains("addressHierarchy"));
		assertTrue(categories.contains("offline-concepts"));
		assertTrue(categories.contains("forms"));
		assertTrue(categories.size() == 5);
	}

	@Test
	public void shouldGetFilterForDevice() {

		ArrayList<String> expectedFilter = new ArrayList<String>();
		expectedFilter.add(AddressEntriesSyncStrategy.FILTER_UUID);

		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("patient"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("encounter"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("addressHierarchy"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("offline-concepts"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("forms"));
		assertEquals(5, addressEntriesSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid").size());
	}

	@Test
	public void shouldSetFilterForAllEncounters() {

		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		EventRecord er = new EventRecord("uuid", "encounter", "", "url/"+ encounterUuid, new Date(), "Encounter");
		eventRecords.add(er);
		List<EventLog> eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);
		assertEquals(eventRecords.size(), eventLogs.size());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
	}

	@Test
	public void shouldSetFilterWhenAddressEventExactMatchesGP() {
		String JSON = updateJSON(Arrays.asList(paris, france));
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(JSON);

		List<EventLog> eventLogs = setupEventLogsAndFilters(JSON);
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
	}

	@Test
	public void shouldSetFilterWhenAddressEventIsChildOfAnAddressInGP() {
		String JSON = updateJSON(Arrays.asList(paris, france));
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(JSON);

		String champsFilter = setupEventLogsAndFilters(JSON).get(3).getFilter();
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, champsFilter);
	}

	@Test
	public void shouldNotSetFilterWhenAddressEventIsNotInGP() {
		String JSON = updateJSON(Arrays.asList(paris, france));
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(JSON);

		String europeFilter = setupEventLogsAndFilters(JSON).get(0).getFilter();
		assertEquals(null, europeFilter);
	}
	
	@Test
	public void shouldSetFilterOnAllLocationsWhenGPIsEmptyOrNull() {
		
		List<EventLog> eventLogs = new ArrayList<>();
				
		when(adminService.getGlobalProperty(any())).thenReturn(null);
		eventLogs = setupEventLogsAndFilters(null);

		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(3).getFilter());
		
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn("");
		eventLogs = setupEventLogsAndFilters("");
		
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(3).getFilter());
		
	}
	

	@Test
	public void shouldHandleUsergGeneratedIdOnlyGP() {
		
		String JSON = "[{\"userGeneratedId\":\"107e1300-c946-481d-88bd-8062f8c20f83\"},{\"userGeneratedId\":\"9f120e7d-9da6-4756-9b40-5c13a257318c\"}]";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(JSON);
		
		List<EventLog> eventLogs = setupEventLogsAndFilters(JSON);
		assertEquals(null, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(3).getFilter());
		
	}

	private String updateJSON(List<AddressHierarchyEntry> addresses) {
		String addressesToMatchAsJSON = "";
		ObjectMapper mapper = new ObjectMapper();
		try {
			addressesToMatchAsJSON = mapper.writeValueAsString(addresses);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return addressesToMatchAsJSON;
	}

	private List<EventLog> setupEventLogsAndFilters(String JSON) {

		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		List<EventLog> eventLogs = new ArrayList<EventLog>();

		when(addressHierarchyService.getAddressHierarchyEntryByUuid(europe.getUuid())).thenReturn(europe);
		when(addressHierarchyService.getAddressHierarchyEntryByUuid(france.getUuid())).thenReturn(france);
		when(addressHierarchyService.getAddressHierarchyEntryByUuid(paris.getUuid())).thenReturn(paris);
		when(addressHierarchyService.getAddressHierarchyEntryByUuid(champsElysees.getUuid())).thenReturn(champsElysees);
		
		when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(europe.getUserGeneratedId())).thenReturn(europe);
		when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(france.getUserGeneratedId())).thenReturn(france);
		when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(paris.getUserGeneratedId())).thenReturn(paris);
		when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(champsElysees.getUserGeneratedId())).thenReturn(champsElysees);

		EventRecord erEurope  = new EventRecord("uuid","address","","url/"+ europe.getUuid(),new Date(),"addressHierarchy");
		EventRecord erFrance  = new EventRecord("uuid","address","","url/"+ france.getUuid(),new Date(),"addressHierarchy");
		EventRecord erParis  = new EventRecord("uuid","address","","url/"+ paris.getUuid(),new Date(),"addressHierarchy");
		EventRecord erChamps  = new EventRecord("uuid","address","","url/"+ champsElysees.getUuid(),new Date(),"addressHierarchy");

		eventRecords.add(erEurope);
		eventRecords.add(erFrance);
		eventRecords.add(erParis);
		eventRecords.add(erChamps);

		eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);

		return eventLogs;
	}


}
