package com.sergio.financial.importer;

import com.sergio.financial.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportHistoryService {
    private final ImportHistoryRepository histories;
    private final UserRepository users;

    public ImportHistoryService(ImportHistoryRepository histories, UserRepository users) {
        this.histories = histories;
        this.users = users;
    }

    @Transactional
    public void record(Long userId, String originalFilename, int rowCount) {
        histories.save(new ImportHistory(users.getReferenceById(userId), originalFilename, Instant.now(), rowCount));
    }

    @Transactional(readOnly = true)
    public List<ImportHistoryResponse> list(Long userId) {
        return histories.findByUserIdOrderByImportedAtDescIdDesc(userId).stream()
                .map(ImportHistoryResponse::from)
                .toList();
    }
}
