package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.dev.models.CaseTask;

public interface CaseRepository extends JpaRepository<CaseTask, Long> {

    Page<CaseTask> findByStatusIgnoreCase(String status, Pageable pageable);
}
