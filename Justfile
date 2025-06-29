build:
	mvn compile

run *ARGS:
	java -cp target/classes ucu.slay.App {{ARGS}}
