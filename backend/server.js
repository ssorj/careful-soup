//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

"use strict";

const rhea = require("rhea");

const amqp_host = process.env.MESSAGING_SERVICE_HOST || "localhost";
const amqp_port = process.env.MESSAGING_SERVICE_PORT || 5672;

const user = process.env.MESSAGING_SERVICE_USER || "example";
const password = process.env.MESSAGING_SERVICE_PASSWORD || "example";

const id = Math.floor(Math.random() * (10000 - 1000)) + 1000;
const container = rhea.create_container({id: "backend-" + id});

var requests_processed = 0;

function process_request(request) {
    return request.body.toUpperCase();
}

container.on("connection_open", function (event) {
    event.connection.open_receiver("careful-soup/requests");
});

container.on("message", function (event) {
    var request = event.message;

    console.log("BACKEND: Received request '%s'", request.body);

    try {
        var response_body = process_request(request);
    } catch (e) {
        console.error("BACKEND: Failed processing message: %s", e);
        return;
    }

    console.log("BACKEND: Sending response '%s'", response_body);

    var response = {
        to: request.reply_to,
        correlation_id: request.id,
        body: response_body
    };

    event.connection.send(response);

    requests_processed++;
});

container.connect({
    host: amqp_host,
    port: amqp_port
    // username: user,
    // password: password
});

console.log("Connected to AMQP messaging service at %s:%s", amqp_host, amqp_port);
