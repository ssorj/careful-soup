.PHONY: build-image
build-image:
	podman build -t careful-soup-frontend .

.PHONY: run-image
run-image: build-image
	podman run --rm -p 8080:8080 careful-soup-frontend
