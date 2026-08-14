package com.sergio.financial.importer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {
    List<ImportHistory> findByUserIdOrderByImportedAtDescIdDesc(Long userId);
}
