package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.dev.models.CaseTask;

import java.util.List;

public interface CaseRepository extends JpaRepository<CaseTask, Long> {

    List<CaseTask> findByStatusIgnoreCase(String status);
}
