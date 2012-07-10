/* 
 * Copyright 2012 Simon Kelly
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.google.safebrowsing2
import org.junit.Test
import org.junit.Assert._
import org.junit.matchers.JUnitMatchers._
import org.hamcrest.CoreMatchers._
import org.junit.Before
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import com.github.tototoshi.http.Client
import org.mockito.Mockito
import org.mockito.Matchers
import com.github.tototoshi.http.Response
import org.apache.http.HttpResponse
import net.google.safebrowsing2.db.DBI
import java.net.URL
import util.Helpers._

class ExpressionGeneratorTests extends MockitoSugar with ByteUtil {

  val eb = new ExpressionGenerator("http://www.google.com")

  @Test
  def testMakeHostKey = {
    val domain = eb.makeHostKeys("www.google.com")
    assertThat(domain(0), is("google.com/"))
    assertThat(domain(1), is("www.google.com/"))
  }

  @Test
  def testMakeHostKey_short = {
    val domain = eb.makeHostKeys("google.com")
    assertThat(domain(0), is("google.com/"))
  }

  @Test
  def testMakeHostKey_long = {
    val domain = eb.makeHostKeys("malware.testing.google.test")
    assertThat(domain(0), is("google.test/"))
    assertThat(domain(1), is("testing.google.test/"))
  }

  @Test
  def testMakeHostList_long = {
    val domains = eb.makeHostList("a.b.c.d.e.f.g")
    assertThat(domains.size, is(5))
    assertTrue(domains.contains("a.b.c.d.e.f.g"))
    assertTrue(domains.contains("c.d.e.f.g"))
    assertTrue(domains.contains("d.e.f.g"))
    assertTrue(domains.contains("e.f.g"))
    assertTrue(domains.contains("f.g"))
  }

  @Test
  def testMakeHostList_short = {
    val domains = eb.makeHostList("d.e.f.g")
    assertThat(domains.size, is(3))
    assertTrue(domains.contains("d.e.f.g"))
    assertTrue(domains.contains("e.f.g"))
    assertTrue(domains.contains("f.g"))
  }

  @Test
  def testMakePathList = {
    val paths = eb.makePathList(new URL("http://test.com/1/2.html?param=1"))
    assertThat(paths.size, is(4))
    assertTrue(paths.contains("/1/2.html?param=1"))
    assertTrue(paths.contains("/1/2.html"))
    assertTrue(paths.contains("/1/"))
    assertTrue(paths.contains("/"))
  }

  @Test
  def testMakePathList_trailingSlash = {
    val paths = eb.makePathList(new URL("http://test.com/1/2/"))
    assertThat(paths.size, is(3))
    assertTrue(paths.contains("/1/2/"))
    assertTrue(paths.contains("/1/"))
    assertTrue(paths.contains("/"))
  }

  @Test
  def testMakePathList_empty = {
    val paths = eb.makePathList(new URL("http://test.com/"))
    assertThat(paths.size, is(1))
    assertTrue(paths.contains("/"))
  }

  @Test
  def testMakePathList_long = {
    val paths = eb.makePathList(new URL("http://test.com/1/2/3/4/5/6/7/a.html?param=1"))
    assertThat(paths.size, is(6))
    assertTrue(paths.contains("/1/2/3/4/5/6/7/a.html?param=1"))
    assertTrue(paths.contains("/1/2/3/4/5/6/7/a.html"))
    assertTrue(paths.contains("/1/2/3/"))
    assertTrue(paths.contains("/1/2/"))
    assertTrue(paths.contains("/1/"))
    assertTrue(paths.contains("/"))
  }

  @Test
  def testExpressions = {
    val expressions = new ExpressionGenerator("http://a.b.c/1/2.html?param=1").expressions
    assertThat(expressions.size, is(8))
    assertTrue(expressions.contains(Expression("a.b.c", "/1/2.html?param=1")))
    assertTrue(expressions.contains(Expression("a.b.c", "/1/2.html")))
    assertTrue(expressions.contains(Expression("a.b.c", "/1/")))
    assertTrue(expressions.contains(Expression("a.b.c", "/")))
    assertTrue(expressions.contains(Expression("b.c", "/1/2.html?param=1")))
    assertTrue(expressions.contains(Expression("b.c", "/1/2.html")))
    assertTrue(expressions.contains(Expression("b.c", "/1/")))
    assertTrue(expressions.contains(Expression("b.c", "/")))
  }

  @Test
  def testExpression = {
    val e = Expression("a.b.c", "/1/2.html?param=1")
    assertThat(e.value, is("a.b.c/1/2.html?param=1"))
    assertThat(e.hexHash.length, is(64))
  }
}
