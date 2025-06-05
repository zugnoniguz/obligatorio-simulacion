build:
	mvn compile

run *ARGS: build
	java -cp target/classes ucu.slay.App {{ARGS}}
