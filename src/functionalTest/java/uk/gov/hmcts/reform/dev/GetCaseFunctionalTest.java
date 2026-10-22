package uk.gov.hmcts.reform.dev;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItems;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetCaseFunctionalTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storesDatesAsTimestampsWithTimeZone() {
        for (String column : List.of("due_date_time", "created_date")) {
            String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                    + "WHERE table_name = 'case_tasks' AND column_name = ?",
                String.class, column);
            assertEquals("timestamp with time zone", dataType);
        }
    }

    @Test
    void retrievesPersistedTaskById() {
        Response created = given()
            .port(port)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "caseNumber": "ABC12345",
                  "title": "Review evidence",
                  "description": "Read the witness statement",
                  "status": "Accepted",
                  "dueDateTime": "2026-11-01T10:30:00+02:00"
                }
                """)
            .when()
            .post("/cases")
            .then()
            .statusCode(201)
            .extract().response();
        long id = created.jsonPath().getLong("id");

        try {
            assertNotNull(Instant.parse(created.jsonPath().getString("createdDate")));
            given()
                .port(port)
                .when()
                .get("/cases/{id}", id)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(created.jsonPath().get("id")))
                .body("title", equalTo("Review evidence"))
                .body("caseNumber", equalTo("ABC12345"))
                .body("description", equalTo("Read the witness statement"))
                .body("status", equalTo("Accepted"))
                .body("createdDate", equalTo(created.jsonPath().getString("createdDate")))
                .body("dueDateTime", equalTo("2026-11-01T08:30:00Z"));
        } finally {
            caseRepository.deleteById(id);
        }

        given().port(port).when().get("/cases/{id}", id).then().statusCode(404);
    }

    @Test
    void retrievesAllPersistedTasks() {
        int initialCount = given().port(port).when().get("/cases")
            .then().statusCode(200).extract().jsonPath().getInt("totalElements");
        List<Long> createdIds = new ArrayList<>();
        List<Map<String, Object>> createdTasks = new ArrayList<>();
        List<String> requests = List.of(
            """
                {
                  "caseNumber": "ABC12345",
                  "title": "Review evidence",
                  "description": "Read the witness statement",
                  "status": "Accepted",
                  "dueDateTime": "2026-11-01T10:30:00Z"
                }
                """,
            """
                {
                  "caseNumber": "00054321",
                  "title": "Schedule hearing",
                  "status": "Pending",
                  "dueDateTime": "2026-11-02T14:00:00Z"
                }
                """
        );

        try {
            for (String request : requests) {
                Response created = given()
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/cases")
                    .then()
                    .statusCode(201)
                    .extract().response();

                createdIds.add(created.jsonPath().getLong("id"));
                createdTasks.add(created.jsonPath().getMap(""));
            }

            List<Map<String, Object>> tasks = given()
                .port(port)
                .when()
                .get("/cases")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath().getList("content");

            assertTrue(tasks.containsAll(createdTasks),
                       "The response should contain both created tasks with all their saved fields");

            for (int index = 0; index < createdTasks.size(); index++) {
                Response page = given().port(port).queryParam("page", initialCount + index).queryParam("size", 1)
                    .when().get("/cases").then().statusCode(200)
                    .body("page", equalTo(initialCount + index))
                    .body("size", equalTo(1))
                    .body("totalElements", equalTo(initialCount + createdTasks.size()))
                    .body("totalPages", equalTo(initialCount + createdTasks.size()))
                    .extract().response();
                assertEquals(List.of(createdTasks.get(index)), page.jsonPath().getList("content"));
            }

            given().port(port).queryParam("page", initialCount + createdTasks.size()).queryParam("size", 1)
                .when().get("/cases").then().statusCode(200).body("content.size()", equalTo(0));
        } finally {
            caseRepository.deleteAllById(createdIds);
        }
    }

    @Test
    void updatesStatusAndDeletesPersistedTask() {
        Response created = given().port(port).contentType(ContentType.JSON)
            .body("""
                {"caseNumber":"MODIFY123","title":"Review evidence","description":"Read statement",
                 "status":"Accepted","dueDateTime":"2026-11-01T10:30:00Z"}
                """)
            .when().post("/cases").then().statusCode(201).extract().response();
        long id = created.jsonPath().getLong("id");

        try {
            Map<String, Object> expected = created.jsonPath().getMap("");
            expected.put("status", "Completed");

            Response updated = given().port(port).contentType(ContentType.JSON)
                .body("{\"status\":\"Completed\"}")
                .when().patch("/cases/{id}/status", id)
                .then().statusCode(200).extract().response();
            assertEquals(expected, updated.jsonPath().getMap(""));

            Response repeatedUpdate = given().port(port).contentType(ContentType.JSON)
                .body("{\"status\":\"Completed\"}")
                .when().patch("/cases/{id}/status", id)
                .then().statusCode(200).extract().response();
            assertEquals(expected, repeatedUpdate.jsonPath().getMap(""));

            Response retrieved = given().port(port).when().get("/cases/{id}", id)
                .then().statusCode(200).extract().response();
            assertEquals(expected, retrieved.jsonPath().getMap(""));

            given().port(port).when().delete("/cases/{id}", id).then().statusCode(204).body(equalTo(""));
            given().port(port).when().get("/cases/{id}", id).then().statusCode(404);
            given().port(port).when().delete("/cases/{id}", id).then().statusCode(404);
            given().port(port).contentType(ContentType.JSON).body("{\"status\":\"Accepted\"}")
                .when().patch("/cases/{id}/status", id).then().statusCode(404);
        } finally {
            caseRepository.deleteById(id);
        }
    }

    @Test
    void documentsModificationEndpoints() {
        given().port(port).when().get("/v3/api-docs").then().statusCode(200)
            .body("paths.'/cases/{id}/status'.patch.responses.'200'.description", equalTo("Status updated"))
            .body("paths.'/cases/{id}/status'.patch.responses.'400'", notNullValue())
            .body("paths.'/cases/{id}/status'.patch.responses.'404'", notNullValue())
            .body("paths.'/cases/{id}'.delete.responses.'204'.description", equalTo("Task deleted"))
            .body("paths.'/cases/{id}'.delete.responses.'400'", notNullValue())
            .body("paths.'/cases/{id}'.delete.responses.'404'", notNullValue());
    }

    @Test
    void filtersPersistedTasksByStatus() {
        List<Long> ids = new ArrayList<>();
        try {
            for (String taskStatus : List.of("Open", "Closed", "In-Progress")) {
                Response created = given().port(port).contentType(ContentType.JSON)
                    .body("""
                        {"caseNumber":"FILTER123","title":"Filter test","status":"%s",
                         "dueDateTime":"2026-11-01T10:30:00Z"}
                        """.formatted(taskStatus))
                    .when().post("/cases").then().statusCode(201).extract().response();
                ids.add(created.jsonPath().getLong("id"));
            }

            List<String> filters = List.of("open", "closed", "in-progress");
            for (int index = 0; index < filters.size(); index++) {
                Response response = given().port(port).queryParam("status", filters.get(index))
                    .when().get("/cases").then().statusCode(200).extract().response();
                List<Long> returnedIds = response.jsonPath().getList("content.id", Long.class);
                assertTrue(returnedIds.contains(ids.get(index)));
                List<String> statuses = response.jsonPath().getList("content.status", String.class);
                String filter = filters.get(index);
                assertTrue(statuses.stream().allMatch(filter::equalsIgnoreCase));
            }
        } finally {
            caseRepository.deleteAllById(ids);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "pending", "in progress", "inprogress"})
    void rejectsUnsupportedStatusFilters(String filter) {
        given().port(port).queryParam("status", filter)
            .when().get("/cases").then().statusCode(400);
    }

    @Test
    void documentsRetrieveAllEndpoint() {
        given()
            .port(port)
            .when()
            .get("/v3/api-docs")
            .then()
            .statusCode(200)
            .body("paths.'/cases'.get.summary", equalTo("Retrieve all tasks"))
            .body("paths.'/cases'.get.parameters.find { it.name == 'status' }.required", equalTo(false))
            .body("paths.'/cases'.get.parameters.find { it.name == 'status' }.schema.enum",
                  hasItems("open", "closed", "in-progress"))
            .body("paths.'/cases'.get.responses.'200'.description", equalTo("Tasks retrieved successfully"))
            .body("paths.'/cases'.get.responses.'200'.content.'application/json'.schema.'$ref'",
                  equalTo("#/components/schemas/CasePage"))
            .body("components.schemas.CasePage.properties.content.type", equalTo("array"));
    }

    @Test
    void documentsRetrievalEndpoint() {
        given()
            .port(port)
            .when()
            .get("/v3/api-docs")
            .then()
            .statusCode(200)
            .body("paths.'/cases/{id}'.get.responses.'200'.content.'application/json'.schema", notNullValue())
            .body("paths.'/cases/{id}'.get.responses.'400'.description",
                  equalTo("ID must be a valid 64-bit integer"))
            .body("paths.'/cases/{id}'.get.responses.'404'.description", equalTo("Task not found"));
    }
}
