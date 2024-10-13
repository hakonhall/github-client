JAVA_HOME := $(HOME)/share/jdk-21.0.4+7
PATH := $(JAVA_HOME)/bin:$(PATH)

install: bin/g

bin/g: make_jar
	mkdir -p bin
	cp src/main/sh/* target/gh-*.jar bin

make_jar:
	mvn install

clean:
	mvn clean
	rm -rf bin

re: clean install
