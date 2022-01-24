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

package com.akkaserverless.javasdk.testkit.impl

import com.akkaserverless.javasdk.impl.DeferredCallImpl
import com.akkaserverless.javasdk.impl.MetadataImpl
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl
import com.akkaserverless.javasdk.impl.effect.SideEffectImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ActionResultSpec extends AnyWordSpec with Matchers {

  "Action Results" must {
    "extract side effects" in {
      val replyWithSideEffectResult = new ActionResultImpl[String](
        ActionEffectImpl.Builder
          .reply("reply")
          .addSideEffect(SideEffectImpl(
            DeferredCallImpl[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", () => ???),
            synchronous = false)))

      replyWithSideEffectResult.isReply() should ===(true)
      replyWithSideEffectResult.getSideEffects().size() should ===(1)
    }

    "extract forward details" in {
      val forwardResult = new ActionResultImpl[String](ActionEffectImpl.Builder.forward(
        DeferredCallImpl[String, String]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", () => ???)))

      forwardResult.isForward() should ===(true)
      forwardResult.getForward().getMessage should ===("request")
    }
  }

}