Build RXTX from sources:

```bash
wget http://rxtx.qbang.org/pub/rxtx/rxtx-2.2pre2.zip
unzip rxtx-2.2pre2.zip
pushd rxtx-2.2pre2
mkdir -p build/{classes,natives}
javac -d build/classes -h build/natives src/gnu/**/*.java
jar cf build/rxtx-2.2pre2-sources.jar -C src gnu
jar cf build/rxtx-2.2pre2.jar -C build/classes .
popd
```
