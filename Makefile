.PHONY: default
default:
	@echo "Targets: build, test, clean"

.PHONY: build
build:
	cd frontend && mvn package
	cd backend && npm install

.PHONY: test
test: build
	scripts/test

.PHONY: clean
clean:
	cd frontend && mvn clean
	cd backend && make clean
	rm -f README.html
	rm -rf scripts/__pycache__

README.html: README.md
	pandoc -o $@ $<
