# Advanced ZIO

Thousands of developers around the world have turned to the open-source ZIO library to build modern applications that are highly scalable and resilient. Yet, while developers may learn the fundamentals of ZIO in books, videos, and articles, there is little material available for ZIO developers who wish to take their skills and their applications to the next level.
In this course, developers will explore the edges and corners of the ZIO library, taking a close look at the fine-grained interruption, custom concurrency structures, streams, application configuring and tuning, and metrics and monitoring.

### Who Should Attend

ZIO developers who wish to expand their knowledge of building, deploying, and troubleshooting ZIO applications.

### Prerequisites

Working knowledge of ZIO is required.

### Topics

 - Functional testing
 - Fine-grained interruption
 - Custom concurrency structures
 - Streams, sinks, and pipelines
 - Stream concurrency & timeouts
 - Streams fan-in, fan-out, grouping, & aggregation
 - Configuration & tuning
 - Metrics & Monitoring
 - Performance optimization

# Usage

## From the UI

1. Download the repository as a [zip archive](https://github.com/jdegoes/advanced-zio/archive/master.zip).
2. Unzip the archive, usually by double-clicking on the file.
3. Configure the source code files in the IDE or text editor of your choice.

## From the Command Line

1. Open up a terminal window.

2. Clone the repository.

    ```bash
    git clone https://github.com/jdegoes/advanced-zio
    ```
5. Launch project provided `sbt`.

    ```bash
    cd advanced-zio; ./sbt
    ```
6. Enter continuous compilation mode.

    ```bash
    sbt:advanced-zio> ~ test:compile
    ```

Hint: You might get the following error when starting sbt:

> [error] 	typesafe-ivy-releases: unable to get resource for com.geirsson#sbt-scalafmt;1.6.0-RC4: res=https://repo.typesafe.com/typesafe/ivy-releases/com.geirsson/sbt-scalafmt/1.6.0-RC4/jars/sbt-scalafmt.jar: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested targe

It's because you have an outdated Java version, missing some newer certificates. Install a newer Java version, e.g. using [Jabba](https://github.com/shyiko/jabba), a Java version manager. See [Stackoverflow](https://stackoverflow.com/a/58669704/1885392) for more details about the error.

# Legal

Copyright&copy; 2019-2021 John A. De Goes. All rights reserved.
