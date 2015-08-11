/** 
 * RWLock.java 
 * A simple read-write lock listed in book JAVA Threads, page 182, 
 * author Scott Oaks and Henry Wong, publisher O'REILLY (c)1997 
 * 
 * Modified by Yu Zhang (c)2004
 * 
 * The purpose of this class is thread synchronization. A RWLock object 
 * set on some variable prevents different threads from simultaneously 
 * accessing this variable before the read or write operation is completed. 
 * The RWLock object forces the thread wait until the other thread that 
 * had put the lock on the variable finishes its operation.
*/

package soccer.common;

import java.util.Enumeration;
import java.util.Vector;

public class RWLock {
	
	private Vector<RWNode> waiters;

	private int firstWriter() {
		Enumeration<RWNode> e;
		int index;
		for (index = 0, e = waiters.elements(); e.hasMoreElements(); index++) {
			RWNode node = (RWNode) e.nextElement();
			if (node.state == RWNode.WRITER)
				return index;
		}
		return Integer.MAX_VALUE;
	}

	private int getIndex(Thread t) {
		Enumeration<RWNode> e;
		int index;
		for (index = 0, e = waiters.elements(); e.hasMoreElements(); index++) {
			RWNode node = (RWNode) e.nextElement();
			if (node.t == t)
				return index;
		}
		return -1;
	}

	public RWLock() {
		waiters = new Vector<RWNode>();
	}

	public synchronized void lockRead() {
		RWNode node;
		Thread me = Thread.currentThread();
		int index = getIndex(me);
		if (index == -1) {
			node = new RWNode(me, RWNode.READER);
			waiters.addElement(node);
		} else
			node = (RWNode) waiters.elementAt(index);
		while (getIndex(me) > firstWriter()) {
			try {
				wait();
			} catch (Exception e) {
			}
		}
		node.nAcquires++;
	}
	
	public synchronized void lockWrite() {
		RWNode node;
		Thread me = Thread.currentThread();
		int index = getIndex(me);
		if (index == -1) {
			node = new RWNode(me, RWNode.WRITER);
			waiters.addElement(node);
		} else {
			node = (RWNode) waiters.elementAt(index);
			if (node.state == RWNode.READER)
				throw new IllegalArgumentException("Upgrade lock");
			node.state = RWNode.WRITER;
		}
		while (getIndex(me) != 0) {
			try {
				wait();
			} catch (Exception e) {
			}
		}
		node.nAcquires++;
	}

	public synchronized void unlock() {
		RWNode node;
		Thread me = Thread.currentThread();
		int index;
		index = getIndex(me);
		if (index > firstWriter())
			throw new IllegalArgumentException("Lock not held");
		node = (RWNode) waiters.elementAt(index);
		node.nAcquires--;
		if (node.nAcquires == 0) {
			waiters.removeElementAt(index);
			notifyAll();
		}
	}
	
	// this inner class defines an auxiliary RWNode object  
	private class RWNode {
		
		static final int READER = 0;
		static final int WRITER = 1;
		Thread t;
		int state;
		int nAcquires;
		
		RWNode(Thread t, int state) {
			this.t = t;
			this.state = state;
			nAcquires = 0;
		}
	}

}
