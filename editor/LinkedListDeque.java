
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Iterator;


public class LinkedListDeque<Item> implements Iterable<Item> {

	private LinkedListDeque sentF;
	private LinkedListDeque sentB;
	private Item item;
	private LinkedListDeque next;
	private LinkedListDeque prev;
	private LinkedListDeque front;
	private int size;
	public LinkedListDeque cursor;
	public LinkedListDeque actualCursorPosition;
    private int cursorOffsetFromBack;
    private ArrayDeque<LinkedListDeque> removedText = new ArrayDeque<LinkedListDeque>();

	

	private LinkedListDeque(Item item0, LinkedListDeque next0, LinkedListDeque prev0) {
		item = item0;
		next = next0;
		prev = prev0;

	}




	public LinkedListDeque() {

		size = 0;
		
		sentF = new LinkedListDeque(903, null, null);
		sentB = new LinkedListDeque(904, sentF, null);
		front = new LinkedListDeque(null, sentB, sentF);
		sentF.next = front;
		sentF.prev = sentB;
		sentB.prev = front;
		cursor = sentB.prev;
		actualCursorPosition = cursor;
        cursorOffsetFromBack = 0;
		

	}

	public void addFirst(Item i) {



		if (isEmpty()) {
			front.item = i;
			front.prev.next = front;
			front.next.prev = front;
			size += 1;
			cursor = front;
			return;
		}

		pointStart();

		size += 1;
		front = new LinkedListDeque(i, front, sentF);
		front.next.prev = front;
		front.prev.next = front;
		cursor = front;


	}

	public void addLast(Item i) {

		if (isEmpty()) {
			front.item = i;
			front.prev.next = front;
			front.next.prev = front;
			size += 1;
			cursor = sentB.prev;
			return;
		}

		pointBack();

		size += 1;

		front = new LinkedListDeque(i, sentB, front);
		front.prev.next = front;
		front.next.prev = front;
		cursor = sentB.prev;

	}

	public void moveCursorLeft() {
		if (cursor != sentF) {
			cursor = cursor.prev;
			cursorOffsetFromBack += 1;
		}
		return;
		
	}

	public void moveCursorRight() {
		if (cursor.next != sentB) {
			cursor = cursor.next;
			cursorOffsetFromBack -= 1;
		}
		return;
	}

	public void adjustPointersForUndo() {

        pointStart();

        LinkedListDeque tempFront = front;

        front = cursor;

        front.prev.next = front;
        front.next.prev = cursor;

        front = tempFront;

    }

    public void insertPreviousDeleted() {
        LinkedListDeque deleted = removedText.pop();
        if (size == 0) {
            front.item = deleted.item;
            cursor = cursor.next;
            size += 1;
            return;
        }
        LinkedListDeque afterCursor = cursor.next;

        afterCursor.prev = deleted;
        cursor.next = deleted;
        afterCursor.prev.next = afterCursor;
        deleted.prev = cursor;
        cursor = cursor.next;
        if (cursor.prev == sentF) {
            front = front.prev;
        }

//        if (front == sentF) {
//            front = front.next;
//        }
        size += 1;

    }

	public void insertAtCursor(Item i) {

		if (cursor == sentB.prev) {
			addLast(i);
			return;
		} else if (cursor == sentF) {
			addFirst(i);
			return;
		}

		LinkedListDeque<Text> newCursor = new LinkedListDeque(i, cursor.next, cursor);
        cursor = newCursor;
		cursor.prev.next = cursor;
		cursor.next.prev = cursor;
		size += 1;


	}

    public void deleteAtCursor() {
        if (cursor == sentB.prev) {
            removeLast();
            return;
        } else if (cursor.prev == sentF) {
            removeFirst();
            cursor = cursor.prev;
            return;
        } else if (cursor == sentF) {
            return;
        } else {

            LinkedListDeque deletedItem = cursor;
            removedText.push(deletedItem);

            cursor.next.prev = cursor.prev;
            cursor.prev.next = cursor.next;
            cursor = cursor.prev;
            size -= 1;
        }

    }

    public Item deleteAfterCursor() {
        if (isEmpty()) {
            return null;
        } else if (cursorAtEnd()) {
            return null;
        }

        if (cursorAtFront()) {

            front = sentF;

            if (size == 1) {
                Item result = (Item) cursor.next.item;
                cursor.next.item = null;
                size -= 1;
                front = sentF.next;
                return result;
            }
        }

        Item result = (Item) cursor.next.item;
        cursor.next = cursor.next.next;
        cursor.next.prev = cursor.next.prev.prev;
        size -= 1;
        return result;


    }

	public void resetCursor(LinkedListDeque newCursor) {

        cursor = newCursor;
	}

	public Item getBeforeCursor() {
		if (!(cursorAtFront())) {
			return (Item) cursor.prev.item;
		}

		return null;

	}


	public boolean cursorAtFront() {
		return (cursor == sentF || size == 0);
	}

	public boolean cursorAtEnd() {
//		System.out.println(cursor);
		return (cursor.next == sentB || size == 0);
	}

	public Item getAfterCursor() {
		return (Item) cursor.next.item;
	}

    public Item getLineItem() {
        return item;
    }

    public Item getNextItem() {
        return (Item) next.item;
    }

    public LinkedListDeque getNextCursor() {
        return cursor.next;
    }

    public LinkedListDeque startOfFile() {
        return sentF;
    }

	public LinkedListDeque endOfFile() {
        return sentB.prev;
    }

    public LinkedListDeque<Item> getCursor() {
        if (size == 0) {
            return sentF;
        }

        return cursor;
    }

	public Item getCursorItem() {
		return (Item) cursor.item;
	}

    public LinkedListDeque<Item> getFront() {
        pointStart();
        return front;
    }



	public boolean isEmpty() {
		return (size == 0);
	}

	public int size() {
		return size;
	}

	private void pointStart() {
		

		if ((front.next == sentB) && (size != 1)) {

			front = front.next.next.next; // this points the front back to the beginning
			
		}
		
	}

	private void pointBack() {

		if ((front.prev == sentF) && (size != 1)) {
			front = front.prev.prev.prev; // this points the front back to the end of the linked list
		}

	}

	public void pointCursorFront() {

		if ((cursorAtEnd()) ) {

			cursor = cursor.next.next; // this points the cursor back to the beginning
			
		}

	}

	private void pointCursorBack() {

		if ((cursor.prev == sentF) && (size != 1)) {

			cursor = cursor.prev.prev.prev; // this points the cursor back to the back
			
		}

	}

	public void printDeque() {
		if (isEmpty()) {
			return;
		}

		pointStart();

		LinkedListDeque temp = front;

		

		

		while (temp != sentB) {
			System.out.print(temp.item + " ");
			temp = temp.next;
		}

		


		 }

	// public String createText() {

	// 	String result = "";

	// 	if (isEmpty()) {
	// 		return result;
	// 	}

	// 	pointStart();

	// 	LinkedListDeque temp = front;

		
		

		

	// 	while (temp != sentB) {
	// 		result += temp.item.charAt(0);
	// 		temp = temp.next;
	// 	}

	// 	return result;

		


	// 	 }

	

	public Item removeFirst() {
		if (isEmpty()) {
			return null;
		}

		pointStart();

        LinkedListDeque deletedItem = front;
        removedText.push(deletedItem);


		// special case for removing from a list of length 1

		Item result = (Item) front.item;

		if (size == 1) {

//			front.item = null;
			size -= 1;
			return result;
		} 

		size -= 1;

		

		sentF.next = front.next;
		front.next.prev = sentF;
		front = front.next;

		cursor = front;

		return result;


	}

	public Item removeLast() {
		if (isEmpty()) {
			return null;
		}

		pointBack();

        LinkedListDeque deltedItem = front;
        removedText.push(deltedItem);

		Item result2 = (Item) front.item;

		// special case for linked list of length 1
		if (size == 1) {
//			front.item = null;
			size -= 1;
			return result2;
		}

		size -= 1;

		

		sentB.prev = front.prev;
		front.prev.next = sentB;
		front = front.prev;

		cursor = sentB.prev;

		return result2;
	}

	public Item get(int index) {
		pointStart();

		if (index >= size) {
			return null;
		}

		LinkedListDeque temp = front;


		while (index > 0) {
			temp = temp.next;
			index -= 1;
		}

		return (Item) temp.item;

	}

	private Item getRecursiveHelper(int i, LinkedListDeque lst) {

		if (i >= size) {
			return null;
		}		


		else if (i == 0) {
			return (Item) lst.item;
		}

		else {
			return getRecursiveHelper(i - 1, lst.next);
		}

	}

	public Item getRecursive(int index) {

		pointStart();

		return getRecursiveHelper(index, front);
	}

    private class LinkedListIter implements Iterator<Item> {

        private LinkedListDeque<Item> tempFront;
       

        private LinkedListIter() {


        	pointStart();
            tempFront = front;
                       

        }

        public boolean hasNext() {

            if (size == 0) {
                return false;
            }
                        
            return (tempFront != sentB);

        }

        public Item next() {

            Item current = (Item) tempFront.item;
            
            tempFront = tempFront.next;

            return current;

        }

    }

    public Iterator<Item> iterator() {
        return new LinkedListIter();
    }
	






}