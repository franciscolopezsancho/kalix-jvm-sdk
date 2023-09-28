/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.pubsub;

import com.example.Main;
import com.example.wiring.TestkitConfig;
import com.example.wiring.eventsourcedentities.counter.CounterEvent.ValueIncreased;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.EventingTestKit.Message;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.KalixConfigurationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static com.example.wiring.pubsub.PublishESToTopic.COUNTER_EVENTS_TOPIC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("eventing-testkit")
@SpringBootTest(classes = Main.class)
@Import({KalixConfigurationTest.class, TestkitConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class EventingTestkitIntegrationTest {

  @Autowired
  private KalixTestKit kalixTestKit;
  private EventingTestKit.Topic eventsTopic;
  @Autowired
  private WebClient webClient;

  @BeforeAll
  public void beforeAll() {
    eventsTopic = kalixTestKit.getTopic(COUNTER_EVENTS_TOPIC);
  }

  @Test
  public void shouldPublishEventWithTypeNameViaEventingTestkit() {
    //given
    String subject = "test";
    ValueIncreased event1 = new ValueIncreased(1);
    ValueIncreased event2 = new ValueIncreased(2);

    //when
    Message<ValueIncreased> test = kalixTestKit.getMessageBuilder().of(event1, subject);
    eventsTopic.publish(test);
    eventsTopic.publish(event2, subject);

    //then
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.of(SECONDS))
        .untilAsserted(() -> {
          var response = DummyCounterEventStore.get(subject);
          assertThat(response).containsOnly(event1, event2);

          var viewResponse = webClient
              .get()
              .uri("/counter-view-topic-sub/less-then/" + 4)
              .retrieve()
              .bodyToFlux(CounterView.class)
              .toStream()
              .toList();

          assertThat(viewResponse).containsOnly(new CounterView("test", 3));
        });
  }
}