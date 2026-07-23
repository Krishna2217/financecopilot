package com.microsoft.financecopilot.report;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutiveReportRepository extends JpaRepository<ExecutiveReportEntity, Long> {

  Optional<ExecutiveReportEntity> findByReportYearAndReportMonth(int reportYear, int reportMonth);
}
