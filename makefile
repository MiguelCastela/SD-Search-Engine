SRC_DIR = src/main/java
TARGET_DIR = target/classes
LIB_DIR = target/lib
CONFIG_FILE = config.properties

LOCAL_IP = $(shell hostname -I | cut -d' ' -f1)

compile:
	mvn install
	mvn compile -f pom.xml
	mkdir -p $(TARGET_DIR)
	find $(SRC_DIR) -name "*.class" -exec cp --parents {} $(TARGET_DIR) \;

clean:
	rm -rf target
	mvn clean

run_gateway:
	java -cp $(TARGET_DIR):$(LIB_DIR)/* -Djava.rmi.server.hostname=$(LOCAL_IP) search.Gateway $(CONFIG_FILE)
run_downloader:
	java -cp $(TARGET_DIR):$(LIB_DIR)/* -Djava.rmi.server.hostname=$(LOCAL_IP) search.Downloader $(CONFIG_FILE)
run_barrel:
	java -cp $(TARGET_DIR):$(LIB_DIR)/* -Djava.rmi.server.hostname=$(LOCAL_IP) search.Barrel $(CONFIG_FILE)
run_client:
	java -cp $(TARGET_DIR):$(LIB_DIR)/* -Djava.rmi.server.hostname=$(LOCAL_IP) search.Client $(CONFIG_FILE)

run_webserver:
	mvn clean package
	java -cp $(TARGET_DIR):$(LIB_DIR)/* -Djava.rmi.server.hostname=$(LOCAL_IP) search.Meta2Application