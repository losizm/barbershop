# Barbershop

**Barbershop** is an example application using [Scamper](https://github.com/losizm/scamper/)
as the web framework. It features a REST API along with a single-page
application to manage arbitrary comments.

## Getting Started

To get started, clone the Git repository:

```
git clone 'https://github.com/losizm/barbershop'
```

Also ensure the following are installed locally with their bin directories on
the executable path:

* Java JDK 8 or higher; see [Adoptium](https://adoptium.net) for prebuilt OpenJDK binaries
* The [sbt](https://scala-sbt.org) build tool

## Building Application

The project uses the [sbt-native-packager](https://www.scala-sbt.org/sbt-native-packager)
plugin.

To build the application package, run the following command in the project directory:

```sh
sbt packageZipTarball
```

The command builds a compressed tarball at `[PROJECT_DIR]/target/universal/barbershop-[RELEASE].tgz`.

## Installing Application

To install, simply untar the application package to a location of your choosing:

```sh
tar -C [INSTALL_DIR] -xvf [PROJECT_DIR]/target/universal/barbershop-[RELEASE].tgz
```

## Configuring Application

The configuration file is located at `[INSTALL_DIR]/conf/application.conf`. This
file supplies only a subset of configuration, which includes some basic settings
as shown below.

```conf
barbershop.server.host = 0.0.0.0
barbershop.server.port = 9999
barbershop.server.key = null
barbershop.server.certificate = null

include "overrides.conf"
```

The file can be edited to include more settings. Alternatively, another file
named `overrides.conf` can be created in the same directory to supply additional
configuration.

See [reference.conf](src/main/resources/reference.conf) for available
configuration along with their default settings.

## Running Application

The startup script is located at `[INSTALL_DIR]/bin/barbershop`.

No command line arguments are required; however, to start the application while
also overriding configuration, you can supply any number of the key/value pairs
as system properties:

```sh
[INSTALL_DIR]/bin/barbershop -D'barbershop.server.host=localhost' -D'barbershop.server.port=8080'
```

A successful startup prints something like the following to the terminal:

```log
[2021-12-23T09:36:48.445-05:00][INFO] barbershop.web.Server - localhost:8080 - Starting server
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Secure: false
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Logger: Logger("barbershop.web.Server")
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Backlog Size: 20
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Pool Size: 4
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Queue Size: 16
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Buffer Size: 8192
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Read Timeout: 250
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Header Limit: 20
[2021-12-23T09:36:48.447-05:00][INFO] barbershop.web.Server - localhost:8080 - Keep-Alive: disabled
[2021-12-23T09:36:48.449-05:00][INFO] barbershop.web.Server - localhost:8080 - Server is up and running
```

When the application is successfully started, a PID file is created at
`[INSTALL_DIR]/barbershop.pid`.

### The REST API

You can interact with the REST API using command line tools, such as curl.

**To create a comment:**

```sh
curl -i -X POST \
  -H 'Content-Type: text/plain' \
  -d 'Hello, barbershop!' \
  http://localhost:8080/api/comments
```

The newly created comment's URI is returned in the **Location** header.

```
HTTP/1.1 201 Created
Location: /api/comments/1
Content-Length: 0
Date: Thu, 23 Dec 2021 23:45:23 GMT
Connection: close
````

**To create a comment with attachments:**

```sh
# Send comment and attachments as multipart/form-data
curl -i -X POST \
  -F text='Here are some more cat photos.' \
  -F 'attachment=@"cat1.jpg"; type=image/jpeg; filename=cat1.jpg' \
  -F 'attachment=@"cat2.jpg"; type=image/jpeg; filename=cat2.jpg' \
  -F 'attachment=@"cat3.jpg"; type=image/jpeg; filename=cat3.jpg' \
  http://localhost:8080/api/comments
```

**To read a comment:**
```sh
curl http://localhost:8080/api/comments/2
```

The response body is a JSON object. If a comment has attachments, the attachments
are described and an identifier is specified for each, which can be used to
download the attachment.

```json
{
  "id": 2,
  "text": "Here are some more cat photos.",
  "attachments": [
    {
      "id": 3,
      "name": "cat1.jpg",
      "kind": "image/jpeg",
      "size": 11050
    },
    {
      "id": 4,
      "name": "cat2.jpg",
      "kind": "image/jpeg",
      "size": 12023
    },
    {
      "id": 5,
      "name": "cat3.jpg",
      "kind": "image/jpeg",
      "size": 3865
    }
  ],
  "time": "2021-12-23T23:45:23.187Z"
}
```

**To download an attachment:**

```sh
# Redirect downloaded attachment to file
curl -s http://localhost:8080/api/attachments/3 > cat.jpg
```

**To read multiple comments:**
```sh
# Limit output to 10 comments
curl http://localhost:8080/api/comments?limit=10
```

The response body is a JSON array of comments.

The following query parameters may be supplied in URL to filter comments:

*  `minId` &ndash; lower bound of comment identifier
*  `maxId` &ndash; upper bound of comment identifier
*  `minTime`<sup>&dagger;</sup> &ndash; lower bound of comment time
*  `maxTime`<sup>&dagger;</sup> &ndash; upper bound of comment time
*  `offset` &ndash; number of leading comments to drop
*  `limit` &ndash; maximum number of comments to list

<small>&dagger; Supplied as Epoch milliseconds or timestamp formatted as `yyyy-MM-ddTmm:hh:ssZ`</small>

**To update a comment:**
```sh
curl -X PUT \
  -H 'Content-Type: text/plain' \
  -d 'HELLO, BARBERSHOP!' \
  http://localhost:8080/api/comments/1
```

**To delete a comment:**
```sh
curl -X DELETE http://localhost:8080/api/comments/1
```

### The Single-page application

For a more visual experience, point your browser to
[http://localhost:8080/ui/comments/index.html](http://localhost:8080/ui/comments/index.html).

<div>
  <img style="padding: 0.2em;" src="images/ui-screenshot.png" width="560"/>
</div>

Enter a comment and watch it appear in the list. Click on a comment to delete
it, or click on an attachment to download it.

## Stopping Application

The PID file can be used to stop the application as follows:

```sh
kill "$(< [INSTALL_DIR]/barbershop.pid)"
```

Or, simply press `Ctrl-C` in the terminal in which the application is running.

## Container Images

There are container images available.

__To pull the latest image:__

```sh
docker pull ghcr.io/losizm/barbershop
```

_**Note:** The container image described above is located at the GitHub container
registry. See also [Barbershop on Docker Hub](https://hub.docker.com/r/losizm/barbershop)
for container images._

__To run the container:__

```sh
docker run \
  --name barbershop \
  -it \
  -p 8080:8080 \
  ghcr.io/losizm/barbershop -D'barbershop.server.port=8080'
```

## License

**Barbershop** is licensed under the Apache License, Version 2. See [LICENSE](LICENSE)
for more information.
