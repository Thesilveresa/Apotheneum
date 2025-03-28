#!/bin/sh
cd `dirname $0`
if [ ! -d ~/Chromatik ]; then
  echo "Chromatik does not appear to be installed, run Chromatik first before continuing."
  exit 1
fi
if [ ! -d ~/Chromatik/Fixtures/Apotheneum ]; then
  echo "Creating ~/Chromatik/Fixtures/Apotheneum"
  mkdir -p ~/Chromatik/Fixtures/Apotheneum
fi
if [ ! -d ~/Chromatik/Projects/Apotheneum ]; then
  echo "Creating ~/Chromatik/Projects/Apotheneum"
  mkdir -p ~/Chromatik/Projects/Apotheneum
fi
cp src/main/resources/fixtures/* ~/Chromatik/Fixtures/Apotheneum/
cp src/main/resources/projects/* ~/Chromatik/Projects/Apotheneum/
echo "Apotheneum assets copied, installing project"
cp bundle/apotheneum-0.0.1-SNAPSHOT.jar ~/Chromatik/Packages/
echo "Apotheneum is now ready to run in Chromatik"

