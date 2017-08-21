# NMSProtocol
Lib for packet manipulation for Minecraft.
Uses NMS with a little Reflection, so this lib is update-friendly and should work for a wide range of versions.

## Reload-friendly
You can use the /reload with this plugin.

## WIP
Performances not tested, althrough I used very little Reflection which will be only called on server startup.
Per-connection protocol injection is done only with direct calls.

## Repo
https://bintray.com/skybeastmc/maven/nmsprotocol

Gradle:
```groovy
compile 'me.skybeast.nmsprotocol:nmsprotocol:1.0.0'
```
Maven:
```xml
<dependency>
  <groupId>me.skybeast.nmsprotocol</groupId>
  <artifactId>nmsprotocol</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
Ivy:
```xml
<dependency org='me.skybeast.nmsprotocol' name='nmsprotocol' rev='1.0.0'>
  <artifact name='nmsprotocol' ext='pom' ></artifact>
</dependency>
```