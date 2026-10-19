package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import com.harpenterprises.rmatracker.storage.RmaAttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Step 9E: isolated persistence/retrieval checks; never touches the user's database. */
class RmaAttachmentEndToEndTest {
    @TempDir Path temp;

    static class MemoryRepository extends RmaAttachmentRepository {
        final Map<Long,RmaAttachment> rows = new HashMap<>();
        @Override public RmaAttachment findById(long id) { return rows.get(id); }
        @Override public List<RmaAttachment> findByRmaNumber(String number) {
            return rows.values().stream().filter(a -> number.equals(a.getRmaNumber())).toList();
        }
        @Override public boolean deleteById(long id) { return rows.remove(id) != null; }
    }
    private RmaAttachment record(long id, String rma, Path file) throws Exception {
        return new RmaAttachment(id, rma, file.getFileName().toString(),
                file.getFileName().toString(), file.toString(), Files.size(file),
                "application/pdf", LocalDateTime.now());
    }
    @Test void storedAttachmentSurvivesNewServiceInstance() throws Exception {
        Path root = Files.createDirectories(temp.resolve("attachments"));
        Path original = Files.writeString(temp.resolve("original.pdf"), "PDF content");
        Path managed = Files.copy(original, root.resolve("managed.pdf"));
        MemoryRepository repository = new MemoryRepository();
        repository.rows.put(1L, record(1L, "RMA-001", managed));
        RmaAttachmentStorageService first = new RmaAttachmentStorageService(repository, root);
        assertEquals(managed, first.getFile(1L));
        RmaAttachmentStorageService reopened = new RmaAttachmentStorageService(repository, root);
        assertArrayEquals(Files.readAllBytes(original), Files.readAllBytes(reopened.getFile(1L)));
        assertEquals(1, reopened.list("RMA-001").size());
    }
    @Test void repeatedDocumentIsDetectedAfterServiceRecreation() throws Exception {
        Path root = Files.createDirectories(temp.resolve("attachments"));
        Path original = Files.writeString(temp.resolve("original.pdf"), "PDF content");
        Path managed = Files.copy(original, root.resolve("managed.pdf"));
        MemoryRepository repository = new MemoryRepository();
        repository.rows.put(1L, record(1L, "RMA-001", managed));
        RmaAttachmentStorageService reopened = new RmaAttachmentStorageService(repository, root);
        assertEquals(1L, reopened.findDuplicate("RMA-001", original).getId());
        assertNull(reopened.findDuplicate("RMA-002", original));
    }
    @Test void multipleDocumentsAreListedAndSourceFilesRemainUntouched() throws Exception {
        Path root = Files.createDirectories(temp.resolve("attachments"));
        Path one = Files.writeString(temp.resolve("one.pdf"), "first");
        Path two = Files.writeString(temp.resolve("two.pdf"), "second");
        Path storedOne = Files.copy(one, root.resolve("one.pdf"));
        Path storedTwo = Files.copy(two, root.resolve("two.pdf"));
        MemoryRepository repository = new MemoryRepository();
        repository.rows.put(1L, record(1L, "RMA-001", storedOne));
        repository.rows.put(2L, record(2L, "RMA-001", storedTwo));
        RmaAttachmentStorageService service = new RmaAttachmentStorageService(repository, root);
        assertEquals(2, service.list("RMA-001").size());
        assertTrue(service.remove(1L));
        assertFalse(Files.exists(storedOne));
        assertTrue(Files.exists(one));
        assertTrue(Files.exists(two));
        assertEquals(1, service.list("RMA-001").size());
    }
}
