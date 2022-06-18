# Careful soup

## Dependencies

 - sudo dnf install python3-opentracing
 - pip3 install --user jaeger_client

## Running a broker

    podman run --rm --name broker -p 5672:5672 -e AMQ_USER=example -e AMQ_PASSWORD=example quay.io/artemiscloud/activemq-artemis-broker

## Running the test script

Requires a broker listening on localhost:5672 with user "example", password "example".

    $ cd careful-soup/
    $ make test

The file `scripts/test` has the details of exercising the application.

## Demo script

1. Start a broker.

2. Start the all-in-one Jaeger container.

        podman run --rm --name jaeger -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 -p 5775:5775/udp -p 6831:6831/udp -p 6832:6832/udp -p 5778:5778 -p 16686:16686 -p 14268:14268 -p 14250:14250 -p 9411:9411 jaegertracing/all-in-one:1.17

   The console is available at http://localhost:16686/.

3. Start the frontend servce.

        export JAEGER_SAMPLER_TYPE=const
        export JAEGER_SAMPLER_PARAM=1
        export HTTP_HOST=localhost
        export HTTP_PORT=8081
        java -jar frontends/java/target/careful-soup-frontend-1.0.0-SNAPSHOT-jar-with-dependencies.jar &

4. Start the backend service.

        python3 backends/python/app.py &

5. Send a request.

        curl -fX POST -d "hello 1" -H "Content-type: text/plain" http://localhost:8081/api/send-request
