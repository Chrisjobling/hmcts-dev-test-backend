package uk.gov.hmcts.reform.dev.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExampleCase {

    private int id;
    private String caseNumber;
    private String title;
    private String description;
    private String status;
    private Instant createdDate;
}
