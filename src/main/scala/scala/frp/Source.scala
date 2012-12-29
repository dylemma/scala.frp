package scala.frp

import collection.parallel.mutable.ParHashSet
import java.lang.ref.WeakReference

/** A Source of data. Data can be produced with the `produce` method (which is `protected`), and will be
  * sent to any number of "Sinks". Sinks can be created by specifying a `handler` 
  * function with the `sink` method.
  *
  * @tparam A The type of the items stored.
  */
trait Source[A] extends Stream[A]{

	/** A number indicating the minimum number of cleared references
	  * that must be encountered before purging all cleared references from the list.
	  * This method may be overridden - the default value is 5.
	  */
	protected def purgeThreshold: Int = 5
	
	private var refs = new ParHashSet[WeakReference[A => Boolean]]
	
	/** Add a handler function that acts as a `sink` for items produced by this
	  * `Source`. The `handler` is expected to return `true` as long as it remains
	  * active. Once the `handler` function returns `false` in response to some
	  * produced item, it will be deactivated and will no longer receive new items.
	  * There is no guarantee of the order that handlers will be called.
	  *
	  * @param handler The handler function to receive items produced by this `Source`.
	  * Once the `handler` returns `false` in response to some produced item, it will
	  * be deactivated and will no longer receive new items.
	  *
	  * @param obs An implicit `Observer` which is required in order to properly 
	  * manage references between this `Source` and any `handler`s, avoiding 
	  * reference loops.
	  */
	def sink(handler: A => Boolean)(implicit obs: Observer): Unit = {
		/* Keep a reference to both `this` and the `handler` in memory, via the 
		 * implicit `Observer`. This way, neither can be garbage collected until
		 * the `Observer` can.
		 */
		obs add this
		obs add handler
		
		/* Add a weak reference to the handler function. By making it a *weak*
		 * reference, we ensure that this `Source` won't force the handler to
		 * stay in memory.
		 */
		//refs += new WeakReference(handler)
		addHandler(handler)
	}
	
	private [frp] def addHandler(handler: A => Boolean): Unit = {
		refs += new WeakReference(handler)
	}
	private [frp] def removeHandler(handler: A => Boolean): Unit = {
		for( ref <- refs.find(_.get == handler) ) refs -= ref
	}
	
	/** Produce a new item. All `handler` functions will be called with `item` as
	  * the argument. There is no guarantee of the order in which the `handler`
	  * functions will be called.
	  *
	  * @param item The item to be sent to all `handler`s (sinks).
	  */
	protected def produce(item: A): Unit = {
		var deadCount = 0
		try {
			for(ref <- refs) ref.get match {
				//count dead references
				case null => deadCount += 1
				
				//run the handler: if it returns false, count it as a dead reference
				case handle if !handle(item) =>
					ref.clear
					deadCount += 1
				case _ => 
			}
		} finally {
			//purge is considered expensive: only do it if there are a lot of dead refs
			if(deadCount >= purgeThreshold) purge
		}
	}
	
	/* Removes dead handler references */
	private def purge = {
		var deadRefs = refs.filter(_.get == null)
		for(r <- deadRefs) refs -= r
	}
}