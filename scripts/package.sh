JAR_FILE_PATH=$(find . -name "vanilla-*.jar")
JAR_FILE_NAME=$(basename "$JAR_FILE_PATH")
LIB="${JAR_FILE_PATH%/*}"
EXECUTABLE_NAME="${JAR_FILE_NAME%.*}"

echo "Building $JAR_FILE_NAME in $LIB to $EXECUTABLE_NAME.exe"

jpackage --input $LIB --main-jar $JAR_FILE_NAME --name $EXECUTABLE_NAME --type "app-image" --dest ./build
