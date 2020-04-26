# Virgo Build Tools

> Note: This document is an addition to [Building Virgo Build](https://wiki.eclipse.org/Virgo/Build#Building_virgo-build-tools).

## Building `virgo-build-tools` for the impatient

Put [Apache Ant](https://ant.apache.org/) on the class path (`export PATH=$PATH:$ANT_HOME/bin`)

```bash
ant -f build-virgo-build/build.xml clean package
...
      [zip] Building zip: /Users/virgo/scm-repos/virgo-build-tools/build-virgo-build/target/artifacts/virgo-build-tools-1.5.0.RELEASE.zip

BUILD SUCCESSFUL
```

## Prepare a release build

> Note: At the time of writing we don't have a CI build anymore!

Visit `build.properties` and

* choose proper `version`,
* swich `release.type` to `release` and
* add `build.stamp=RELEASE`:

```properties
version=1.5.0
release.type=release
build.stamp=RELEASE
...
```

Version `1.5.0.RELEASE` has bee uploaded manually!
