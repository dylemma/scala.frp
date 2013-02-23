package com.dylemma.frp.test

import org.scalatest._
import com.dylemma.frp._

class EventStream_flatMap extends FunSuite with TestHelpers with Observer {

	test("EventStream.flatMap basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[Int]

		val x = for {
			i <- s
			j <- t
		} yield i -> j

		val results = accumulateEvents(x)

		s fire 1

		s fire 2
		t fire 1 //yield 2->1
		t fire 2 //yield 2->2

		s fire 3
		t fire 4 //yield 3->4

		assert(results.toList == List(2 -> 1, 2 -> 2, 3 -> 4))
	}

	test("EventStream.flatMap over multiple layers") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		val u = EventSource[Int]

		val x = for {
			i <- s
			j <- t
			k <- u
		} yield (i, j, k)

		val results = accumulateEvents(x)

		t fire 1 //ignore
		u fire 1 //ignore
		s fire 1 //(now waiting for t)
		u fire 2 //ignore
		t fire 2 //(now waiting for u)
		u fire 3 // fire (1, 2, 3)
		u fire 4 // fire (1, 2, 4)
		s fire 2 //(now waiting for t)
		u fire 5 //ignore
		t fire 3 //(now waiting for u)
		u fire 6 // fire (2, 3, 6)

		assert(results.toList == (1, 2, 3) :: (1, 2, 4) :: (2, 3, 6) :: Nil)
	}

	test("EventStream.flatMap encounters nothing when the second stream fires nothing") {
		val s = EventSource[Int]
		val t = EventSource[Int]

		val x = for {
			i <- s
			j <- t
		} yield i -> j

		val results = accumulateEvents(x)

		s fire 1
		s fire 2
		s fire 3
		s fire 4

		assert(results.toList == Nil)
	}

	test("EventStream.flatMap does not end if the mapped stream ends") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		val x = for {
			i <- s
			j <- t
		} yield i -> j
		val stopped = awaitStop(x)
		val results = accumulateEvents(x)

		s fire 1
		t fire 2
		t.stop

		assert(!stopped())
		assert(results.toList == List(1 -> 2))
	}

	test("EventStream.flatMap ends when the base stream ends") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		val x = for {
			i <- s
			j <- t
		} yield i -> j

		val stopped = awaitStop(x)
		val results = accumulateEvents(x)

		s fire 1
		t fire 2
		s.stop
		assert(stopped())
		assert(results.toList == List(1 -> 2))
	}

	test("EventStream.flatMap on a stopped stream results in a stopped stream") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		s.stop

		val x = for {
			i <- s
			j <- t
		} yield i -> j

		assert(x.stopped)
	}
}