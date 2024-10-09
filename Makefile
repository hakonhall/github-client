JAVA_HOME := $(HOME)/share/jdk-21.0.4+7
PATH := $(JAVA_HOME)/bin:$(PATH)

install:
	mvn install

clean:
	mvn clean

re: clean install
