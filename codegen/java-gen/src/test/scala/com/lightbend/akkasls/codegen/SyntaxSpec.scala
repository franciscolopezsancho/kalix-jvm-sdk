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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import com.lightbend.akkasls.codegen.Syntax

class SyntaxSpec extends munit.FunSuite {

  test("indenting should indent for every line") {

    val method = """|public String someName(){
                    |  return "hi";
                    |}""".stripMargin

    val obtained =
      s"""
        |a
        |
        |${Syntax.indent(method, 2)}
        |b
        """.stripMargin

    val expected =
      s"""
        |a
        |
        |  public String someName(){
        |    return "hi";
        |  }
        |b
        """.stripMargin

    assertEquals(obtained, expected)
  }

}