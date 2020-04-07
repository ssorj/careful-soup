.PHONY: default
default:
	@echo "Targets: build, test, clean"

.PHONY: build
build:
	cd frontends/java && mvn package
	cd backends/javascript && npm install

.PHONY: test
test:
	scripts/test

.PHONY: clean
clean:
	cd frontends/java && mvn clean
	cd backends/javascript && make clean
	rm -f README.html
	rm -rf scripts/__pycache__

README.html: README.md
	pandoc -o $@ $<
