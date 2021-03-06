package(default_visibility = ["//src:__subpackages__"])

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
    visibility = ["//src/test/shell/bazel:__pkg__"],
)

java_library(
    name = "libSingleJar",
    srcs = glob(["java/**/singlejar/**/*.java"]),
    deps = [
        ":zip",
        "//src/main/java:shell",
        "//third_party:jsr305",
    ],
)

java_binary(
    name = "SingleJar",
    main_class = "com.google.devtools.build.singlejar.SingleJar",
    runtime_deps = [":libSingleJar"],
)

java_test(
    name = "tests",
    srcs = glob(["javatests/**/singlejar/**/*.java"]),
    args = ["com.google.devtools.build.singlejar.SingleJarTests"],
    deps = [
        ":libSingleJar",
        ":zip",
        "//src/main/java:shell",
        "//src/test/java:testutil",
        "//third_party:guava",
        "//third_party:jsr305",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_library(
    name = "zip",
    srcs = glob(["java/**/zip/**/*.java"]),
    deps = [
        "//third_party:jsr305",
    ],
)

java_test(
    name = "zipTests",
    size = "large",
    srcs = glob(["javatests/**/zip/**/*.java"]),
    args = ["com.google.devtools.build.zip.ZipTests"],
    tags = ["zip"],
    deps = [
        ":zip",
        "//src/test/java:testutil",
        "//third_party:guava",
        "//third_party:guava-testlib",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

#
# Bootstrapping using Skylark rules
#
load("/tools/build_rules/java_rules_skylark", "bootstrap_java_library", "bootstrap_java_binary")

bootstrap_java_library(
    name = "skylark-deps",
    jars = [
        "//third_party:jsr305-jars",
    ],
)

bootstrap_java_binary(
    name = "bootstrap",
    srcs = glob(["java/**/*.java"]),
    main_class = "com.google.devtools.build.singlejar.SingleJar",
    visibility = ["//visibility:public"],
    deps = [
        ":skylark-deps",
        "//src/main/java:shell-skylark",
    ],
)
