.PHONY: build

build:
	sbt -v compile
	sbt -v test:compile

.PHONY: clean

clean:
	find . -name .DS_Store | xargs rm -fr
	find . -name .metals | xargs rm -fr
	find . -name .vscode | xargs rm -fr
	find . -name .idea | xargs rm -fr
	find . -name .bsp | xargs rm -fr
	find . -name .bloop | xargs rm -fr
	find . -name "*.json" | xargs rm -fr
	find . -name metals.sbt | xargs rm -fr
	rm -fr project/project/
	find . -name target | xargs rm -fr
	rm -fr .sandbox/

.PHONY: clean-json

clean-json:
	find . -name "*.json" | xargs rm -fr

.PHONY: test

test:
	sbt -v test

.PHONY: doc

doc:
	sbt -v doc

.PHONY: example-runAll

example-runAll:
	sbt -v "example/runMain durable.example.Fibonacci"
	sbt -v "example/runMain durable.example.HelloWorld"
	sbt -v "example/runMain durable.example.PingPong"
	sbt -v "example/runMain durable.example.Random"

.PHONY: scalafmt

scalafmt:
	sbt -v scalafmtAll
	sbt -v scalafmtSbt

.PHONY: scalafmtCheck

scalafmtCheck:
	sbt -v scalafmtCheckAll
	sbt -v scalafmtSbtCheck

.PHONY: sandbox

sandbox:
	rm -rf .sandbox
	rsync -a . .sandbox --exclude='.sandbox'
	cd .sandbox && $(MAKE) clean
	cd .sandbox && $(MAKE) build
	cd .sandbox && $(MAKE) test
	cd .sandbox && $(MAKE) example-runAll
	cd .sandbox && $(MAKE) scalafmtCheck
