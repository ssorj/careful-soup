.PHONY: build-image
build-image:
	podman build -t careful-soup-backend .

.PHONY: run-image
run-image: build-image
	podman run --rm -p 8080:8080 careful-soup-backend

.PHONY: clean
clean:
	rm -rf node_modules
	rm -f npm-debug.log
