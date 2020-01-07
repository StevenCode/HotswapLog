java -Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar \
     -jar HotswapLog-1.0-SNAPSHOT-jar-with-dependencies.jar \
     -pid 13038  \
     -className com.steven.springboothelloworld.controller.HelloController \
     -logEnable 1