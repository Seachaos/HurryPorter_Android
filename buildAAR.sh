./gradlew assembleRelease
cp hurryporter/build/outputs/aar/hurryporter-release.aar ./hurryporter.aar
./gradlew bintrayUpload