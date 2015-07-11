# continuation-local-storage-jvm

This is a common implementation of continuation-local storage for the JVM.
It is based on Twitter's com.twitter.util.Local, but this is a Java port
without any external dependencies (notably; no dependency on Scala).

The ContinuationLocal can be used for any data that should be persisted in
a continuation scope, but is especially useful for various monitoring and tracing
implementations.

## Usage example

```java
import com.github.distributivetracing.ContinuationLocal

public final class MyProgram {
    private static final ContinuationLocal<Integer> userId
      = new ContinuationLocal<Integer>();

    public static void main(String[] args) {
        userId.set(123);
        printUserId();
    }

    private static void printUserId() {
        System.out.println(userId.get());
    }
}
```

## Design differences between com.twitter.util.Local and ContinuationLocal:

- The value of `com.twitter.util.Local<T>` is of type `scala.Option<T>`,
  where `Some(value)` holds the value, while `None` indicates that the
  value is absent. In `com.github.distributivetracing.ContinuationLocal<T>`,
  the value is not an Option, instead being a nullable of type `T`.
- In `com.twitter.util.Local` the inner storage mechanism is a `ThreadLocal`,
  while in `ContinuationLocal`, it is a `InheritableThreadLocal`. This means
  that a value that would not be passed on in `com.twitter.util.Local` will be
  in a `ContinuationLocal`, in case a new thread is started in the same
  context as the `ContinuationLocal` was set.
- `com.twitter.util.Local`'s `update` method is not included. The similar
  method `set` should instead be used. `update` in `c.t.u.Local` uses `Option`
  types, while `set` in `ContinuationLocal` deals with nullable values directly.
- `com.twitter.util.Local`'s (static) `letClear` function has been renamed to
  `letAllClear` to avoid a naming collision with the non-static method `letClear`.
