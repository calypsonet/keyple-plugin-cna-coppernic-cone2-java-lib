include(":example-app")
include(":coppernic-plugin")
include(":coppernic-plugin-mock")
rootProject.name = "keyple-plugin-cna-coppernic-cone2-java-lib"

// Fix resolution of dependencies with dynamic version in order to use SNAPSHOT first when available.
// See explanation here : https://docs.gradle.org/6.8.3/userguide/single_versions.html
enableFeaturePreview("VERSION_ORDERING_V2")