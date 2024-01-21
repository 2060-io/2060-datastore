# 2060-datastore

A simple datastore for PoCs

Customizable opts:

io.twentysixty.datastore.debug=true
io.twentysixty.datastore.tmp.dir=/tmp/tmp
io.twentysixty.datastore.tmp.lifetimedays=5
io.twentysixty.datastore.repo.lifetimedays=90
io.twentysixty.datastore.repo.fs.dir=/tmp/repo
io.twentysixty.datastore.purge.intervalms=86400000
io.twentysixty.datastore.media.maxchunks=128

you can deploy several instances of 2060-data-store but they must share the same read/write partition for both tmp.dir and repo.fs.dir

you can set same path for both tmp.dir and repo.fs.dir

API documentation: Run the application and browse:

http://localhost:8080/q/swagger-ui/

Manual chunk upload example:

curl -v -v -F chunk=@chunk3.bin.part  http://localhost:8080/u/430fc295-a1f2-4810-817a-eecdab57cf4a/3

*Note*: for security reasons, if shared data *is not ciphered*, for a given file, target audience should receive a hash of the file through p2p connection, download the file, calculate its hash, and render the file only if hash matches.

*Note*: you can optionally use a management token when creating a file. By using a management token you will be able to later update or delete the file.

## How to create a file (no token):

1. create the media by specifying its uuid and number of chunks /c/{uuid}/{noc}
2. upload the chunks /u/{uuid}/{c}
3. media is available /r/{uuid}

files created without a management token are not modifiable nor deletable.

## How to create/update/delete a file (with token):

Create

1. create the media by specifying its uuid and number of chunks and management token /c/{uuid}/{noc}?{token}
2. upload the chunks /u/{uuid}/{c}?{token}
3. media is available for download /r/{uuid}

Update

1. prepare chunk upload for the media by specifying its uuid and number of chunks and same management token /c/{uuid}/{noc}?{token}
2. upload the chunks /u/{uuid}/{c}?{token}
3. media is available for download /r/{uuid}

Delete
1. call /d/{uuid}?{token}


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/2060-data-store-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

