# Building
Pre-requisites
-------------

* Java 8 or above [[Windows](https://adoptopenjdk.net/), [Ubuntu](https://help.ubuntu.com/community/Java), [CentOS](https://stackoverflow.com/questions/20901442/how-to-install-jdk-in-centos/20901970#20901970), [OS X](https://adoptopenjdk.net/)]

Building JAR File
--------------------

1. `git clone https://github.com/Cloudburst/Server`
2. `cd Server`
3. `git submodule update --init`
4. `./mvnw clean package`

The compiled JAR can be found in the `target/` directory.
