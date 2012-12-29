import org.scalatest._
import scala.frp._
import collection.mutable.ListBuffer

class EventStreamSinkTests extends FunSuite with Observer {
	
	test("EventStream.onNext encounters *only* the next event") {
		val s = EventSource[Int]
		val ints = new ListBuffer[Int]
		s onNext { ints += _ }
		
		s fire 1
		s fire 2
		
		assert(ints.toList == List(1))
	}
	
	test("EventStream.onEnd not triggered when `stop` not called") {
		val s = EventSource[Int]
		var gotEnd = false
		s onEnd { gotEnd = true }
		
		s fire 1
		s fire 2
		
		assert(!gotEnd)
	}
	
	test("EventStream.onEnd triggered when `stop` called") {
		val s = EventSource[Int]
		var gotEnd = false
		s onEnd { gotEnd = true }
		
		s fire 1
		s fire 2
		s.stop
		
		assert(gotEnd)
	}
	
	test("EventStream.foreach encounters all `fire`d events") {
		val s = EventSource[Int]
		val ints = new ListBuffer[Int]
		s foreach { ints += _ }
		
		s fire 1
		s fire 2
		s fire 3
		
		assert(ints.toList == List(1,2,3))
		
	}
}