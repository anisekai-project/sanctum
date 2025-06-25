package fr.anisekai.sanctum;

import fr.anisekai.sanctum.entities.ScopedEntityA;
import fr.anisekai.sanctum.entities.ScopedEntityB;
import fr.anisekai.sanctum.enums.StorePolicy;
import fr.anisekai.sanctum.exceptions.LibraryException;
import fr.anisekai.sanctum.exceptions.ResolveOutOfBoundException;
import fr.anisekai.sanctum.exceptions.StorageException;
import fr.anisekai.sanctum.exceptions.StoreRegistrationException;
import fr.anisekai.sanctum.exceptions.context.ContextUnavailableException;
import fr.anisekai.sanctum.exceptions.scope.ScopeDefinitionException;
import fr.anisekai.sanctum.exceptions.scope.ScopeGrantException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.Library;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.stores.RawStorage;
import fr.anisekai.sanctum.stores.ScopedDirectoryStorage;
import fr.anisekai.sanctum.stores.ScopedFileStorage;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.UUID;

@DisplayName("Library Storage")
@Tags({@Tag("unit-test"), @Tag("library-storage")})
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class SanctumTests {

    public static final Path TEST_DATA_PATH    = Path.of("test-data");
    public static final Path TEST_LIBRARY_PATH = TEST_DATA_PATH.resolve("library");
    public static final Path TEST_FILE_PATH    = TEST_DATA_PATH.resolve("file.txt");

    private static String randomName() {

        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private static FileStore randomRaw() {

        return new RawStorage(randomName());
    }

    private static FileStore randomDirStore(Class<? extends ScopedEntity> entityClass) {

        return new ScopedDirectoryStorage(randomName(), entityClass);
    }

    private static FileStore randomFileStore(Class<? extends ScopedEntity> entityClass) {

        return new ScopedFileStorage(randomName(), entityClass, "txt");
    }

    @BeforeEach
    public void beforeEach() throws IOException {

        if (!Files.exists(TEST_LIBRARY_PATH)) {
            Files.createDirectories(TEST_LIBRARY_PATH);
        }

        if (!Files.exists(TEST_FILE_PATH)) {
            Files.createFile(TEST_FILE_PATH);
        }
    }

    @AfterEach
    public void cleanup() throws IOException {

        SanctumUtils.delete(TEST_DATA_PATH);
    }

    @Test
    @DisplayName("Library Creation | On File")
    public void testLibraryCreationOnFile() {


        LibraryException ex = Assertions.assertThrows(
                LibraryException.class,
                () -> new Sanctum(TEST_FILE_PATH)
        );

        Assertions.assertTrue(ex.getMessage().contains("Directory was expected"), ex.getMessage());
    }

    @Test
    @DisplayName("Store Registration | Name Clash")
    public void testStoreRegistrationNameClashes() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore store = randomRaw();

            Assertions.assertDoesNotThrow(() -> manager.registerStore(store, StorePolicy.PRIVATE));

            StoreRegistrationException ex = Assertions.assertThrows(
                    StoreRegistrationException.class,
                    () -> manager.registerStore(store, StorePolicy.PRIVATE)
            );

            Assertions.assertTrue(ex.getMessage().contains("already exists"), ex.getMessage());
        }
    }

    @Test
    @DisplayName("Store Registration | Raw Stores policies")
    public void testStoreRegistrationPolicyForRawStores() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore sPrivate   = randomRaw();
            FileStore sOverwrite = randomRaw();
            FileStore sFullSwap  = randomRaw();
            FileStore sDiscard   = randomRaw();

            Assertions.assertDoesNotThrow(() -> manager.registerStore(sPrivate, StorePolicy.PRIVATE));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sDiscard, StorePolicy.DISCARD));

            StoreRegistrationException ex;

            ex = Assertions.assertThrows(
                    StoreRegistrationException.class,
                    () -> manager.registerStore(sOverwrite, StorePolicy.OVERWRITE)
            );

            Assertions.assertTrue(ex.getMessage().contains("policy"), ex.getMessage());

            ex = Assertions.assertThrows(
                    StoreRegistrationException.class,
                    () -> manager.registerStore(sFullSwap, StorePolicy.FULL_SWAP)
            );

            Assertions.assertTrue(ex.getMessage().contains("policy"), ex.getMessage());
        }
    }

    @Test
    @DisplayName("Store Registration | Entity Directory Stores policies")
    public void testStoreRegistrationPolicyForEntityDirectoryStores() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore sPrivate   = randomDirStore(ScopedEntityA.class);
            FileStore sOverwrite = randomDirStore(ScopedEntityA.class);
            FileStore sFullSwap  = randomDirStore(ScopedEntityA.class);
            FileStore sDiscard   = randomDirStore(ScopedEntityA.class);

            Assertions.assertDoesNotThrow(() -> manager.registerStore(sPrivate, StorePolicy.PRIVATE));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sOverwrite, StorePolicy.OVERWRITE));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sFullSwap, StorePolicy.FULL_SWAP));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sDiscard, StorePolicy.DISCARD));
        }
    }

    @Test
    @DisplayName("Store Registration | Entity File Stores policies")
    public void testStoreRegistrationPolicyForEntityFileStores() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore sPrivate   = randomFileStore(ScopedEntityA.class);
            FileStore sOverwrite = randomFileStore(ScopedEntityA.class);
            FileStore sFullSwap  = randomFileStore(ScopedEntityA.class);
            FileStore sDiscard   = randomFileStore(ScopedEntityA.class);

            Assertions.assertDoesNotThrow(() -> manager.registerStore(sPrivate, StorePolicy.PRIVATE));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sOverwrite, StorePolicy.OVERWRITE));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sFullSwap, StorePolicy.FULL_SWAP));
            Assertions.assertDoesNotThrow(() -> manager.registerStore(sDiscard, StorePolicy.DISCARD));
        }
    }

    @Test
    @DisplayName("Access Scope | Equality")
    public void testAccessScopeEquality() {

        FileStore storeA = randomFileStore(ScopedEntityA.class);
        FileStore storeB = randomFileStore(ScopedEntityB.class);

        ScopedEntity entityA1 = new ScopedEntityA("1");
        ScopedEntity entityA2 = new ScopedEntityA("2");
        ScopedEntity entityB1 = new ScopedEntityB("1");

        AccessScope scopeA1 = new AccessScope(storeA, entityA1);
        AccessScope scopeA2 = new AccessScope(storeA, entityA2);
        AccessScope scopeB1 = new AccessScope(storeB, entityB1);

        AccessScope scopeDupeA1 = new AccessScope(storeA, entityA1);
        AccessScope scopeDupeA2 = new AccessScope(storeA, entityA2);
        AccessScope scopeDupeB1 = new AccessScope(storeB, entityB1);

        Assertions.assertEquals(scopeA1, scopeDupeA1);
        Assertions.assertNotEquals(scopeA1, scopeDupeA2);
        Assertions.assertNotEquals(scopeA1, scopeDupeB1);

        Assertions.assertNotEquals(scopeA2, scopeDupeA1);
        Assertions.assertEquals(scopeA2, scopeDupeA2);
        Assertions.assertNotEquals(scopeA2, scopeDupeB1);

        Assertions.assertNotEquals(scopeB1, scopeDupeA1);
        Assertions.assertNotEquals(scopeB1, scopeDupeA2);
        Assertions.assertEquals(scopeB1, scopeDupeB1);
    }

    @Test
    @DisplayName("Access Scope | Creation")
    public void testAccessScopeCreation() {

        FileStore rawStore    = randomRaw();
        FileStore scopedStore = randomDirStore(ScopedEntityA.class);

        ScopedEntity entityA1 = new ScopedEntityA("1");
        ScopedEntity entityB1 = new ScopedEntityB("1");

        ScopeDefinitionException ex;

        ex = Assertions.assertThrows(
                ScopeDefinitionException.class,
                () -> new AccessScope(rawStore, entityA1)
        );

        Assertions.assertTrue(ex.getMessage().contains("non-scoped"), ex.getMessage());

        ex = Assertions.assertThrows(
                ScopeDefinitionException.class,
                () -> new AccessScope(scopedStore, entityB1)
        );

        Assertions.assertTrue(ex.getMessage().contains("non compatible scoped entity type"), ex.getMessage());
    }

    @Test
    @DisplayName("Library Stores | Store Out Of Bounds")
    public void testStoreOutOfBounds() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore outOfBounds = new RawStorage("../out-of-bounds");

            StoreRegistrationException ex = Assertions.assertThrows(
                    StoreRegistrationException.class,
                    () -> manager.registerStore(outOfBounds, StorePolicy.PRIVATE)
            );

            ResolveOutOfBoundException roob = Assertions.assertInstanceOf(ResolveOutOfBoundException.class, ex.getCause());
            Assertions.assertTrue(roob.getMessage().contains("out-of-bound"), roob.getMessage());
        }
    }

    @Test
    @DisplayName("Library Stores | Store directory creation failure")
    public void testStoreCreationOnFile() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            FileStore store = randomRaw();

            File lock = new File(TEST_LIBRARY_PATH.toFile(), store.name());
            Assertions.assertDoesNotThrow(lock::createNewFile, "fuck, this shit is not even part of the test");

            StoreRegistrationException sre = Assertions.assertThrows(
                    StoreRegistrationException.class,
                    () -> manager.registerStore(store, StorePolicy.PRIVATE)
            );

            Assertions.assertTrue(sre.getMessage().contains("could not be obtain"), sre.getMessage());
            StorageException se = Assertions.assertInstanceOf(StorageException.class, sre.getCause());
            Assertions.assertTrue(se.getMessage().contains("Directory was expected"), se.getMessage());
        }
    }

    @Test
    @DisplayName("Isolation Context | Create with no scope")
    public void testIsolationCreationNoScope() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            IsolationSession context = Assertions.assertDoesNotThrow(() -> manager.createIsolation());

            try (context) {
                Path contextPath = TEST_LIBRARY_PATH.resolve("isolation").resolve(context.name());
                Assertions.assertTrue(Files.exists(contextPath), contextPath.toString());
            }
        }
    }

    @Test
    @DisplayName("Isolation Context | Create with a scope")
    public void testIsolationCreationValidScope() throws Exception {

        ScopedEntityA entity = new ScopedEntityA("1");
        FileStore     store  = randomFileStore(ScopedEntityA.class);
        AccessScope   scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);
            IsolationSession context = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scope));

            try (context) {
                Path contextPath = TEST_LIBRARY_PATH.resolve("isolation").resolve(context.name());
                Assertions.assertTrue(Files.exists(contextPath), contextPath.toString());
            }
        }
    }

    @Test
    @DisplayName("Isolation Context | Create with a scope in use")
    public void testIsolationCreationScopeClash() throws Exception {

        ScopedEntityA entity = new ScopedEntityA("1");
        FileStore     store  = randomFileStore(ScopedEntityA.class);
        AccessScope   scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);
            IsolationSession context = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scope));

            try (context) {
                ScopeGrantException ex = Assertions.assertThrows(ScopeGrantException.class, () -> manager.createIsolation(scope));
                Assertions.assertTrue(ex.getMessage().contains("scope is already claimed"), ex.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Isolation Context | Request used scope")
    public void testIsolationRequestUsedScope() throws Exception {

        FileStore     store   = randomFileStore(ScopedEntityA.class);
        ScopedEntityA entityA = new ScopedEntityA("A");
        ScopedEntityA entityB = new ScopedEntityA("B");
        AccessScope   scopeA  = new AccessScope(store, entityA);
        AccessScope   scopeB  = new AccessScope(store, entityB);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            IsolationSession contextA = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scopeA));
            IsolationSession contextB = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scopeB));

            ScopeGrantException ex;

            ex = Assertions.assertThrows(ScopeGrantException.class, () -> contextA.requestScope(scopeB));
            Assertions.assertTrue(ex.getMessage().contains("is already claimed"), ex.getMessage());

            ex = Assertions.assertThrows(ScopeGrantException.class, () -> contextB.requestScope(scopeA));
            Assertions.assertTrue(ex.getMessage().contains("is already claimed"), ex.getMessage());

            contextA.close();
            contextB.close();
        }
    }

    @Test
    @DisplayName("Isolation Context | Request unused scope")
    public void testIsolationRequestUnusedScope() throws Exception {

        FileStore     store   = randomFileStore(ScopedEntityA.class);
        ScopedEntityA entityA = new ScopedEntityA("A");
        ScopedEntityA entityB = new ScopedEntityA("B");
        AccessScope   scopeA  = new AccessScope(store, entityA);
        AccessScope   scopeB  = new AccessScope(store, entityB);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            IsolationSession context = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scopeA));

            try (context) {
                Assertions.assertDoesNotThrow(() -> context.requestScope(scopeB));
            }
        }
    }

    @Test
    @DisplayName("Isolation Context | Request freed scope")
    public void testIsolationRequestFreedScope() throws Exception {

        FileStore     store   = randomFileStore(ScopedEntityA.class);
        ScopedEntityA entityA = new ScopedEntityA("A");
        ScopedEntityA entityB = new ScopedEntityA("B");
        AccessScope   scopeA  = new AccessScope(store, entityA);
        AccessScope   scopeB  = new AccessScope(store, entityB);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            IsolationSession contextA = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scopeA));
            IsolationSession contextB = Assertions.assertDoesNotThrow(() -> manager.createIsolation(scopeB));

            ScopeGrantException ex = Assertions.assertThrows(
                    ScopeGrantException.class,
                    () -> contextA.requestScope(scopeB)
            );

            Assertions.assertTrue(ex.getMessage().contains("is already claimed"), ex.getMessage());
            contextB.close();

            Assertions.assertDoesNotThrow(() -> contextA.requestScope(scopeB));
        }
    }

    @Test
    @DisplayName("Isolation Context | Create on unregistered store")
    public void testIsolationCreationStoreNotSupported() throws Exception {

        FileStore     store  = randomFileStore(ScopedEntityA.class);
        ScopedEntityA entity = new ScopedEntityA("1");
        AccessScope   scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {

            ScopeGrantException ex = Assertions.assertThrows(
                    ScopeGrantException.class,
                    () -> manager.createIsolation(scope)
            );
            Assertions.assertTrue(ex.getMessage().contains("is not registered in this library."), ex.getMessage());
        }
    }

    @Test
    @DisplayName("Isolation Context | Forbidden use after commit/discard")
    public void testIsolationUseAfterCommit() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            IsolationSession context = Assertions.assertDoesNotThrow(() -> manager.createIsolation());
            context.commit();

            ContextUnavailableException ex;

            ex = Assertions.assertThrows(ContextUnavailableException.class, context::commit);
            Assertions.assertTrue(ex.getMessage().contains("already been committed"), ex.getMessage());

            ex = Assertions.assertThrows(ContextUnavailableException.class, () -> context.requestTemporaryFile("txt"));
            Assertions.assertTrue(ex.getMessage().contains("already been committed"), ex.getMessage());

            context.close();

            ex = Assertions.assertThrows(ContextUnavailableException.class, context::commit);
            Assertions.assertTrue(ex.getMessage().contains("already been discarded"), ex.getMessage());

            ex = Assertions.assertThrows(ContextUnavailableException.class, () -> context.requestTemporaryFile("txt"));
            Assertions.assertTrue(ex.getMessage().contains("already been discarded"), ex.getMessage());
        }
    }

    @Test
    @DisplayName("Isolation Writing | Write to a temporary file")
    public void testIsolationWritingToTemporaryFile() throws Exception {

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            IsolationSession context = manager.createIsolation(Collections.emptySet());

            try (context) {
                Path temporary = Assertions.assertDoesNotThrow(() -> context.requestTemporaryFile("txt"));

                Assertions.assertDoesNotThrow(() -> Files.writeString(
                        temporary,
                        "Unit Test",
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                ));

                context.commit();
            }
        }
    }

    @Test
    @DisplayName("Isolation Resolution | EntityFileStore")
    public void testIsolationResolutionOnEntityFileStore() throws Exception {

        FileStore    store  = randomFileStore(ScopedEntityA.class);
        ScopedEntity entity = new ScopedEntityA("1");
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Assertions.assertThrows(StorageException.class, () -> context.resolve(scope, "unit.txt"));
                Assertions.assertDoesNotThrow(() -> context.resolve(scope));
            }
        }
    }

    @Test
    @DisplayName("Isolation Resolution | EntityDirectoryStore")
    public void testIsolationResolutionOnEntityDirectoryStore() throws Exception {

        FileStore    store  = randomDirStore(ScopedEntityA.class);
        ScopedEntity entity = new ScopedEntityA("1");
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Assertions.assertDoesNotThrow(() -> context.resolve(scope));
                Assertions.assertDoesNotThrow(() -> context.resolve(scope, "unit.txt"));
            }
        }
    }

    @Test
    @DisplayName("Isolation Writing | EntityFileStore")
    public void testIsolationWritingEntityFileStore() throws Exception {

        FileStore    store   = randomFileStore(ScopedEntityA.class);
        ScopedEntity entity  = new ScopedEntityA("1");
        AccessScope  scope   = new AccessScope(store, entity);
        String       content = "UnitTest";
        byte[]       bytes   = content.getBytes(StandardCharsets.UTF_8);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope);

                InputStream  is = new ByteArrayInputStream(bytes);
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Path output = manager.resolve(scope);
            Assertions.assertTrue(Files.isRegularFile(output), output.toString());
            String finalContent = Files.readString(output, StandardCharsets.UTF_8);
            Assertions.assertEquals(content, finalContent);
        }
    }

    @Test
    @DisplayName("Isolation Writing | EntityDirectoryStore")
    public void testIsolationWritingEntityDirectoryStore() throws Exception {

        FileStore    store   = randomDirStore(ScopedEntityA.class);
        ScopedEntity entity  = new ScopedEntityA("1");
        AccessScope  scope   = new AccessScope(store, entity);
        String       content = "UnitTest";
        byte[]       bytes   = content.getBytes(StandardCharsets.UTF_8);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope, "unit.txt");

                InputStream  is = new ByteArrayInputStream(bytes);
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Path output = manager.resolve(scope, "unit.txt");
            Assertions.assertTrue(Files.isRegularFile(output), output.toString());
            String finalContent = Files.readString(output, StandardCharsets.UTF_8);
            Assertions.assertEquals(content, finalContent);
        }
    }


    @Test
    @DisplayName("Store Policy | Directory Overwrite")
    public void testStorePolicyDirectoryOverwrite() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomDirStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            String startingContent = "unit-test-start";
            String endingContent   = "unit-test-end";
            String staticContent   = "unit-test-static";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path staticPath   = manager.resolve(scope, "static.txt");
            Path replacedPath = manager.resolve(scope, "replaced.txt");
            Files.writeString(staticPath, staticContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.writeString(replacedPath, startingContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope, "replaced.txt");

                InputStream  is = new ByteArrayInputStream(endingContent.getBytes(StandardCharsets.UTF_8));
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Assertions.assertTrue(Files.isRegularFile(staticPath), staticPath.toString());
            Assertions.assertTrue(Files.isRegularFile(replacedPath), replacedPath.toString());

            String finalStaticContent   = Files.readString(staticPath);
            String finalReplacedContent = Files.readString(replacedPath);

            Assertions.assertEquals(staticContent, finalStaticContent);
            Assertions.assertEquals(endingContent, finalReplacedContent);
        }
    }

    @Test
    @DisplayName("Store Policy | Directory Full Swap")
    public void testStorePolicyDirectoryFullSwap() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomDirStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.FULL_SWAP);

            String startingContent = "unit-test-start";
            String endingContent   = "unit-test-end";
            String staticContent   = "unit-test-static";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path staticPath   = manager.resolve(scope, "static.txt");
            Path replacedPath = manager.resolve(scope, "replaced.txt");
            Files.writeString(staticPath, staticContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.writeString(replacedPath, startingContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope, "replaced.txt");

                InputStream  is = new ByteArrayInputStream(endingContent.getBytes(StandardCharsets.UTF_8));
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Assertions.assertFalse(Files.isRegularFile(staticPath), staticPath.toString());
            Assertions.assertTrue(Files.isRegularFile(replacedPath), replacedPath.toString());

            String finalReplacedContent = Files.readString(replacedPath);

            Assertions.assertEquals(endingContent, finalReplacedContent);
        }
    }

    @Test
    @DisplayName("Store Policy | File Overwrite (Content)")
    public void testStorePolicyFileOverwriteContent() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomFileStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            String startingContent = "unit-test-start";
            String endingContent   = "unit-test-end";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path path = manager.resolve(scope);
            Files.writeString(path, startingContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope);

                InputStream  is = new ByteArrayInputStream(endingContent.getBytes(StandardCharsets.UTF_8));
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Assertions.assertTrue(Files.isRegularFile(path), path.toString());

            String finalReplacedContent = Files.readString(path);

            Assertions.assertEquals(endingContent, finalReplacedContent);
        }
    }

    @Test
    @DisplayName("Store Policy | File Overwrite (No Content)")
    public void testStorePolicyFileOverwriteNoContent() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomFileStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.OVERWRITE);

            String content = "unit-test";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path path = manager.resolve(scope);
            Files.writeString(path, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                context.commit();
            }

            Assertions.assertTrue(Files.isRegularFile(path), path.toString());

            String finalContent = Files.readString(path);

            Assertions.assertEquals(content, finalContent);
        }
    }

    @Test
    @DisplayName("Store Policy | File Full Swap (Content)")
    public void testStorePolicyFileFullSwapContent() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomFileStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.FULL_SWAP);

            String startingContent = "unit-test-start";
            String endingContent   = "unit-test-end";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path path = manager.resolve(scope);
            Files.writeString(path, startingContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                Path output = context.resolve(scope);

                InputStream  is = new ByteArrayInputStream(endingContent.getBytes(StandardCharsets.UTF_8));
                OutputStream os = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                try (is; os) {
                    is.transferTo(os);
                }

                context.commit();
            }

            Assertions.assertTrue(Files.isRegularFile(path), path.toString());

            String finalReplacedContent = Files.readString(path);

            Assertions.assertEquals(endingContent, finalReplacedContent);
        }
    }

    @Test
    @DisplayName("Store Policy | File Full Swap (No Content)")
    public void testStorePolicyFileFullSwapNoContent() throws Exception {

        ScopedEntity entity = new ScopedEntityA("1");
        FileStore    store  = randomFileStore(ScopedEntityA.class);
        AccessScope  scope  = new AccessScope(store, entity);

        try (Library manager = new Sanctum(TEST_LIBRARY_PATH)) {
            manager.registerStore(store, StorePolicy.FULL_SWAP);

            String content = "unit-test";

            // Please don't do the following in production code (it defeats isolation, very bad), only allowed during tests :)
            Path path = manager.resolve(scope);
            Files.writeString(path, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            try (IsolationSession context = manager.createIsolation(scope)) {
                context.commit();
            }

            Assertions.assertFalse(Files.isRegularFile(path), path.toString());
        }
    }

}
