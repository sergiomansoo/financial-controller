package com.sergio.financial.importer;

import com.sergio.financial.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "import_history")
public class ImportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    protected ImportHistory() {
    }

    public ImportHistory(User user, String originalFilename, Instant importedAt, int rowCount) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.importedAt = importedAt;
        this.rowCount = rowCount;
    }

    public String getOriginalFilename() { return originalFilename; }
    public Instant getImportedAt() { return importedAt; }
    public int getRowCount() { return rowCount; }
}
