package io.dylemma.frp.test

import io.dylemma.frp._
import collection.mutable.ListBuffer

trait TestHelpers {

	def accumulateEvents[A](e: EventStream[A])(implicit obs: Observer): ListBuffer[A] = {
		val lb = new ListBuffer[A]
		e foreach { lb += _ }
		lb
	}

	class StopCollector(e: EventStream[_])(implicit obs: Observer) {
		private var gotStop = false
		e onEnd { gotStop = true }

		def apply() = gotStop
	}

	def awaitStop(e: EventStream[_])(implicit obs: Observer) = new StopCollector(e)

	class FakeTime extends Time[Long] {
		var _time: Long = 0
		def currentTime = _time
		def setTime(t: Long) = { _time = t }
	}
}