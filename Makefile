JAVA_HOME := $(HOME)/share/jdk-21.0.4+7
PATH := $(JAVA_HOME)/bin:$(PATH)

install: bin/g

bin/g: make_jar
	mkdir -p bin
	cp src/main/sh/* target/gh-*.jar \
$(HOME)/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.18.0/jackson-databind-2.18.0.jar \
$(HOME)/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.18.0/jackson-core-2.18.0.jar \
$(HOME)/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.0/jackson-annotations-2.18.0.jar \
	bin

make_jar:
	mvn install

clean:
	mvn clean
	rm -rf bin

re: clean install
