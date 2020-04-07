#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

import os
import random
import sys

from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container
from proton.tracing import init_tracer

tracer = init_tracer("backend")

class BackendHandler(MessagingHandler):
    def __init__(self):
        super(BackendHandler, self).__init__()

        self.sender = None
        self.requests_processed = 0

    def on_start(self, event):
        host = os.environ.get("MESSAGING_SERVICE_HOST", "localhost")
        port = os.environ.get("MESSAGING_SERVICE_PORT", 5672)
        user = os.environ.get("MESSAGING_SERVICE_USER", "example")
        password = os.environ.get("MESSAGING_SERVICE_PASSWORD", "example")

        conn_url = f"amqp://{host}:{port}"
        conn = event.container.connect(conn_url, user=user, password=password)

        event.container.create_receiver(conn, "careful-soup/requests")
        self.sender = event.container.create_sender(conn, None)

    def on_link_opened(self, event):
        if event.link.is_sender:
            print("BACKEND: Opened anonymous sender for responses")

        if event.link.is_receiver:
            print(f"BACKEND: Opened receiver for source address '{event.receiver.source.address}'")

    def on_message(self, event):
        request = event.message

        print(f"BACKEND: Received request '{request.body}'")

        try:
            response_body = self.process_request(request)
        except Exception as e:
            print(f"BACKEND: Failed processing message: {e}")
            return

        response = Message(response_body)
        response.address = request.reply_to
        response.correlation_id = request.id

        print(f"BACKEND: Sending response '{response.body}' to '{request.reply_to}'")

        self.sender.send(response)

        self.requests_processed += 1

    def process_request(self, request):
        return request.body.upper()

def main():
    handler = BackendHandler()

    container = Container(handler)
    container.container_id = f"backend-{random.randint(0, 1000)}"

    container.run()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
