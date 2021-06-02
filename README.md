# kached-properties

## About

This library approaches a problem of time-bound values caching by exposing a simple API using Kotlin properties. You can
simply say *"cache the result of a function for 5 minutes"* by writing:

```kotlin
val activeSatellites by cachedLazyFor(Duration.ofMinutes(5), {
    fetchActiveSatellites()
})
```

There are two delegated property builders available, depending on your business needs:

* `cachedLazyFor` - once the cached value expires, a request for value returns last known value and starts updating it
  in the background. If needed, you can customize `java.util.concurrent.Executor` used to refresh the value, which
  defaults to `Executors.newCachedThreadPool()`
* `cachedBlockingFor` - once the cached value expires, a request for value first updates the value in a blocking way,
  and then returns it

## Installation

The library is published to Maven Central. An example of consuming it from Gradle using Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    ...
    implementation("com.dynatrace:kached-properties:1.0.0")
}
```

## Contributing

Reporting an issue, proposing a feature, submitting a pull requests - all of these are very welcome!

Before making a contribution, please familiarize yourself with [the Code of Conduct](CODE_OF_CONDUCT.md)

## License

Distributed under Apache License Version 2.0. See [LICENSE](LICENSE) for details.
