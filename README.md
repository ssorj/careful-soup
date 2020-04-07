# Careful soup

Requires a broker listening on localhost:5672 with user "example", password "example".

    $ cd careful-soup/
    $ make test

The file `scripts/test` has the details of exercising the application.

## Demo script

1. Start a broker.

2. Start the all-in-one Jaeger container.

        podman run --rm --name jaeger -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 -p 5775:5775/udp -p 6831:6831/udp -p 6832:6832/udp -p 5778:5778 -p 16686:16686 -p 14268:14268 -p 14250:14250 -p 9411:9411 jaegertracing/all-in-one:1.17

3. Start the frontend servce.

        HTTP_HOST=localhost HTTP_PORT=8081 java -jar frontends/java/target/careful-soup-frontend-1.0.0-SNAPSHOT-jar-with-dependencies.jar &

4. Start the backend service.

        python3 backends/python/app.py &

5. Send a request.

        curl -fX POST -d "hello 1" -H "Content-type: text/plain" http://localhost:8081/api/send-request
