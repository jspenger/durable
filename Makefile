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

.PHONY: build

build:
	sbt -v compile
	sbt -v test:compile

.PHONY: test

test:
	sbt -v test

.PHONY: doc

doc:
	sbt -v doc

.PHONY: example-runAll

EXAMPLE_MAIN_PATHS = durable.example.Fibonacci \
	durable.example.HelloWorld \
	durable.example.PingPong \
	durable.example.Random

example-runAll:
	set -e; \
	for class in $(EXAMPLE_MAIN_PATHS); do \
		sbt -v "example/runMain $$class"; \
	done

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
	mkdir -p .sandbox
	rsync -a . .sandbox --exclude='.sandbox'
	cd .sandbox \
		&& make clean \
		&& make build \
		&& make test \
		&& make example-runAll \
		&& make scalafmtCheck
ifdef arg
		cd .sandbox && $(arg)
endif
