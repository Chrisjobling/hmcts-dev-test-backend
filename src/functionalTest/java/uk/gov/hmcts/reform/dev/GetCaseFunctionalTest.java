package uk.gov.hmcts.reform.dev;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetCaseFunctionalTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private CaseRepository caseRepository;

    @Test
    void retrievesPersistedTaskById() {
        Response created = given()
            .port(port)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "title": "Review evidence",
                  "description": "Read the witness statement",
                  "status": "Accepted",
                  "dueDateTime": "2026-11-01T10:30:00"
                }
                """)
            .when()
            .post("/cases")
            .then()
            .statusCode(201)
            .extract().response();
        long id = created.jsonPath().getLong("id");

        try {
            given()
                .port(port)
                .when()
                .get("/cases/{id}", id)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(created.jsonPath().get("id")))
                .body("title", equalTo("Review evidence"))
                .body("description", equalTo("Read the witness statement"))
                .body("status", equalTo("Accepted"))
                .body("dueDateTime", equalTo("2026-11-01T10:30:00"));
        } finally {
            caseRepository.deleteById(id);
        }

        given().port(port).when().get("/cases/{id}", id).then().statusCode(404);
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
