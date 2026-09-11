# Sanctum

Sanctum is a Java library for organizing filesystem-backed application data and staging changes before they reach the main library.

It is designed for applications that delegate file processing to workers or external services. Each task writes into an isolated session, receives access only to explicitly claimed entity scopes, and can then commit all staged changes or discard them.

> Sanctum manages paths and filesystem operations. It does not validate file contents and is not a security sandbox.

## Requirements

- Java 25
- The included Gradle wrapper

Run the test suite with:

```shell
./gradlew test
```

## Core concepts

A library contains named file stores. Three store shapes are available:

- `RawStorage`: an unstructured directory.
- `ScopedFileStorage`: one file per entity key, with an enforced extension.
- `ScopedDirectoryStorage`: one directory per entity key, with unrestricted contents inside it.

Each store also has a policy controlling its behavior in isolated sessions:

| Policy | Isolated access | Commit behavior |
| --- | --- | --- |
| `PRIVATE` | Denied | Managed directly by the library owner |
| `OVERWRITE` | Requires a scope | Adds or replaces staged files while retaining other files |
| `FULL_SWAP` | Requires a scope | Completely replaces the scoped file or directory |
| `DISCARD` | Implicitly allowed | Session-local data is deleted instead of committed |

The library automatically creates private `isolation` storage and discardable `tmp` storage.

## Example

```java
public final class MediaLibrary extends Sanctum {

    public static final FileStore THUMBNAILS =
            new ScopedFileStorage("thumbnails", Video.class, "jpg");
    public static final FileStore CONTENT =
            new ScopedDirectoryStorage("content", Video.class);

    public MediaLibrary(Path root) {
        super(root);
        registerStore(THUMBNAILS, StorePolicy.OVERWRITE);
        registerStore(CONTENT, StorePolicy.FULL_SWAP);
    }
}
```

Stage and commit a thumbnail using the video's stable identifier as its scope claim:

```java
AccessScope scope = new AccessScope(MediaLibrary.THUMBNAILS, video.getScopedName());

try (IsolationSession session = library.createIsolation(scope)) {
    Files.copy(input, session.resolve(scope), StandardCopyOption.REPLACE_EXISTING);
    session.commit();
}
```

If the operation exits before `commit()`, closing the session discards its files and releases its scopes. A committed session must also be closed, which removes its staging directory and releases its claims.

More detailed explanations and examples are available in the [wiki](https://github.com/anisekai-project/sanctum/wiki).

## Contributing

Before submitting a pull request, open an issue describing the proposed bug fix or feature and wait for approval. This keeps contributions aligned with the project's scope.

## License

Sanctum is licensed under the [Apache License 2.0](LICENSE).
