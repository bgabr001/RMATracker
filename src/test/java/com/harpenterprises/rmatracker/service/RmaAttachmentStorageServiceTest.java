package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RmaAttachment;
import com.harpenterprises.rmatracker.storage.RmaAttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/** Phase 3 Step 7F: isolated tests; NEVER connects to the real SQLite database. */
class RmaAttachmentStorageServiceTest {
    @TempDir Path temp;

    private static final class FakeRepository extends RmaAttachmentRepository {
        final Map<Long, RmaAttachment> attachments = new HashMap<>();
        @Override public RmaAttachment findById(long id) { return attachments.get(id); }
        @Override public List<RmaAttachment> findByRmaNumber(String number) {
            List<RmaAttachment> result = new ArrayList<>();
            for (RmaAttachment a : attachments.values()) {
                if (number.equals(a.getRmaNumber())) result.add(a);
            }
            return result;
        }
        @Override public boolean deleteById(long id) { return attachments.remove(id) != null; }
    }

    private RmaAttachment attachment(long id, String rma, Path file) throws IOException {
        return new RmaAttachment(id, rma, file.getFileName().toString(),
                file.getFileName().toString(), file.toString(),
                Files.exists(file) ? Files.size(file) : 0, "application/pdf", LocalDateTime.now());
    }

    @Test void getFileReturnsManagedAttachment() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path file = Files.writeString(root.resolve("document.pdf"), "test document");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", file));
        assertEquals(file, new RmaAttachmentStorageService(repo, root).getFile(1));
    }

    @Test void missingFileProducesHelpfulError() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", root.resolve("missing.pdf")));
        assertThrows(IOException.class, () -> new RmaAttachmentStorageService(repo, root).getFile(1));
    }

    @Test void unknownAttachmentIdIsRejected() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        assertThrows(IOException.class, () -> new RmaAttachmentStorageService(new FakeRepository(), root).getFile(999));
    }

    @Test void outsideStoragePathIsRejected() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path outside = Files.writeString(temp.resolve("private.pdf"), "do not access");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", outside));
        assertThrows(IOException.class, () -> new RmaAttachmentStorageService(repo, root).getFile(1));
        assertEquals("do not access", Files.readString(outside));
    }

    @Test void listOnlyReturnsAttachmentsForRequestedRma() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path first = Files.writeString(root.resolve("one.pdf"), "1");
        Path second = Files.writeString(root.resolve("two.pdf"), "2");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", first));
        repo.attachments.put(2L, attachment(2, "RMA-002", second));
        RmaAttachmentStorageService service = new RmaAttachmentStorageService(repo, root);
        assertEquals(1, service.list("RMA-001").size());
        assertEquals(2L, service.list("RMA-002").get(0).getId());
    }

    @Test void removeDeletesManagedCopyAndMetadata() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path file = Files.writeString(root.resolve("delete-me.pdf"), "sample");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", file));
        RmaAttachmentStorageService service = new RmaAttachmentStorageService(repo, root);
        assertTrue(service.remove(1));
        assertFalse(Files.exists(file));
        assertNull(repo.findById(1));
        assertFalse(service.remove(1));
    }

    @Test void removingStoredCopyDoesNotDeleteSource() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path original = Files.writeString(temp.resolve("original.pdf"), "original bytes");
        Path stored = Files.copy(original, root.resolve("copy.pdf"));
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", stored));
        assertTrue(new RmaAttachmentStorageService(repo, root).remove(1));
        assertTrue(Files.exists(original));
        assertEquals("original bytes", Files.readString(original));
    }

    @Test void addRejectsInvalidInputsBeforeDatabaseAccess() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        RmaAttachmentStorageService service = new RmaAttachmentStorageService(new FakeRepository(), root);
        Path source = Files.writeString(temp.resolve("source.pdf"), "test");
        assertThrows(IllegalArgumentException.class, () -> service.add("  ", source));
        assertThrows(IOException.class, () -> service.add("RMA-001", temp.resolve("not-there.pdf")));
        assertThrows(IOException.class, () -> service.add("RMA-001", temp));
    }
    @Test void duplicateContentWithinSameRmaIsFoundEvenWithDifferentName() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path stored = Files.writeString(root.resolve("stored.pdf"), "same bytes");
        Path incoming = Files.writeString(temp.resolve("incoming.pdf"), "same bytes");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", stored));
        assertEquals(1L, new RmaAttachmentStorageService(repo, root)
                .findDuplicate("RMA-001", incoming).getId());
    }

    @Test void sameFilenameWithDifferentContentIsNotDuplicate() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path stored = Files.writeString(root.resolve("report.pdf"), "old version");
        Path incoming = Files.writeString(temp.resolve("report.pdf"), "new version");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-001", stored));
        assertNull(new RmaAttachmentStorageService(repo, root)
                .findDuplicate("RMA-001", incoming));
    }

    @Test void identicalContentInDifferentRmaIsNotDuplicate() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path stored = Files.writeString(root.resolve("stored.pdf"), "same bytes");
        Path incoming = Files.writeString(temp.resolve("incoming.pdf"), "same bytes");
        FakeRepository repo = new FakeRepository();
        repo.attachments.put(1L, attachment(1, "RMA-OTHER", stored));
        assertNull(new RmaAttachmentStorageService(repo, root)
                .findDuplicate("RMA-001", incoming));
    }

    @Test void missingManagedFileDoesNotCountAsDuplicate() throws Exception {
        Path root = Files.createDirectory(temp.resolve("attachments"));
        Path incoming = Files.writeString(temp.resolve("incoming.pdf"), "same bytes");
        FakeRepository repo = new FakeRepository();
        Path missing = root.resolve("missing.pdf");
        repo.attachments.put(1L, new RmaAttachment(1, "RMA-001", "missing.pdf",
                "missing.pdf", missing.toString(), Files.size(incoming),
                "application/pdf", LocalDateTime.now()));
        assertNull(new RmaAttachmentStorageService(repo, root)
                .findDuplicate("RMA-001", incoming));
    }

}
