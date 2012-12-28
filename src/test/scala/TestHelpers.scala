import scala.frp._
import collection.mutable.ListBuffer

trait TestHelpers {
	
	def accumulateEvents[A](e: EventStream[A])(implicit obs: Observer): ListBuffer[A] = {
		val lb = new ListBuffer[A]
		Sink.events(e){ lb += _ }
		lb
	}
	
	class StopCollector(e: EventStream[_])(implicit obs: Observer) {
		private var gotStop = false
		Sink.end(e){ gotStop = true }
		
		def apply() = gotStop
	}
	
	def awaitStop(e: EventStream[_])(implicit obs: Observer) = new StopCollector(e)
	
}